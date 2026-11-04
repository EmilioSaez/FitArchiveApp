# Publicación en Google Play y App Store (T51)

Checklist ordenado. **No se ha publicado nada ni se ha usado ninguna cuenta**: lo hecho es lo que se puede preparar en el repo. T51 sigue abierta hasta completar los pasos manuales.

Leyenda: ✅ hecho en el repo · ⬜ pendiente (manual o depende de otra tarea).

## 0. Antes de nada
- ⬜ T50 (prueba con usuarias reales) cerrada y errores críticos corregidos.
- ⬜ Datos del responsable decididos: nombre/razón social, correo de contacto, edad mínima (los pide la política de privacidad).
- ⬜ Dominio/hosting para la política (ver paso 3).

## 1. Cuentas de desarrollador (manual)
- ⬜ **Google Play Console**: cuota única de 25 USD. Si es cuenta personal nueva, Google exige una **prueba cerrada con ≥12 testers durante 14 días** antes de dar acceso a producción (comprobar la regla vigente; es el paso más lento). Verificación de identidad y de dispositivo.
- ⬜ **Apple Developer Program**: 99 USD/año. Apple ID con 2FA y, si es organización, número D-U-N-S.
- ⬜ Decidir titular (persona o empresa): aparece públicamente en las fichas.

## 2. Android: build de release
- ✅ `androidApp/build.gradle.kts`: `release` con R8 (`isMinifyEnabled`) y `isShrinkResources`; reglas en `androidApp/proguard-rules.pro` (kotlinx.serialization, Ktor/SLF4J; SQLDelight, ML Kit, Compose y OkHttp traen sus consumer rules).
- ✅ `versionName = "1.0.0"`; `versionCode = 1` (subir con `-PversionCode=N` o editando; Play exige uno mayor en cada subida).
- ✅ Firma leída de variables de entorno (`FITARCHIVE_KEYSTORE_FILE`, `FITARCHIVE_KEYSTORE_PASSWORD`, `FITARCHIVE_KEY_ALIAS`, `FITARCHIVE_KEY_PASSWORD`) o de `keystore.properties` en la raíz (plantilla `keystore.properties.example`). `.gitignore` excluye `keystore.properties`, `*.jks`, `*.keystore`, `*.p12`, `*.pem`, `*.p8`, `*.mobileprovision`. Sin firma, el bundle sale sin firmar (compila igual).
- ✅ Icono adaptive (con monocromo) integrado desde `store/android/res` + `mipmap-anydpi-v26/ic_launcher.xml`.
- ✅ `./gradlew :androidApp:bundleRelease` funciona → `androidApp/build/outputs/bundle/release/androidApp-release.aab` (~6 MB). `assembleRelease` firmado con una clave temporal también (APK ~2,6 MB).
- ✅ Permisos mínimos según `docs/permisos.md`: `INTERNET`, `ACCESS_COARSE_LOCATION`, `CAMERA` (+ `camera` no requerida). El manifiesto fusionado solo añade `ACCESS_NETWORK_STATE` (ML Kit) y un permiso interno de AndroidX. Sin `READ_MEDIA_*` (Photo Picker).
- ✅ `allowBackup="false"`: coherente con la política ("todo se queda en tu móvil"); con `true`, Android subiría el armario a Google Drive.
- ⬜ **Crear la clave de subida** (fuera del repo, con copia en un gestor de contraseñas; si se pierde hay que pedir un reset a Google):
  `keytool -genkeypair -v -keystore fitarchive-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
- ⬜ Activar **Play App Signing** al crear la app (Google guarda la clave de firma real; la nuestra es solo de subida).
- ⬜ Generar el AAB final con la clave real y guardar `mapping.txt` (`androidApp/build/outputs/mapping/release/`) para subirlo como archivo de desofuscación.
- ⬜ Revisar `targetSdk` (36) frente al mínimo exigido por Play en la fecha de subida.

## 3. Política de privacidad
- ✅ Texto en `docs/politica-privacidad.md` (T49).
- ⬜ Rellenar los **[PENDIENTE]** (responsable, correo, fecha, edad mínima).
- ⬜ **Alojarla en una URL pública estable** (p. ej. GitHub Pages o página propia) que muestre es/en sin login. Ambas tiendas la exigen; sustituir el `[PENDIENTE: URL…]` de `docs/permisos.md`.

## 4. Ficha y assets
Estado de `store/`:
- ✅ Textos es/en (`ficha-es.md`, `ficha-en.md`), icono 512 (Play), icono 1024 (iOS), SVG fuente.
- ⬜ **Capturas de pantalla** (no existen). Play: mínimo 2 de teléfono; recomendable 4–8. App Store: 6,9" (1320×2868) obligatorio y 6,5" según se pida; iPad solo si se declara compatible. Idealmente en es y en. Se pueden sacar del emulador con datos de ejemplo (flujos en `maestro/`).
- ⬜ **Gráfico de funciones** de Play (feature graphic) 1024×500 PNG/JPG, sin alfa.
- ⬜ (Opcional) vídeo promocional (URL de YouTube).
- ⬜ Categoría: Estilo de vida (Play) / Estilo de vida + Productividad (App Store) — decidido en las fichas; falta marcarlo en las consolas.
- ⬜ Contacto de la ficha: correo, web (opcional).
- ⬜ Comprobar que el nombre "FitArchive" está libre en ambas tiendas y en registros de marcas.

## 5. Formularios de Play Console (contenido de la app)
- ⬜ **Clasificación de contenido (IARC)**: cuestionario; esperado apto para todos (sin violencia, sexo, juego, contenido generado por usuarias ni compras).
- ⬜ **Público objetivo**: según la edad mínima que se fije en la política; **no** marcar niños (evita la política de Familias).
- ⬜ **Anuncios**: no contiene anuncios.
- ⬜ **Seguridad de los datos** (propuesta, a contrastar con `docs/politica-privacidad.md`):
  - **Ubicación aproximada**: se envía a Open-Meteo (proveedor de servicio, redondeada a ~11 km), finalidad "funcionalidad de la app", **opcional**, cifrada en tránsito (HTTPS). Declarar como recopilada y compartida con terceros según el criterio de Google para servicios externos.
  - **Fotos, medidas, armario, outfits**: procesados y guardados **solo en el dispositivo** → no recopilados.
  - **Cámara**: se usa solo para fotografiar prendas; los datos no salen del móvil.
  - **ML Kit / Google Play services**: el SDK puede enviar a Google diagnósticos y métricas de rendimiento (https://developers.google.com/ml-kit/android-data-disclosure). Revisarlo y declarar "Diagnósticos" si procede; **actualizar entonces la política**.
  - Cifrado en tránsito: sí. Borrado de datos: no hay cuenta ni datos en servidor (desinstalar borra todo).
- ⬜ **Permisos**: cámara y ubicación aproximada son permisos normales de ejecución; sin ubicación en segundo plano ni acceso amplio a almacenamiento, así que no requieren declaración especial.
- ⬜ ID de publicidad: no se usa. País de distribución, precio **gratis** (no se puede pasar de gratis a de pago después), sin compras integradas.

## 6. Subida a Google Play
1. ⬜ Crear la app en Play Console (nombre, idioma por defecto es-ES, gratuita).
2. ⬜ Completar fichas (es-ES y en-US), gráficos y formularios del paso 5.
3. ⬜ **Pruebas internas** (hasta 100 testers): subir el AAB firmado y probar instalando desde Play (comprueba firma y descarga del modelo ML Kit).
4. ⬜ Si la cuenta exige prueba cerrada: ≥12 testers durante 14 días seguidos.
5. ⬜ Solicitar acceso a producción → versión de producción con notas → enviar a revisión (de horas a unos días).
6. ⬜ Tras publicar: vigilar Android vitals/crashes; subir `versionCode` en cada actualización.

## 7. iOS (requiere un Mac con Xcode)
Detalle de lo no validado en `docs/ios-pendiente.md`. Nada de esto se ha intentado en Windows.
- ✅ (T53) Proyecto Xcode generado con XcodeGen desde `iosApp/project.yml` (`docs/ios-xcodegen.md`). Antes: **el proyecto Xcode no estaba en el repo** (`iosApp/` solo tiene fuentes Swift, `Info.plist` y `InfoPlist.strings`, sin `.xcodeproj`). Crearlo en el Mac (bundle id `app.luxion.fitarchiveapp`) e integrar el framework `Shared` (Run Script `embedAndSignAppleFrameworkForXcode`).
- ⬜ Enlazar y ejecutar: `linkReleaseFrameworkIosArm64`, simulador y dispositivo real; validar lo listado en `ios-pendiente.md` (cámara, PHPicker, Vision iOS 17+, ubicación).
- **Info.plist**: ✅ ya tiene `NSCameraUsageDescription` y `NSLocationWhenInUseUsageDescription` (es y en, vía `InfoPlist.strings`). ⬜ Añadir `CFBundleLocalizations` (es, en), `ITSAppUsesNonExemptEncryption = NO` (solo HTTPS estándar) y versiones (`CFBundleShortVersionString` 1.0.0, `CFBundleVersion` incremental). `NSPhotoLibraryUsageDescription` no hace falta con PHPicker.
- ⬜ **Icono**: copiar `store/ios/AppIcon-1024.png` a `Assets.xcassets/AppIcon.appiconset` (1024×1024 sin alfa).
- ⬜ **Firma**: Team del Apple Developer Program, "Automatically manage signing", certificado de distribución y perfil. Automatizado (sin probar) en `docs/ios-ci.md` (T55).
- ⬜ **Privacy manifest** (`PrivacyInfo.xcprivacy`): declarar APIs de "required reason" que usen la app o dependencias; sin tracking.
- ⬜ **App Store Connect**: crear la app (bundle id, SKU), ficha es/en (`store/ficha-*.md`), capturas, categoría, **clasificación por edades**, **App Privacy** (ubicación aproximada: no vinculada a la usuaria, sin tracking, finalidad funcionalidad de la app; resto: no recopilado), URL de política, URL de soporte, notas para el revisor (sin cuenta; ubicación opcional).
- ⬜ **Archive** (Product → Archive) → Distribute → App Store Connect.
- ⬜ **TestFlight**: prueba interna (hasta 100 miembros del equipo, sin revisión) y, si se quiere, externa (con revisión beta).
- ⬜ Enviar a revisión (24–48 h habitualmente). Riesgos típicos: permisos sin justificar (hay textos), funcionalidad mínima (4.2), capturas que no coincidan con la app.

## 8. Después de publicar
- ⬜ Etiqueta git `v1.0.0` en `main` y anotar versión/build de cada release.
- ⬜ Enlaces a las fichas en el README.
- ⬜ Marcar T51 como `[x]` en `docs/tareas.md` solo cuando ambas tiendas estén publicadas.
