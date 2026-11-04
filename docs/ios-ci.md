# Firma y subida a TestFlight desde CI (T55)

Estado: **preparado, sin probar**. Bloqueado hasta tener una cuenta de pago del Apple Developer Program (99 USD/año). No hace falta un Mac.

Piezas: `iosApp/fastlane/{Fastfile,Appfile}`, `iosApp/Gemfile` y `.github/workflows/ios-testflight.yml`. Dependen del proyecto XcodeGen `iosApp/project.yml` (T53; proyecto y scheme `FitArchive`, bundle id `app.luxion.fitarchiveapp`). El `.xcodeproj` no se versiona.

El workflow se lanza a mano (Actions → iOS TestFlight → Run workflow) o al crear una etiqueta `v*`. Si falta algún secreto, el job `comprobar-secretos` lo avisa y **omite** la subida sin fallar.

## Secretos de GitHub (Settings → Secrets and variables → Actions)
| Secreto | Contenido |
|---|---|
| `ASC_KEY_ID` | ID de la clave de la API de App Store Connect |
| `ASC_ISSUER_ID` | Issuer ID (misma pantalla de claves) |
| `ASC_KEY_P8_BASE64` | Archivo `AuthKey_XXXX.p8` en base64 |
| `APPLE_TEAM_ID` | Team ID (Membership en developer.apple.com) |
| `IOS_DIST_CERT_P12_BASE64` | Certificado de distribución + clave privada (`.p12`) en base64 |
| `IOS_DIST_CERT_PASSWORD` | Contraseña del `.p12` |
| `IOS_PROVISION_PROFILE_BASE64` | Perfil App Store (`.mobileprovision`) en base64 |
| `IOS_PROFILE_NAME` | (opcional) Nombre del perfil, si no se quiere el nombre por defecto |

Nunca subas al repo certificados, claves ni perfiles (`.gitignore` ya excluye `*.p12`, `*.p8`, `*.pem`, `*.mobileprovision`). Guarda los originales en un gestor de contraseñas.

## Cómo crearlos sin Mac
1. **Clave API de App Store Connect**: App Store Connect → Usuarios y acceso → Integraciones → Claves de App Store Connect → crear con rol *App Manager*. Anota Key ID e Issuer ID y descarga el `.p8` (solo se puede una vez). Codifícalo: `base64 -w0 AuthKey_XXXX.p8` (Git Bash) o `[Convert]::ToBase64String([IO.File]::ReadAllBytes("AuthKey_XXXX.p8"))` (PowerShell).
2. **Identificador de la app**: developer.apple.com → Certificates, IDs & Profiles → Identifiers → App IDs → `app.luxion.fitarchiveapp`. Crea además la app en App Store Connect con ese bundle id.
3. **Certificado de distribución** con openssl (Git Bash lo incluye):
   ```
   openssl genrsa -out dist.key 2048
   openssl req -new -key dist.key -out dist.csr -subj "/CN=FitArchive Distribution/emailAddress=TU_CORREO"
   ```
   Sube `dist.csr` en Certificates → + → *Apple Distribution* y descarga `distribution.cer`. Después:
   ```
   openssl x509 -inform DER -in distribution.cer -out distribution.pem
   openssl pkcs12 -export -inkey dist.key -in distribution.pem -out dist.p12 -password pass:CONTRASEÑA
   base64 -w0 dist.p12
   ```
   (Con OpenSSL 3 y runners antiguos puede hacer falta `-legacy`; si `import_certificate` falla por MAC, regenera con esa opción.)
4. **Perfil de aprovisionamiento**: Profiles → + → *App Store Connect* → App ID `app.luxion.fitarchiveapp` → el certificado anterior → descarga el `.mobileprovision` y codifícalo en base64.
5. Crea los secretos en GitHub (`gh secret set NOMBRE < archivo.txt` o desde la web).

## Número de build
TestFlight exige un `CFBundleVersion` mayor en cada subida para la misma versión.
- Por defecto el lane usa `latest_testflight_build_number + 1` (no hace falta tocar nada).
- Para fijarlo, pásalo en el campo *build_number* al lanzar el workflow (o `BUILD_NUMBER`).
- La versión visible (`CFBundleShortVersionString`, p. ej. 1.0.0) se cambia en `project.yml`.

## Sin probar (todo)
Nada se ha ejecutado: no hay cuenta de Apple ni Mac. Solo se validó la sintaxis YAML del workflow. Riesgos a vigilar en la primera ejecución:
- Opciones de `build_app`/`export_options` y el nombre exacto del perfil (`IOS_PROFILE_NAME`).
- Que `xcodegen generate` y el build del framework `Shared` (T53) funcionen en `macos-15`.
- Que `increment_build_number` actúe sobre el `.xcodeproj` generado (si no, mover `CURRENT_PROJECT_VERSION` a una variable de `project.yml`).
- Formato del `.p12` (ver nota de `-legacy`).
