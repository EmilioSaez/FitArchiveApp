# Distribuir una build de prueba (T50)

Cómo hacer llegar FitArchive a las testers, cómo instalarla y cómo recoger errores. La configuración de release y firma de la tienda es de T51 y **no se toca aquí**. Guion y cuestionario: `docs/prueba-usuarias.md`.

Regla de oro: **ninguna clave, contraseña ni keystore entra en el repo** (`.gitignore` ya excluye `*.apk`, `*.aab`, `*.keystore`, `*.jks`). Los APK se reparten por fuera del repositorio.

## Opciones

| Opción | Cuándo | Pros | Contras |
|---|---|---|---|
| **A. APK debug** | Primera ronda rápida con 1–2 testers de confianza | Cero configuración | Más lento y pesado (≈16 MB frente a ≈12 MB); firmado con la clave debug pública; no representa el rendimiento real |
| **B. APK release firmado con clave de prueba** | Ronda principal de testers | Es lo más parecido a lo que se publicará; instalación directa | Hay que activar "instalar apps desconocidas"; actualizaciones manuales; la clave de prueba **no** es la de publicación |
| **C. Play Console, prueba interna** | Cuando T51 haya creado la ficha en Play | Instalación desde Play, actualizaciones automáticas, firma de Play; hasta 100 testers por correo | Necesita cuenta de desarrollador y que T51 haya configurado la firma; requiere cuenta Google de cada tester |

Recomendación: **B ahora** (no depende de T51) y **C** para las rondas siguientes cuando T51 lo permita.

## Generar la build (opción A y B)

Requisito: JDK 17 y Android SDK (`ANDROID_HOME`).

```
./gradlew :androidApp:assembleDebug     # opción A
./gradlew :androidApp:assembleRelease   # opción B (sale sin firmar)
```
Salidas:
- `androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- `androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk`

### Firmar el release con una clave de prueba (opción B)
Crea **una vez** una keystore de prueba fuera del repo (p. ej. en `~/claves/fitarchive-prueba.jks`) y no la subas ni la compartas por chat:

```
keytool -genkeypair -keystore ~/claves/fitarchive-prueba.jks -alias prueba \
  -keyalg RSA -keysize 2048 -validity 365 -dname "CN=FitArchive prueba"
```
Firma con `apksigner` (de `build-tools` del SDK); pedirá la contraseña por consola, sin dejarla en el historial ni en ficheros:

```
apksigner sign --ks ~/claves/fitarchive-prueba.jks --out fitarchive-rc.apk \
  androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk
apksigner verify -v fitarchive-rc.apk
```
Reglas:
- Mantén **siempre la misma clave de prueba** durante toda la ronda: Android solo deja actualizar encima si la firma coincide; si cambia, hay que desinstalar y se pierden los datos (pedir antes una copia, ver abajo).
- La clave de prueba **no** es la de publicación; la gestiona T51 (Play App Signing).
- Nombra el archivo con versión y fecha: `fitarchive-rc-1.0-2026-11-03.apk`. `versionCode`/`versionName` están en `androidApp/build.gradle.kts`.
- Comparte el APK por un enlace privado (nube personal, con acceso solo a las testers) y, si quieres, el **SHA-256** (`sha256sum fitarchive-rc.apk`) para que comprueben que no se ha alterado.

### Prueba interna en Google Play (opción C)
1. En Play Console → Pruebas → Prueba interna, crear una lista de testers por correo.
2. Subir el `.aab` (`./gradlew :androidApp:bundleRelease`, firmado con la configuración de T51).
3. Compartir el enlace de participación que genera Play.
Depende de T51; no se hace en T50.

## Instalar (guion para la tester)
1. Descarga el archivo `.apk` desde el enlace que te enviamos, en el móvil.
2. Ábrelo. Si el móvil dice que no puede instalar apps de esa fuente, pulsa **Ajustes** y activa **Permitir desde esta fuente** para el navegador o gestor de archivos que usaste. (Desactívalo al terminar.)
3. Pulsa **Instalar**. Si aparece "Play Protect" avisando de una app no reconocida, es normal en versiones de prueba: **Instalar de todos modos**.
4. Abre FitArchive. Los permisos se piden solo cuando los necesitas (cámara al hacer una foto, ubicación al pedir el tiempo); **puedes negarlos**, es una de las cosas que queremos probar.
5. Actualizar: instala el APK nuevo encima; **no desinstales** (perderías tus prendas) salvo que te lo pidamos y, en ese caso, exporta antes tu copia.
6. Para terminar la prueba: desinstala la app desde los ajustes del móvil (esto borra todos sus datos).

## Cómo reportar errores
Canal: un único grupo o correo privado de la prueba. Pedimos la **plantilla de incidencia** de `prueba-usuarias.md` (sección 6). Resumen:

**Datos que SÍ pedimos**
- Modelo de móvil y versión de Android.
- Versión de la app.
- Qué pantalla, qué intentabas, qué pasó y cómo repetirlo.
- Captura de pantalla o vídeo corto **solo si no sale nada personal** (que recorte o tape caras, ubicación exacta, nombres).
- Si hay cierre inesperado: hora aproximada.

**Datos que NO pedimos ni queremos**
- Nombre real, teléfono, dirección ni ubicación exacta (basta con la ciudad aproximada si es relevante).
- Fotos de tu ropa o de personas, ni el archivo de copia `.fitarchive` (contiene tus fotos y medidas).
- Contraseñas, claves ni cuentas.

Para el equipo, el log técnico de un cierre se saca con el móvil conectado por USB y depuración activada: `adb logcat -d -b crash` (en la sesión guiada, no a distancia). La app **no envía informes automáticos ni tiene analítica**.

## Privacidad de la prueba
- La app no tiene cuentas y todo queda en el móvil (`docs/politica-privacidad.md`). La única conexión es la consulta del tiempo (Open-Meteo).
- Las testers deben recibir antes: qué datos se recogen **en la prueba** (solo lo que ellas envían con la plantilla), quién los verá y que pueden pedir que se borren.
- Se identifica a cada tester como T1..Tn; la tabla que une número y persona se guarda aparte, fuera del repo.
- Las plantillas y resultados que entran en el repo (`bugs-v10.md`) **no contienen nombres, correos ni capturas** personales.
- La política de privacidad todavía tiene campos `[PENDIENTE]` (responsable, contacto, fecha, edad mínima). Para la prueba basta con informar de palabra del contenido; hay que completarlos antes de publicar (T51).
