package com.example.sosphone

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sosphone.databinding.ActivityLlamadaBinding
import android.Manifest
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Activity encargado de gestionar el proceso de llamada telefónica al número SOS configurado.
 * Controla los permisos de llamada, permite modificar el número guardado y redirige al usuario
 * a la configuración de la aplicación si no se han concedido los permisos necesarios.
 */
class LLamadaActivity : AppCompatActivity() {

    // Enlace de vista generado por ViewBinding
    private lateinit var mainBinding: ActivityLlamadaBinding

    // Número de teléfono SOS configurado
    private var phoneSOS: String? = null

    // Lanzador para solicitar permisos en tiempo de ejecución
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // Bandera que indica si el permiso de llamada fue concedido
    private var permisionPhone = false

    /**
     * Método principal del ciclo de vida de la Activity.
     * Configura la vista, obtiene el número de teléfono almacenado
     * y prepara los controladores de permisos y eventos.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityLlamadaBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(mainBinding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obtiene el número de teléfono guardado en SharedPreferences o pasado por Intent
        val nameSharedFich = getString(R.string.name_preferen_shared_fich)
        val nameSharedPhone = getString(R.string.name_shared_phone)
        val sharedFich = getSharedPreferences(nameSharedFich, Context.MODE_PRIVATE)
        phoneSOS = intent.getStringExtra(getString(R.string.string_phone))
            ?: sharedFich.getString(nameSharedPhone, null)
        mainBinding.txtPhone.setText(phoneSOS)

        init()
        initEventCall()
    }

    /**
     * Método llamado cuando la Activity vuelve a primer plano.
     * Comprueba nuevamente los permisos y actualiza el número de teléfono mostrado.
     */
    override fun onResume() {
        super.onResume()
        permisionPhone = isPermissionCall()

        // Recarga el número guardado en preferencias por si se ha modificado
        val nameSharedFich = getString(R.string.name_preferen_shared_fich)
        val nameSharedPhone = getString(R.string.name_shared_phone)
        val sharedFich = getSharedPreferences(nameSharedFich, Context.MODE_PRIVATE)
        phoneSOS = sharedFich.getString(nameSharedPhone, null)
        mainBinding.txtPhone.setText(phoneSOS)
    }

    /**
     * Inicializa el lanzador de permisos y define el evento de cambio de número de teléfono.
     */
    private fun init() {
        registerLauncher()

        // Verifica si se dispone del permiso de llamada; en caso contrario lo solicita
        if (!isPermissionCall()) {
            requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }

        // Evento para eliminar el número guardado y volver a la pantalla de configuración
        mainBinding.ivChangePhone.setOnClickListener {
            val nameSharedFich = getString(R.string.name_preferen_shared_fich)
            val nameSharedPhone = getString(R.string.name_shared_phone)
            val sharedFich = getSharedPreferences(nameSharedFich, Context.MODE_PRIVATE)
            val edit = sharedFich.edit()
            edit.remove(nameSharedPhone)
            edit.apply()

            val intent = Intent(this, ConfActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("back", true)
            }
            startActivity(intent)
        }
    }

    /**
     * Registra un lanzador para solicitar permisos de llamada.
     * El resultado se procesa en la lambda asociada al contrato.
     */
    private fun registerLauncher() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                permisionPhone = true
            } else {
                Toast.makeText(
                    this,
                    "Necesitas habilitar los permisos de llamada",
                    Toast.LENGTH_LONG
                ).show()
                goToConfiguracionApp()
            }
        }
    }

    /**
     * Asocia el botón principal al evento de llamada telefónica.
     * Si el permiso no ha sido concedido, se vuelve a solicitar.
     */
    private fun initEventCall() {
        mainBinding.button.setOnClickListener {
            permisionPhone = isPermissionCall()
            if (permisionPhone) {
                call()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }

    /**
     * Comprueba si el permiso CALL_PHONE ha sido otorgado.
     * En versiones de Android anteriores a la API 23 no es necesario solicitar permisos en ejecución.
     */
    private fun isPermissionCall(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            true
        } else {
            isPermissionToUser()
        }
    }

    /**
     * Devuelve true si el permiso CALL_PHONE fue concedido al usuario.
     * Utiliza ContextCompat para mantener compatibilidad con versiones anteriores.
     */
    private fun isPermissionToUser() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Realiza la llamada telefónica al número configurado.
     * Si el número no existe, se muestra un mensaje informativo.
     */
    private fun call() {
        if (!phoneSOS.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneSOS")
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "No hay número configurado", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Redirige al usuario a la configuración de la aplicación actual
     * para habilitar manualmente los permisos necesarios.
     */
    private fun goToConfiguracionApp() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    /**
     * Sobrescritura del método onNewIntent.
     * Este método se invoca cuando la Activity ya existe en memoria y recibe un nuevo Intent.
     * Es necesario llamar a setIntent(intent) para actualizar los datos.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

/**
 * Detalle conceptual del flujo de permisos:
 *
 * 1. Registro del permiso:
 *    - Al llamar a registerForActivityResult(), se define el contrato RequestPermission()
 *      y una función lambda que manejará la respuesta (aceptar/denegar).
 *    - En este momento aún no se lanza la solicitud, solo se prepara el comportamiento futuro.
 *
 * 2. Lanzamiento del permiso:
 *    - Se realiza mediante requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE).
 *    - Android mostrará el diálogo de solicitud de permiso al usuario.
 *
 * 3. Respuesta del usuario:
 *    - Si el usuario acepta o deniega, se ejecuta la lambda registrada previamente.
 *    - Esta lambda recibe como parámetro un boolean indicando si el permiso fue concedido.
 *
 * Este flujo permite un manejo claro, moderno y seguro de los permisos en tiempo de ejecución.
 */
