# Permisos y textos de justificación

Textos que ve la usuaria al pedir cada permiso. Deben ser claros, concretos y coherentes con `politica-privacidad.md`. Solo se piden en el momento de uso, nunca al abrir la app.

## iOS (Info.plist)

| Clave | Texto (es) | Texto (en, `InfoPlist.strings`) |
|---|---|---|
| `NSCameraUsageDescription` | FitArchive usa la cámara para que fotografíes tus prendas y añadirlas a tu armario. Las fotos se quedan en tu móvil. | FitArchive uses the camera so you can photograph your clothes and add them to your wardrobe. Photos stay on your phone. |
| `NSPhotoLibraryUsageDescription` | FitArchive accede a tus fotos solo para que elijas la imagen de una prenda. Las fotos se quedan en tu móvil. | FitArchive accesses your photos only so you can pick an image of a garment. Photos stay on your phone. |
| `NSLocationWhenInUseUsageDescription` | FitArchive usa tu ubicación aproximada solo para consultar el tiempo y sugerirte outfits. Es opcional: puedes elegir una ciudad. | FitArchive uses your approximate location only to check the weather and suggest outfits. It's optional: you can pick a city instead. |

Notas:
- Solo "cuando la app está en uso" (`WhenInUse`); no se declara `NSLocationAlwaysUsageDescription`.
- Si se usa el selector de fotos del sistema (`PHPickerViewController`), no hace falta `NSPhotoLibraryUsageDescription`; se deja por si se accede a la biblioteca directamente. Eliminarla si no se usa.
- Guardar en la galería (si se añade exportar imagen del outfit) requeriría `NSPhotoLibraryAddUsageDescription`: "FitArchive guarda en tu galería los outfits que decidas exportar como imagen." Ahora mismo no se usa; compartir mediante la hoja del sistema no lo necesita.

## Android (AndroidManifest.xml)

```xml
<!-- Foto de prendas. La cámara no es imprescindible: se puede elegir de galería -->
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-permission android:name="android.permission.CAMERA" />

<!-- Ubicación aproximada: solo para el tiempo. Opcional (alternativa: elegir ciudad) -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

Decisiones:
- **Ubicación**: solo `ACCESS_COARSE_LOCATION`; no se pide `ACCESS_FINE_LOCATION` ni ubicación en segundo plano. El tiempo no necesita precisión.
- **Servicio del tiempo**: Open-Meteo (api.open-meteo.com y geocoding-api.open-meteo.com), gratuito y sin clave de API. Recibe coordenadas aproximadas (redondeadas a 1 decimal, ~11 km) o la ciudad buscada, más la IP inevitable. Política: https://open-meteo.com/en/terms#privacy
- **Internet**: `INTERNET` es un permiso normal (sin diálogo); necesario para la consulta del tiempo.
- **Galería**: usar el selector de fotos del sistema (Photo Picker / `ActivityResultContracts.PickVisualMedia`). Así **no** se declaran `READ_MEDIA_IMAGES` ni `READ_EXTERNAL_STORAGE`, y la política puede decir que solo se accede a las fotos elegidas.
- **Exportar/importar copia**: usar el selector de documentos del sistema (`CreateDocument` / `OpenDocument`); no requiere permisos de almacenamiento.

Textos de justificación previa (diálogo propio antes del permiso del sistema, ya que Android no admite texto propio en el diálogo del sistema):

| Permiso | Texto (es) | Texto (en) |
|---|---|---|
| Cámara | Necesitamos la cámara para que fotografíes tus prendas. Las fotos se guardan solo en tu móvil. | We need the camera so you can photograph your clothes. Photos are stored only on your phone. |
| Ubicación | Usamos tu ubicación aproximada solo para saber el tiempo y sugerirte outfits. Si prefieres, elige una ciudad. | We use your approximate location only to check the weather and suggest outfits. If you prefer, pick a city. |

Si se deniega un permiso, la app sigue funcionando: sin cámara, elegir de galería; sin ubicación, elegir ciudad.

## Fichas de las tiendas (Google Play / App Store)
- Declarar: sin cuenta, sin recopilación de datos por el desarrollador, sin analítica, sin anuncios, sin compartir datos con terceros salvo la consulta del tiempo (Open-Meteo, sin clave de API, recibe coordenadas aproximadas o ciudad).
- URL de la política de privacidad: [PENDIENTE: URL pública donde se alojará `politica-privacidad.md`].
- Revisar este documento si se añade cualquier SDK o permiso nuevo.
