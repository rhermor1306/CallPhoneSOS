package com.example.sosphone

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity principal que actúa como lanzador de acciones rápidas:
 * - Apertura de cliente de correo (pref. Gmail).
 * - Acceso a la pantalla de llamada telefónica SOS.
 * - Apertura de una URL configurada por el usuario.
 * - Acceso a la aplicación de Alarmas/Reloj del dispositivo.
 * - Acceso a la pantalla de configuración de la aplicación.
 *
 * Esta clase no almacena estado propio (más allá de referencias a SharedPreferences) y delega
 * la configuración y validación a otras pantallas (ConfActivity, LLamadaActivity).
 */
class MainActivity : AppCompatActivity() {

    // Referencia a SharedPreferences donde se guarda la configuración de la app.
    private lateinit var sharedFich: android.content.SharedPreferences

    // Clave de acceso a la URL configurada (proveniente de strings.xml).
    private lateinit var claveUrl: String

    /**
     * Punto de entrada del ciclo de vida de la Activity. Inicializa la interfaz, resuelve
     * las claves de preferencias y asocia listeners a cada botón del menú principal.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa SharedPreferences y claves de configuración
        val nameSharedFich = getString(R.string.name_preferen_shared_fich)
        claveUrl = getString(R.string.name_shared_url)
        sharedFich = getSharedPreferences(nameSharedFich, Context.MODE_PRIVATE)

        // Botón: abrir cliente de correo (preferentemente Gmail)
        val btnGmail = findViewById<ImageButton>(R.id.btnGmail)
        btnGmail.setOnClickListener {
            abrirGmail()
        }

        // Botón: ir a pantalla de llamada SOS
        val btnTelefono = findViewById<ImageButton>(R.id.btnTelefono)
        btnTelefono.setOnClickListener {
            val intent = Intent(this, LLamadaActivity::class.java)
            startActivity(intent)
        }

        // Botón: abrir URL guardada en configuración
        val btnURL = findViewById<ImageButton>(R.id.btnURL)
        btnURL.setOnClickListener {
            val urlGuardada = sharedFich.getString(claveUrl, null)
            if (!urlGuardada.isNullOrEmpty()) {
                abrirURL(urlGuardada)
            } else {
                Toast.makeText(this, "No hay URL configurada", Toast.LENGTH_LONG).show()
            }
        }

        // Botón: abrir aplicación de Alarmas/Reloj del sistema
        val btnAlarma = findViewById<ImageButton>(R.id.btnAlarma)
        btnAlarma.setOnClickListener {
            abrirAlarma()
        }

        // Botón: ir a pantalla de configuración
        val btnAjustes = findViewById<ImageButton>(R.id.btnAjustes)
        btnAjustes.setOnClickListener {
            val intent = Intent(this, ConfActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Intenta abrir el cliente de correo Gmail mediante un intent implícito SENDTO con esquema mailto:.
     * Si Gmail no está instalado, muestra un chooser para elegir cualquier app de correo disponible.
     *
     * Consideraciones:
     * - Se utiliza ACTION_SENDTO y data "mailto:" para limitar el chooser a apps de correo.
     * - Se fuerza el paquete "com.google.android.gm" si está disponible para abrir Gmail directamente.
     * - Se capturan excepciones para escenarios sin apps de correo instaladas.
     */
    private fun abrirGmail() {
        try {
            // Intent base limitado a aplicaciones de correo (mailto:)
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // Restringe a clientes de correo
                putExtra(Intent.EXTRA_EMAIL, arrayOf("")) // Campo destinatario (opcional)
                putExtra(Intent.EXTRA_SUBJECT, "Asunto del correo")
                putExtra(Intent.EXTRA_TEXT, "Escribe tu mensaje aquí...")
            }

            // Forzar Gmail si está instalado
            intent.`package` = "com.google.android.gm"

            // Si Gmail puede manejar el intent, se abre directamente
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Si no hay Gmail, mostrar chooser con otras apps de correo
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_email))
                startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "No se encontró ninguna aplicación de correo", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Abre una URL en el navegador del dispositivo mediante ACTION_VIEW.
     * Se capturan excepciones para casos de URLs mal formadas o sin manejadores disponibles.
     *
     * @param url URL a abrir (se espera una URI válida con esquema http/https).
     */
    private fun abrirURL(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "URL inválida o no accesible", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Intenta abrir la aplicación de alarmas/reloj del sistema.
     * Flujo:
     * 1) Intent estándar ACTION_SHOW_ALARMS.
     * 2) Si no existe handler, se intenta abrir explícitamente la app de reloj de Google
     *    (u otros fabricantes) mediante setClassName.
     * 3) Si tampoco es posible, se informa al usuario.
     *
     * Nota: la disponibilidad de la app de reloj depende del fabricante y del dispositivo.
     */
    private fun abrirAlarma() {
        // Intent estándar de plataforma para mostrar alarmas
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback: intento explícito a la app de reloj (Google Clock u otros)
            try {
                val relojIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    // Paquete/clase habituales en dispositivos con Google Clock
                    setClassName("com.google.android.deskclock", "com.android.deskclock.DeskClock")
                }
                startActivity(relojIntent)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "No se encontró ninguna aplicación de alarma o reloj",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
