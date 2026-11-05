# iOS: pendiente de validar en un Mac

`./gradlew :shared:compileKotlinIosSimulatorArm64` y `:shared:compileKotlinIosArm64` compilan en Windows
(chequeo de tipos de Kotlin/Native). Lo que NO se puede hacer en Windows: enlazar el framework, ejecutar
en simulador/dispositivo y compilar el proyecto Xcode.

## Cambios al arreglar la compilacion
- `QuitaFondo.ios.kt` (T07): `generateMaskedImageForInstances` -> `generateMaskedImageOfInstances` (nombre real de la API de Vision) e import de `platform.CoreImage.createCGImage` (extension).
- `SelectorFoto.ios.kt` (T06): la galeria usaba `canLoadObjectOfClass/loadObjectOfClass(UIImage)`, que Kotlin/Native no admite (`UIImage.Companion` no es `NSItemProviderReading`). Ahora usa `hasItemConformingToTypeIdentifier("public.image")` + `loadDataRepresentationForTypeIdentifier` + `UIImage.imageWithData`.

## Sin validar en un Mac
- Enlazado del framework (`linkDebugFrameworkIosSimulatorArm64`) y build de Xcode.
- Galeria (PHPicker): que la carga por `public.image` devuelva la imagen correcta (orientacion, HEIC) y que el callback llegue en el hilo principal.
- Camara (`UIImagePickerController`) y permisos de Info.plist.
- Quitar fondo (Vision `VNGenerateForegroundInstanceMaskRequest`, iOS 17+): resultado del recorte, PNG con transparencia, `createCGImage` con el rect de `ci.extent`, comportamiento en iOS < 17.
- Ubicacion iOS (T39b), `IosPhotoStore` y carga de imagenes: solo comprobado que compilan, no su ejecucion.

## Copia de seguridad y versión (B15, O19)
`SelectorCopia.ios.kt` (`UIDocumentPickerViewController` para exportar con `forExportingURLs` e importar con `forOpeningContentTypes`, `asCopy = true`) y `VersionApp.ios.kt` compilan, pero no se han ejecutado. Sin validar en un Mac:
- Que el selector se presente sobre el controlador superior y que los delegados lleguen en el hilo principal.
- Exportar: archivo temporal en `NSTemporaryDirectory` copiado donde elija la usuaria, y que `.fitarchive` se guarde con ese nombre.
- Importar: que `UTTypeData` deje elegir archivos `.fitarchive` desde Archivos/iCloud y se lean sus bytes completos.
- Ida y vuelta con `IosPhotoStore`: las rutas de foto son relativas a Documents, y `Repositories.copia` usa `IosPhotoStore()`.
- Versión: `CFBundleShortVersionString`/`CFBundleVersion` en `Info.plist` (hoy sin proyecto Xcode).

## Publicación (T51): lo que exige un Mac
Checklist completo en `docs/publicacion.md` (sección 7). No se ha intentado nada de esto en Windows:
- Crear el proyecto Xcode (no existe `.xcodeproj` en `iosApp/`) e integrar el framework `Shared`.
- Completar `Info.plist` (`CFBundleLocalizations`, `ITSAppUsesNonExemptEncryption`, versiones), `PrivacyInfo.xcprivacy` y el `AppIcon.appiconset` desde `store/ios/AppIcon-1024.png`.
- Firma con cuenta de Apple Developer, Archive, TestFlight y App Store Connect.

## Proyecto XcodeGen (T53): solo se confirma en CI
Ver `docs/ios-xcodegen.md`. En Windows solo se validó: YAML/plist/JSON válidos, rutas existentes y `:shared:compileKotlinIosSimulatorArm64`. Sin confirmar hasta el primer run de macOS (T54):
- Que `xcodegen generate` acepte `project.yml` (claves, `preBuildScripts`, `excludes` del Info.plist).
- Que el Run Script de Gradle encuentre `./gradlew` ejecutable y que `embedAndSignAppleFrameworkForXcode` deje el framework en `FRAMEWORK_SEARCH_PATHS`.
- Enlazado: `-lsqlite3` suficiente con el framework estático; `import Shared` resuelto.
- Que el catálogo de assets compile y App Store Connect acepte el icono sin alfa.
- Que los motivos de `PrivacyInfo.xcprivacy` sean los correctos (revisar el aviso de privacidad al subir a TestFlight).
- Mínimo iOS 17.0 elegido por Vision; confirmar si se quiere bajar.

## CI en macOS (T54)
`.github/workflows/ios.yml` (PRs y `develop`, runner `macos-15`) enlaza `Shared` para el simulador, ejecuta `:shared:iosSimulatorArm64Test` (303 tests), genera el proyecto con XcodeGen y compila con `xcodebuild` sin firma. Ya validado en GitHub: compila y pasa. Notas:
- `macos-14` ya no se admite como etiqueta; los runners macOS arm64 pueden tardar en asignarse (si no se obtiene en ~15 min, GitHub cancela el job: relanzar).
- `xcodebuild` lleva `ARCHS=arm64`: el destino genérico pide también `x86_64` y `Shared` solo tiene `iosSimulatorArm64`.
- `gradlew` debe tener el bit ejecutable en git (`git update-index --chmod=+x gradlew`).
- Los tests iOS activan `PRAGMA foreign_keys` como la app real; sin él fallaban 6 borrados en cascada.
- Esto solo compila y ejecuta tests de shared; no ejecuta la app. La validación en dispositivo sigue pendiente (T56).
