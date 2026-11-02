# Pruebas v0.1 (T15): resultados y bugs

Entorno: emulador Android API 36 (Medium_Phone), `androidApp` debug. Fotos de prueba: camisetas de color plano empujadas con `adb push` a `/sdcard/Pictures`. Flujos reutilizables en `maestro/`.

## Recorrido

| Paso | Resultado |
|---|---|
| Pantalla de carga (puertas) | OK |
| Armario vacío + CTA | OK |
| Añadir desde galería | OK |
| Añadir con cámara | OK tras arreglar B2 (cámara virtual del emulador) |
| Quitar fondo (ML Kit) | OK tras arreglar B1 |
| Color detectado | OK (rojo → Rojo, azul → Azul) |
| Guardar | OK |
| Pestañas por tipo / color / estación | OK (contadores correctos) |
| Filtro multicriterio + "Solo favoritas" + Limpiar | OK |
| Favorita | OK |
| Detalle | OK |
| Editar (marca, talla, estación) | OK |
| Eliminar con confirmación (cancelar conserva; confirmar borra BD y foto) | OK |
| Reiniciar app: persisten prendas, edición y favorito | OK |

## Bugs corregidos en esta rama

- **B1. Primera vez, la foto se guarda sin quitar el fondo ni detectar color.** La primera segmentación ocurre mientras Google Play services aún descarga el módulo de ML Kit (`MlKitException: Waiting for the subject segmentation optional module to be downloaded`, código `UNAVAILABLE`). `AnadirPrendaFlow` traga el error (`runCatching…getOrDefault(original)`) y sigue con la foto original. Repro: instalar, `pm clear`, añadir prenda de galería inmediatamente. Fix: `conReintentos` en `QuitaFondo.android.kt` (hasta 45 intentos, 2 s) mientras el error sea `UNAVAILABLE`; test `ReintentosTest`. Limitación: el reintento solo está verificado con test unitario; en el emulador el módulo ya estaba descargado al reinstalar, así que no se reprodujo la descarga con el fix. Mientras tanto se ve el indicador de progreso.
- **B2. "Hacer foto" no abre la cámara.** Faltaba `android.permission.CAMERA` en `androidApp/src/main/AndroidManifest.xml`; el diálogo de justificación aparecía, pero al pulsar "Continuar" Android no mostraba el permiso (`No requestable permission in the request`) y se volvía a la pantalla anterior sin aviso. Fix: añadidos `uses-permission CAMERA` y `uses-feature camera required=false` (según `docs/permisos.md`).

## Observaciones menores corregidas (rama `feature/fix-bugs-v01`)

- **O1. Botón "Aplicar" del filtro fuera de pantalla. ✅ Corregida.** La hoja se abre entera (`skipPartiallyExpanded`) y "Limpiar"/"Aplicar" quedan fijos abajo; solo los criterios hacen scroll. Flujo `maestro/filtro_aplicar_fijo.yaml`.
- **O2. Corazón de favorita poco visible. ✅ Corregida.** Disco semitransparente de superficie tras el corazón (`FotoPrenda`). Flujo `maestro/favorita_visible.yaml`.
- **O3. Extensión `.jpg` con contenido PNG. ✅ Corregida.** `extensionDeImagen(bytes)` elige `png`/`jpg` según la firma real y la usan `AlmacenFotos` y `PhotoStore` (Android e iOS). Compatibilidad: las fotos ya guardadas como `.jpg` (con PNG dentro) se siguen leyendo y decodificando, porque ni la lectura ni la decodificación dependen del nombre; la copia de seguridad guarda rutas tal cual y al restaurar usa la extensión correcta. Tests: `ExtensionImagenTest`, `AndroidPhotoStoreTest` (extensión por formato, ruta `.jpg` heredada con PNG).
- **O4. Sin aviso si falla el recorte de fondo. ✅ Corregida.** Si `quitarFondo` falla se muestra "No pudimos quitar el fondo" con "Reintentar" y "Usar original" (cerrar vuelve a elegir foto). Flujo `maestro/recorte_fallido.yaml` (verificado forzando el fallo en un build temporal, ya revertido).

## Observaciones pendientes

- **O5. Splash del sistema** muestra el icono por defecto de Android antes de las puertas (pendiente de T48).
- El selector de fotos de Android permite elegir varias imágenes con "Done"; solo se usa una.

## Cómo repetir

```
shogun emulator start
./gradlew :androidApp:installDebug
# fotos: adb push camiseta_*.jpg /sdcard/Pictures/ (+ scan)
maestro test maestro/limpiar_e_iniciar.yaml
maestro test -e FOTO_X=16% -e TIPO=Camiseta -e ESTACION=Verano -e USO=Casual maestro/anadir_prenda.yaml
```
Ver `maestro/README.md`. No cubierto: iOS (Vision, sin compilar en Windows).
