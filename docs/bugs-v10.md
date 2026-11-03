# Prueba de humo v1.0 (T50): resultados y bugs

Preparación de la prueba con usuarias reales (`docs/prueba-usuarias.md`, `docs/distribucion-prueba.md`). Esta página recoge la prueba de humo previa de la build release candidata; **la prueba con usuarias sigue pendiente** y sus incidencias se añadirán aquí (formato de `bugs-v05.md`).

Entorno: emulador Android API 36 (Medium_Phone), fecha 3 nov 2026, ubicación simulada y red. Build: `:androidApp:assembleRelease` (sin firmar, versionName 1.0, versionCode 1), firmada **solo localmente** con la clave debug estándar de Android para poder instalarla; instalación limpia. Flujos en `maestro/`.

## Recorrido

| Paso | Resultado |
|---|---|
| Instalar el release firmado y abrir (sin crash; `adb logcat` sin excepciones de FitArchive) | OK |
| Hoy con armario vacío ("Tu armario está vacío", "Añadir prenda") | OK |
| Perfil: medidas, ubicación y buscador de ciudad | OK |
| Añadir camiseta y pantalón desde la galería (`hoy_anadir_prenda`) | OK |
| Crear y listar outfit "Prueba RC" (`outfit_crear`) | OK |
| Aceptar la sugerencia para hoy (`hoy_aceptar`) | OK |
| Crear viaje (`viaje_crear`) | OK |
| Reiniciar la app y conservar datos (`hoy_persistencia`) | OK |
| **Exportar / importar copia (T45)** | **No hay interfaz (B15)** |

Flujos que no pasaron por orden de ejecución, no por bugs: `hoy_sugerencia` (exige no tener outfits guardados) y `sucia_manual` (exige haber ejecutado antes `calendario_usos`).

## Bugs pendientes

- **B15. T45 no tiene interfaz: la usuaria no puede exportar ni importar su copia.** `BackupService`, el códec y `SqlWardrobeBackupStore` existen y tienen tests en `shared`, pero ninguna pantalla los usa: Perfil solo muestra medidas y "Ubicación y tiempo". Falta, por plataforma, un selector de documentos (`CreateDocument` / `OpenDocument`, ver `docs/formato-copia.md`), botones en Perfil, confirmación antes de importar (reemplaza todo) y mensajes de éxito/error. No se corrige aquí por ser una funcionalidad nueva y no un bug pequeño. **Impacto:** la tarea 9 del guion de prueba no se puede hacer y la política de privacidad ya promete esta función. Debería resolverse antes de T51.

## Observaciones

- **O19. La versión de la app no se muestra en ninguna pantalla.** Para los informes de error hay que pedir el nombre del archivo APK. Valorar mostrarla en Perfil.
- **O20. Release sin firma de tienda ni minificación.** `assembleRelease` sale sin firmar y sin R8; la configuración de firma/minificación corresponde a T51. El APK release (≈12 MB) funciona igual que el debug (≈16 MB) en lo probado.
