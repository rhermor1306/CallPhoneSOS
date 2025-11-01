# CallPhoneSOS

Aplicación Android nativa (Kotlin) que centraliza acciones rápidas de **emergencia**: realizar una **llamada SOS** a un número configurado, abrir una **URL** de referencia y **programar una alarma/recordatorio** que genera notificación. Incluye un lanzador con accesos directos (correo, ajustes de la app, reloj/alarma del sistema).

> Proyecto preparado para Android **minSdk 24** (Android 7.0) y **targetSdk/compileSdk 34**. Paquete: `com.example.sosphone`.

## Objetivos y alcance

- Tener a mano un flujo de **llamada inmediata** a un contacto SOS, cumpliendo con las políticas de permisos de Android (solicitud en tiempo de ejecución de `CALL_PHONE`).
- **Programar una alarma exacta** que, al disparar, **muestra una notificación** (canal propio `sos_alarm_channel`). Para Android 13+ se solicita `POST_NOTIFICATIONS`.
- Guardar y reutilizar **teléfono, URL y hora/fecha** en `SharedPreferences`.
- Interoperar con apps del sistema (Cliente de **correo**, **Reloj/Alarmas**).

## Características principales

- **Llamada SOS** desde `LLamadaActivity` (sic). Comprueba y solicita el permiso `CALL_PHONE` y realiza `Intent.ACTION_CALL` con el número configurado.
- **Configuración avanzada** en `ConfActivity`:
    - Validación del teléfono con **libphonenumber** (perfil España, ajustable).
    - Selección de **fecha** y **hora** con `DatePickerDialog` + `TimePickerDialog`.
    - Creación del **canal de notificaciones** y **programación de alarma** mediante `AlarmManager` (`setExactAndAllowWhileIdle`/`setExact`, fallback `setAndAllowWhileIdle`).
    - Persistencia en `SharedPreferences` (teléfono, URL, “alarma” como texto legible).
- **Notificación al sonar la alarma**: `AlarmReceiver` construye y publica la notificación en el canal `sos_alarm_channel`.
- **Lanzador** `MainActivity` con accesos: Gmail/cliente de correo, pantalla de llamada SOS y app de reloj (maneja variantes de paquetes comunes).

## Requisitos de compilación

- **Android Studio Giraffe/Koala o superior**.
- **Gradle** con Catálogo de versiones (libs.*).
- Kotlin JVM **1.8** (según `jvmTarget` del módulo).

### Configuración del entorno

1. Abrir la carpeta del proyecto raíz `CallPhoneSOS/CallPhoneSOS` en Android Studio.
2. Sincronizar Gradle. El módulo de aplicación es `app`.
3. Compilar y ejecutar en un dispositivo o emulador con Android 7.0+ (API 24+).

## Permisos y justificación

Declarados en `AndroidManifest.xml`:

- `android.permission.CALL_PHONE`: necesario para realizar llamadas directas con `ACTION_CALL` desde `LLamadaActivity`.
- `android.permission.POST_NOTIFICATIONS` (Android 13+): publicar notificaciones cuando suena la alarma.
- `android.permission.SCHEDULE_EXACT_ALARM` (opcional según políticas del dispositivo): el código usa `setAlarmClock`/`setExactAndAllowWhileIdle` y contempla fallback si el sistema deniega exactitud.

Además, se declara la **feature** `android.hardware.telephony` como `required="false"` para permitir instalación en dispositivos sin telephony (se inhabilitará la llamada directa en esos casos).

## Componentes principales (arquitectura)

- **`MainActivity`**: pantalla inicial con tres accesos rápidos:
    - **Correo**: intenta abrir Gmail; si no existe, envía `ACTION_VIEW` de `mailto:` para que el usuario elija cliente.
    - **Llamada SOS**: navega a `LLamadaActivity`.
    - **Reloj**: abre la app de reloj/alarma del dispositivo (manejo de paquetes comunes, con `try/catch`).

- **`LLamadaActivity`**:
    - Carga el **número SOS** desde `SharedPreferences` o `Intent`.
    - Flujo de **permisos en ejecución** usando `ActivityResultContracts.RequestPermission`.
    - Si se concede `CALL_PHONE`, ejecuta `Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))`.
    - En denegación permanente sugiere ir a **Ajustes de la app** (`ACTION_APPLICATION_DETAILS_SETTINGS`).

- **`ConfActivity`**:
    - Campos de **teléfono**, **URL** y **“alarma”**.
    - Valida números con **`com.googlecode.libphonenumber:libphonenumber:8.12.41`** (por defecto región ES).
    - **Selección de fecha/hora** y programación de alarma mediante `AlarmManager`:
        - `setAlarmClock` o `setExactAndAllowWhileIdle`/`setExact` según versión.
        - Fallback `setAndAllowWhileIdle` ante `SecurityException` o políticas restrictivas.
    - Crea el **canal de notificaciones** requerido (`sos_alarm_channel`) mediante `NotificationChannelCompat` y usa `NotificationManagerCompat`.
    - Persiste los valores en `SharedPreferences`.

- **`AlarmReceiver`**:
    - Recibe el `PendingIntent` de la alarma y publica una **notificación de alta prioridad** en `sos_alarm_channel`.

- **`AndroidManifest.xml`**:
    - Actividades: `MainActivity`, `ConfActivity`, `LLamadaActivity` (exported=false).
    - Receiver: `AlarmReceiver` (exported=false).
    - Permisos y `uses-feature` ya mencionados.

## Estructura relevante

```
app/
  build.gradle.kts
  src/main/
    AndroidManifest.xml
    java/com/example/sosphone/
      MainActivity.kt
      ConfActivity.kt
      LLamadaActivity.kt
      AlarmReceiver.kt
    res/
      layout/
        activity_main.xml
        activity_conf.xml
        activity_llamada.xml
      values/ (strings, themes, colors)
      drawable/ (iconos e imágenes)
      xml/
        backup_rules.xml
        data_extraction_rules.xml
```

## Dependencias destacadas

- **AndroidX**: AppCompat, Activity, ConstraintLayout, Core KTX, Material Components.
- **libphonenumber**: `com.googlecode.libphonenumber:libphonenumber:8.12.41` para validación de teléfonos.

## Instrucciones de uso (flujo funcional)

1. Abre la app. Desde el **menú principal** puedes:
    - Ir a **Correo**.
    - Abrir **Llamada SOS**.
    - Abrir la app de **Reloj** del sistema.
2. En **Configuración**:
    - Introduce **teléfono SOS** (validación automática).
    - (Opcional) Introduce una **URL** asociada.
    - Pulsa el campo **Alarma** para seleccionar **fecha** y **hora** y pulsa **Guardar** para programarla.
3. En **Llamada SOS**:
    - La primera vez se te pedirá el permiso **Llamadas telefónicas**. Concédelo para llamar automáticamente al número configurado.

## Compatibilidad por versión de Android

- **API < 23 (Android 6)**: no se solicitan permisos en ejecución (no soportado por minSdk 24).
- **API 24–32**: se usa `setExact`/`setExactAndAllowWhileIdle` para alarmas exactas.
- **API 33+ (Android 13)**: se solicita **`POST_NOTIFICATIONS`** antes de publicar notificaciones.
- **API 31+ (Android 12)**: si el dispositivo restringe alarmas exactas, el código realiza **fallback** a `setAndAllowWhileIdle` y notifica al usuario.

## Internacionalización

- `strings.xml` centraliza los textos. Por defecto en **español**. Se puede añadir soporte multilenguaje con variantes `values-xx/strings.xml`.

## Seguridad y privacidad

- Los datos (teléfono, URL, hora de alarma) se almacenan en **`SharedPreferences`** locales del dispositivo.
- No hay tráfico a red salvo que el usuario abra la **URL** indicada.
- El permiso **`CALL_PHONE`** se solicita y utiliza exclusivamente para iniciar la llamada SOS.

## Pruebas manuales sugeridas

- Guardar un **teléfono válido** y realizar la llamada. Probar denegación y concesión de permiso.
- Programar una **alarma** a +1 minuto y verificar que aparece la **notificación**.
- Cambiar **región de número** si procede (código en `ConfActivity`/validación) y probar con distintos formatos.
- Verificar en dispositivo **sin app de reloj de Google** que el fallback abre otra app de reloj disponible o muestra mensaje adecuado.
- Probar en Android **13+** la solicitud de **notificaciones**.

## Personalización rápida

- Cambiar **iconos** en `res/mipmap-*` y `res/drawable/*`.
- Ajustar **canal de notificaciones** (`sos_alarm_channel`): nombre, descripción e importancia.
- Modificar región por defecto de **libphonenumber** si el caso de uso no es España.
- Revisar **temas** en `values/themes.xml` y `values-night/themes.xml`.

## Construcción y firma

- Compilación Debug: botón Run de Android Studio o `./gradlew :app:assembleDebug`.
- Firma Release: crear un keystore y configurar `signingConfigs { release { ... } }` en `app/build.gradle.kts`, luego `./gradlew :app:assembleRelease`.
- Ofuscar (opcional): activar `minifyEnabled true` y añadir reglas en `proguard-rules.pro` si se incorpora.

## Licencia

Indica aquí la licencia del proyecto (por ejemplo, MIT, Apache-2.0 o propietaria).

---

**Metadatos del módulo (`app/build.gradle.kts`):** `applicationId = "com.example.sosphone"`, `minSdk = 24`, `targetSdk = 34`, `versionName = "1.0"`, `versionCode = 1`.
