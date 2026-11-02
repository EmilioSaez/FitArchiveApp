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
