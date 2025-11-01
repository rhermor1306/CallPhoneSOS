package com.example.sosphone

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sosphone.databinding.ActivityConfBinding
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Calendar
import java.util.Locale

/**
 * Pantalla de configuración de la aplicación.
 *
 * Responsabilidades:
 * - Cargar y persistir en SharedPreferences el teléfono SOS, la URL y la hora/fecha de la alarma.
 * - Permitir seleccionar fecha y hora mediante DatePicker + TimePicker.
 * - Programar una alarma del sistema usando AlarmManager.setAlarmClock (fiable en Android 12+).
 * - Crear el canal de notificaciones necesario para avisar al usuario al disparar la alarma.
 * - Solicitar el permiso POST_NOTIFICATIONS en Android 13+ para asegurar la visibilidad del aviso.
 *
 * Notas de compatibilidad:
 * - setAlarmClock crea una alarma “de usuario” visible para el sistema y no requiere
 *   SCHEDULE_EXACT_ALARM en Android 12+ para la mayoría de los casos de uso interactivos.
 * - El DatePicker retorna meses en base 0 (0..11); se refleja en el formateo del texto.
 */
class ConfActivity : AppCompatActivity() {
    private lateinit var claveUrl: String
    private lateinit var claveAlarma: String
    private lateinit var confBinding: ActivityConfBinding
    private lateinit var sharedFich: android.content.SharedPreferences
    private lateinit var nameSharedPhone: String

    // Variables para recordar la fecha seleccionada por el usuario (DatePicker)
    private var selYear: Int? = null
    private var selMonth: Int? = null // 0..11
    private var selDay: Int? = null

    /**
     * onCreate: inicializa ViewBinding, preferencias, canal de notificaciones
     * y solicita permiso de notificaciones en Android 13+ si no está concedido.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        confBinding = ActivityConfBinding.inflate(layoutInflater)
        setContentView(confBinding.root)
        initPreferentShared()
        createNotificationChannel()

        // Android 13+: se solicita POST_NOTIFICATIONS si no está concedido para asegurar que
        // la notificación de la alarma será visible al dispararse.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(perm), 33)
            }
        }

        start()
    }

    /**
     * Resuelve nombres de claves y obtiene la referencia a SharedPreferences.
     */
    private fun initPreferentShared() {
        val nameSharedFich = getString(R.string.name_preferen_shared_fich)
        this.nameSharedPhone = getString(R.string.name_shared_phone)
        this.claveUrl = getString(R.string.name_shared_url)
        this.claveAlarma = getString(R.string.name_shared_alarm)
        this.sharedFich = getSharedPreferences(nameSharedFich, Context.MODE_PRIVATE)
    }

    /**
     * onResume: si vuelve con la bandera "back", limpia el teléfono para forzar nueva entrada.
     */
    override fun onResume() {
        super.onResume()
        val ret = intent.getBooleanExtra("back", false)
        if (ret) {
            confBinding.editPhone.setText("")
            Toast.makeText(this, R.string.msg_new_phone, Toast.LENGTH_LONG).show()
            intent.removeExtra("back")
        }
    }

    /**
     * Inicializa la UI con valores guardados, configura el lanzador del DatePicker/TimePicker
     * y maneja la persistencia al pulsar el botón de confirmación.
     */
    private fun start() {
        val sharedPhone: String? = sharedFich.getString(nameSharedPhone, null)
        val sharedUrl: String? = sharedFich.getString(claveUrl, "")
        val sharedAlarma: String? = sharedFich.getString(claveAlarma, "")

        // Precarga de valores desde SharedPreferences
        confBinding.editPhone.setText(sharedPhone)
        confBinding.editUrl.setText(sharedUrl)
        confBinding.editAlarma.setText(sharedAlarma)

        // El campo de alarma actúa como “botón” para abrir los pickers; se evita mostrar teclado.
        confBinding.editAlarma.isFocusable = false
        confBinding.editAlarma.isClickable = true
        confBinding.editAlarma.setOnClickListener {
            val cal = Calendar.getInstance()
            val initY = selYear ?: cal.get(Calendar.YEAR)
            val initM = selMonth ?: cal.get(Calendar.MONTH)
            val initD = selDay ?: cal.get(Calendar.DAY_OF_MONTH)

            // 1) Selector de fecha
            android.app.DatePickerDialog(
                this,
                { _, year, month, day ->
                    selYear = year; selMonth = month; selDay = day

                    // 2) Selector de hora
                    val horaActual = cal.get(Calendar.HOUR_OF_DAY)
                    val minutoActual = cal.get(Calendar.MINUTE)
                    android.app.TimePickerDialog(
                        this,
                        { _, hourOfDay, minute ->
                            // Reflejar fecha/hora elegida en el campo de texto
                            confBinding.editAlarma.setText(
                                String.format(
                                    Locale.getDefault(),
                                    "%04d-%02d-%02d %02d:%02d",
                                    year, month + 1, day, hourOfDay, minute
                                )
                            )
                            // Programar la alarma del sistema
                            scheduleAlarmClock(year, month, day, hourOfDay, minute)
                        },
                        horaActual,
                        minutoActual,
                        true
                    ).show()
                },
                initY, initM, initD
            ).show()
        }

        // Persistencia de la configuración (teléfono, URL y “alarma” como texto formateado)
        confBinding.btnConf.setOnClickListener {
            val url = confBinding.editUrl.text.toString()
            val alarma = confBinding.editAlarma.text.toString()
            val numberPhone = confBinding.editPhone.text.toString()

            if (numberPhone.isEmpty())
                Toast.makeText(this, R.string.msg_empty_phone, Toast.LENGTH_LONG).show()
            else if (!isValidPhoneNumber2(numberPhone, "ES"))
                Toast.makeText(this, R.string.msg_not_valid_phone, Toast.LENGTH_LONG).show()
            else if (url.isNotEmpty() && !Patterns.WEB_URL.matcher(url).matches())
                Toast.makeText(this, "URL no válida", Toast.LENGTH_LONG).show()
            else {
                val edit = sharedFich.edit()
                edit.putString(nameSharedPhone, numberPhone)
                edit.putString(claveUrl, url)
                edit.putString(claveAlarma, alarma)
                edit.apply()

                Toast.makeText(this, "Configuración guardada correctamente", Toast.LENGTH_SHORT).show()
            }
        }

        // Navegación a la pantalla principal
        confBinding.btnIrMain.setOnClickListener {
            val intent = Intent(this@ConfActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Validación genérica mediante API de framework (no regional).
     */
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)
    }

    /**
     * Validación con libphonenumber (región parametrizable).
     * @param countryCode código de región (por ejemplo, "ES").
     */
    fun isValidPhoneNumber2(phoneNumber: String, countryCode: String): Boolean {
        val phoneUtil = PhoneNumberUtil.getInstance()
        return try {
            val number = phoneUtil.parse(phoneNumber, countryCode)
            phoneUtil.isValidNumber(number)
        } catch (e: NumberParseException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Crea el canal de notificaciones requerido para mostrar avisos al disparar la alarma.
     * Obligatorio a partir de Android 8.0 (API 26).
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannelCompat.Builder(
                "sos_alarm_channel",
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
                .setName("Alarma SOS")
                .setDescription("Notificaciones de alarmas configuradas")
                .build()

            NotificationManagerCompat.from(this).createNotificationChannel(channel)
        }
    }

    /**
     * Programa una alarma visible para el sistema mediante AlarmManager.setAlarmClock.
     *
     * Ventajas:
     * - Alta fiabilidad en Android 12+ sin requerir permisos especiales de exactitud.
     * - Integra una “intención de presentación” (icono de reloj) que abre la app al tocar el aviso.
     *
     * @param year Año seleccionado.
     * @param month0To11 Mes en base 0 (0..11) tal como lo entrega DatePicker.
     * @param day Día del mes.
     * @param hour Hora en formato 24h.
     * @param minute Minutos.
     */
    private fun scheduleAlarmClock(year: Int, month0To11: Int, day: Int, hour: Int, minute: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month0To11)    // Importante: DatePicker usa 0..11
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si la fecha/hora es pasada, se programa a +1 minuto para facilitar pruebas.
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.timeInMillis = System.currentTimeMillis() + 60_000L
            Toast.makeText(this, "Hora pasada: programo en 1 minuto para probar.", Toast.LENGTH_SHORT).show()
        }

        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // PendingIntent que recibirá el broadcast cuando dispare la alarma (AlarmReceiver)
        val alarmIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = "com.example.sosphone.ALARM_TRIGGERED"
            `package` = packageName
        }
        val alarmPi = PendingIntent.getBroadcast(
            this,
            1001,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntent “de presentación”: se muestra en el icono del reloj y abre la app al tocar
        val showIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPi = PendingIntent.getActivity(
            this,
            1002,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // setAlarmClock preferente (visible para el sistema). Fallbacks por compatibilidad.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val info = AlarmManager.AlarmClockInfo(cal.timeInMillis, showPi)
                am.setAlarmClock(info, alarmPi)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, alarmPi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, alarmPi)
            }
            Toast.makeText(this, "Alarma programada para ${cal.time}", Toast.LENGTH_LONG).show()
        } catch (se: SecurityException) {
            // Fallback defensivo en caso de denegación de exactitud por políticas del sistema
            Toast.makeText(this, "Exacta denegada; uso inexacta.", Toast.LENGTH_LONG).show()
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, alarmPi)
        }
    }

    /**
     * onNewIntent: permite que la Activity existente reciba nuevos datos desde otra parte de la app.
     * Útil cuando se usa FLAG_ACTIVITY_CLEAR_TOP o FLAG_ACTIVITY_SINGLE_TOP en la navegación.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
