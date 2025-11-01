package com.example.sosphone

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Receiver que atiende el disparo de la alarma programada mediante AlarmManager.
 *
 * Responsabilidades:
 * - Construir y mostrar una notificación en el canal "sos_alarm_channel" cuando la
 *   alarma se activa (ver configuración del canal en ConfActivity).
 *
 * Requisitos y consideraciones de plataforma:
 * - En Android 13 (API 33) y superior, es necesario que la aplicación cuente con el
 *   permiso POST_NOTIFICATIONS concedido para poder publicar notificaciones.
 *   La anotación @RequiresPermission sirve como documentación y ayuda a las herramientas
 *   de análisis estático, pero no realiza la solicitud en tiempo de ejecución.
 *   La solicitud del permiso se gestiona en la actividad (por ejemplo, en ConfActivity).
 *
 * - Este receiver asume que la alarma se programó con un PendingIntent explícito que
 *   apunta a AlarmReceiver (ver scheduleAlarmClock en ConfActivity). El intent de
 *   broadcast recibido no se usa para datos adicionales en esta implementación.
 *
 * - El icono utilizado en la notificación es R.drawable.ic_launcher_foreground. Sustituir
 *   por un recurso propio si se requiere personalización.
 */
class AlarmReceiver : BroadcastReceiver() {

    /**
     * Punto de entrada cuando el sistema entrega el broadcast de la alarma.
     *
     * @param context Contexto proporcionado por el sistema. Debe utilizarse para acceder
     *                a recursos y a servicios como NotificationManagerCompat.
     * @param intent  Intent recibido. En esta implementación no se leen extras.
     *
     * Nota:
     * - Se utiliza NotificationManagerCompat para mantener compatibilidad hacia atrás.
     * - El canal "sos_alarm_channel" debe existir. Su creación se realiza en la actividad
     *   de configuración (ConfActivity.createNotificationChannel()) en Android 8.0+.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent?) {
        val builder = NotificationCompat.Builder(context, "sos_alarm_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Reemplazar por icono propio si procede
            .setContentTitle("Alarma SOS")
            .setContentText("¡Tu alarma configurada está sonando!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Publicación de la notificación en el canal configurado.
        // En Android 13+ requiere POST_NOTIFICATIONS concedido.
        NotificationManagerCompat.from(context).notify(2001, builder.build())
    }
}
