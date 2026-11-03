# Pruebas v0.5 (T44): resultados y bugs

Cubre T39–T43 (tiempo, reglas de color, motor de sugerencias, afinidad aprendida y pantalla "outfit del día"). Entorno: emulador Android API 36 (Medium_Phone) con ubicación simulada (Mountain View) y red, `androidApp` debug, instalación limpia. Armario de prueba: camiseta roja (Verano/Casual), pantalón azul (Invierno/Trabajo) y, para las alternativas, camiseta verde (Entretiempo/Trabajo); fotos de color plano (ver `docs/bugs-v01.md`). Fecha del emulador: 3 nov 2026. Flujos en `maestro/` (`hoy_*`).

## Recorrido

| Paso | Resultado |
|---|---|
| **Sugerencia (T41, T43)** | |
| Con ubicación y red: cabecera "16° · máx 20° / mín 9° · Nublado", avatar vestido con la sugerencia, % y motivo | OK |
| Desglose: Clima, Colores, Frescura (sin Ocasión si no hay ocasión elegida) | OK |
| Armario vacío: "Tu armario está vacío", sin Aceptar/Regenerar, "Añadir prenda" abre el alta | OK |
| Con Trabajo/Casual aparece el criterio "Ocasión" y el motivo lo cita | OK |
| Ocasión sin outfit posible (Deporte): "Hoy no encontramos un outfit" + "Probar sin ocasión" | OK |
| Elegir y desmarcar la misma ocasión la quita | OK |
| Alternativas (2 camisetas + 1 pantalón): fila con la elegida resaltada, elegir una cambia avatar y % | OK |
| "Siguiente" (desactivado con una sola opción; da la vuelta tras la última) y "Regenerar" (vuelve a la mejor) | OK |
| **Tiempo (T39)** | |
| Sin permiso y sin ciudad: aviso "Sin ubicación no sabemos…", sugerencia sin Clima, "Configurar ubicación" lleva a Perfil | OK (ver O16) |
| Sin permiso pero con ciudad elegida (Madrid, España): Hoy usa su tiempo y no muestra el aviso; persiste tras reiniciar | OK |
| Sin red (wifi y datos apagados): "No pudimos consultar el tiempo…", sugerencia sin Clima | OK |
| "Reintentar" con la red restaurada recupera el tiempo | OK |
| **Afinidad aprendida (T42)** | |
| Sin likes, outfits ni usos: no aparece "Tu estilo" | OK |
| Tras guardar un outfit: aparece "Tu estilo" | OK |
| Sin outfits y con un like en una prenda: aparece "Tu estilo" | OK |
| **Acciones** | |
| "Guardar como outfit": aviso "Outfit guardado", botón "Guardado en tus outfits" desactivado, aparece en Outfits | OK |
| "Aceptar para hoy": "Asignado a hoy", outfit en la casilla de hoy del calendario (detalle "Outfit: …") y en Outfits | OK |
| Reiniciar tras aceptar: Hoy sigue mostrando "Asignado a hoy" / "Guardado en tus outfits" | **Fallaba, corregido (B14)** |

Tests: `:shared:testAndroidHostTest` en verde (322 tests, uno nuevo).

## Bugs corregidos en esta rama

- **B14. Tras reiniciar la app, Hoy olvidaba que el outfit sugerido ya estaba guardado y asignado a hoy.** El mapa de outfits guardados (`guardados`) solo vivía en memoria: al reabrir, la misma sugerencia volvía a ofrecer "Aceptar para hoy" y "Guardar como outfit", y pulsarlos creaba un outfit duplicado. Corregido en `OutfitDelDiaViewModel.cargarEntorno`: se rellena `guardados` con los outfits de la BD (misma combinación de prendas). Test `trasReiniciarReconoceElOutfitYaAceptadoYNoLoDuplica`; flujo `hoy_persistencia`.

## Bugs pendientes

Ninguno que requiera cambios de BD.

## Observaciones

- **O14. Dos outfits guardados desde Hoy el mismo día se llaman igual** ("Outfit del 03/11") y en Outfits solo se distinguen por las prendas. Propuesta: sufijo numérico o la ocasión en el nombre.
- **O15. Con alternativas, "Aceptar para hoy" y "Guardar como outfit" quedan bajo el pliegue** y hay que hacer scroll (avatar de 380 dp + fila de alternativas). Propuesta: acciones fijas abajo o avatar más pequeño.
- **O16. "Configurar ubicación" abre Perfil en "Mis medidas"**; la sección "Ubicación y tiempo" está al final y hay que bajar hasta ella. Propuesta: abrir Perfil ya en esa sección.
- **O17. El motivo dice "Hace días que no llevas estas prendas" también con prendas que nunca se han usado.** Correcto pero poco informativo; con armario nuevo es siempre el motivo.
- **O18. El tiempo cambia entre aperturas (15° → 17°, Nublado → Despejado):** consulta real en cada apertura o al regenerar, sin caché. Coherente, solo anotado (relacionado con T47).
- **Entorno:** el recorte de fondo falla en el emulador con fotos planas (diálogo "No pudimos quitar el fondo"); `hoy_anadir_prenda` pulsa "Usar original". Las capturas guardadas en `/sdcard` entran en la galería y el selector las ofrece como foto de prenda (a una prenda se le puso una captura de la propia app): guardarlas en `/data/local/tmp`. Maestro concede los permisos al lanzar: los flujos "sin ubicación" usan `permissions: all: deny`. `hoy_aceptar` necesita el punto de la casilla de hoy (`CASILLA`).
- No cubierto: iOS (no compilable en Windows), prioridad de la ubicación del móvil sobre la ciudad elegida (solo por tests), "Tu estilo" por usos reales (requiere días pasados; cubierto por los tests de `AfinidadAprendida`), outfit con todas las prendas sucias, cambio de huso o de fecha del dispositivo.

## Cómo repetir

```
shogun emulator start
./gradlew :androidApp:installDebug
# en la galería, solo las fotos de prueba (camisetas de color plano)
maestro test maestro/limpiar_e_iniciar.yaml
maestro test -e FOTO_X=16% -e TIPO=Camiseta -e ESTACION=Verano -e USO=Casual maestro/hoy_anadir_prenda.yaml
maestro test -e FOTO_X=50% -e TIPO=Pantalón -e ESTACION=Invierno -e USO=Trabajo maestro/hoy_anadir_prenda.yaml
maestro test maestro/hoy_sugerencia.yaml
maestro test -e OCASION_VACIA=Deporte maestro/hoy_ocasion.yaml
maestro test -e "NOMBRE=Outfit del 03/11" -e "CASILLA=24%,31%" maestro/hoy_aceptar.yaml
maestro test maestro/hoy_persistencia.yaml
maestro test -e FOTO_X=83% -e TIPO=Camiseta -e ESTACION=Entretiempo -e USO=Trabajo maestro/hoy_anadir_prenda.yaml
maestro test maestro/hoy_alternativas.yaml
maestro test -e "NOMBRE=Outfit del 03/11" maestro/hoy_guardar.yaml
maestro test maestro/hoy_estilo.yaml
adb shell pm revoke app.luxion.fitarchiveapp android.permission.ACCESS_COARSE_LOCATION
maestro test maestro/hoy_sin_ubicacion.yaml
maestro test -e CIUDAD=Madrid -e "RESULTADO=Madrid, España" maestro/hoy_ciudad.yaml
adb shell svc wifi disable; adb shell svc data disable
maestro test maestro/hoy_sin_red.yaml
adb shell svc wifi enable; adb shell svc data enable
maestro test maestro/hoy_reintentar.yaml
maestro test maestro/hoy_vacio.yaml   # borra los datos: el último
```
