# Proyecto Xcode con XcodeGen (T53)

El `.xcodeproj` no se versiona (está en `.gitignore`): se genera desde `iosApp/project.yml`.

## Generarlo
```
brew install xcodegen          # en un Mac o runner macOS
cd iosApp && xcodegen generate # crea iosApp/FitArchive.xcodeproj
```
Proyecto y scheme: `FitArchive`. Bundle id: `app.luxion.fitarchiveapp`. Target mínimo: **iOS 17.0** (el recorte de fondo con Vision lo exige; el código compila con menos, pero esa función fallaría).

## Qué contiene
- `iosApp/project.yml`: definición. Versiones por variables `MARKETING_VERSION` (1.0.0) y `CURRENT_PROJECT_VERSION` (1); CI puede sobrescribirlas con `xcodebuild MARKETING_VERSION=… CURRENT_PROJECT_VERSION=…`.
- Build phase "Compilar framework Shared (Gradle)": `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, con `ENABLE_USER_SCRIPT_SANDBOXING=NO`. El framework (estático) queda en `shared/build/xcode-frameworks/<config>/<sdk>`, que está en `FRAMEWORK_SEARCH_PATHS`. Se enlaza `-lsqlite3` (SQLDelight).
- `iosApp/iosApp/Info.plist`: `CFBundleLocalizations` es/en, `ITSAppUsesNonExemptEncryption=NO`, permisos de cámara y ubicación, solo iPhone en vertical.
- `PrivacyInfo.xcprivacy`: sin tracking ni datos recopilados; APIs de motivo requerido: UserDefaults (CA92.1), marcas de tiempo de archivos (C617.1), hora de arranque (35F9.1).
- `Assets.xcassets/AppIcon.appiconset`: copia de `store/ios/AppIcon-1024.png` (RGB, sin alfa). Si cambia el icono, copiarlo de nuevo.

## Compilar en simulador (sin firma)
```
cd iosApp && xcodegen generate
xcodebuild -project FitArchive.xcodeproj -scheme FitArchive \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```
La firma (equipo, perfil) no está en `project.yml`: la inyecta T55 desde CI.
