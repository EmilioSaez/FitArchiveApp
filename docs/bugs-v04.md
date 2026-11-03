# Pruebas v0.4 (T38): resultados y bugs

Cubre T32–T37 (calendario, historial/contador de usos, limpia/sucia, sucia automática, clasificación por uso y maleta de viaje). Entorno: emulador Android API 36 (Medium_Phone), `androidApp` debug, instalación limpia (la BD no tiene migraciones). Armario de prueba: camiseta roja (Verano/Casual) y pantalón azul (Invierno/Trabajo), fotos de color plano (ver `docs/bugs-v01.md`), y un outfit "Look playa" con ambas. Fecha del emulador: 2 nov 2026. Flujos en `maestro/` (`calendario_*`, `viaje_*`, `sucia_*`). Prioridad: la pantalla de Viajes (T37), que no se había probado nunca en emulador, y sus fechas como texto.

## Recorrido

| Paso | Resultado |
|---|---|
| **Viajes (T37)** | |
| Lista vacía + CTA "Nuevo viaje" | OK |
| Guardar sin nombre ni destino → "Ponle un nombre o un destino al viaje" | OK |
| Fecha de ida `2026-02-30` (no existe) y `10/12/2026` (formato) → "Fecha de inicio no válida" | OK |
| Vuelta sin ida → "Indica también la fecha de inicio" | OK |
| Vuelta anterior a la ida → "La vuelta no puede ser antes de la ida" | OK |
| Crear viaje válido: se abre el detalle con "2026-12-10 → 2026-12-14 · 5 días" | OK |
| Sin nombre, el destino hace de nombre (y no se repite en el detalle) | OK |
| Elegir/quitar outfit (chip): sus prendas entran/salen de la maleta; contador "0 de 2 guardadas" | OK |
| Añadir prenda suelta ("Añadida a mano") y quitarla con "Quitar" | OK |
| Marcar casillas → "1 de 2 guardadas" → "¡Maleta lista! 2 prendas guardadas" | OK |
| Reiniciar app: persisten viaje, outfit y casillas | OK |
| Prenda sucia en la maleta: aviso "Sucias, lávalas antes de salir: Camiseta" y "· sucia" en la fila | OK |
| Editar viaje (nombre y vuelta → "3 días"), volver a la lista | OK |
| Eliminar viaje con confirmación (cancelar conserva; confirmar borra y deja el armario intacto) | OK |
| Eliminar una prenda que está en una maleta: el viaje sigue y la prenda desaparece de la lista | OK |
| Quitar un outfit del viaje cuando una de sus prendas se había añadido también a mano | Corregido (B8); verificado con tests, falta pasada visual |
| **Calendario (T32)** | |
| Mes con los días en su columna (1 nov 2026 = domingo), hoy resaltado | OK |
| Asignar outfit a un día (nombre en la casilla), nota (punto "•") y quitar outfit | OK |
| Cambiar de mes (‹ ›) y volver con "Hoy" | OK |
| Reiniciar app: persisten outfits y notas | OK |
| **Historial y contador de usos (T33)** | |
| Detalle de prenda: "Veces usada", "Desde el último lavado", lista de fechas (más reciente primero) | OK |
| Un día pasado con outfit suma uso a cada prenda al abrir la app | OK |
| El día de hoy (y los futuros) no cuentan | OK, por diseño; ver O9 |
| **Limpia/sucia y sucia automática (T34, T35)** | |
| Marcar como sucia/limpia a mano; "Sucia" en la tarjeta del Armario | OK |
| Limpiar pone "Desde el último lavado" a 0 | OK |
| Camiseta (umbral por defecto 1 uso, pantalón 3): tras usarla pasa sola a sucia | OK |
| Ajustar el umbral con − / +: "Sucia tras N usos" ↔ "Sucia: solo a mano" | OK |
| **Clasificación por uso (T36)** | |
| Armario "Ver por → Uso": pestañas con contador y la prenda en su uso (Casual/Trabajo) | OK |
| Combinar con el filtro de estación | OK |

Tests: `:shared:testAndroidHostTest` en verde (294 tests).

## Bugs corregidos en esta rama

- **B8. Quitar un outfit del viaje borraba también las prendas añadidas a mano que coincidían con él.** Causa: `trip_garment` no distinguía "añadida a mano". Corregido: columna `manual` en `TripGarment` (la BD no tiene migraciones: hay que reinstalar la app o borrar sus datos al actualizar el esquema); `anadirPrenda` la marca y `alternarOutfit` solo elimina las filas no manuales, de modo que la prenda a mano sigue en la maleta con su casilla. La copia de seguridad incluye el campo (por defecto `false`, compatible con copias antiguas). Tests: `ViajesViewModelTest`, `RepositoriesTest`, `MaletaTest`. Sin flujo maestro nuevo (requiere emulador).
- Un cambio de texto (O9, abajo).

## Bugs pendientes

Ninguno.

## Observaciones

- **O9. El historial vacío no explicaba por qué un outfit asignado a hoy no cuenta.** El uso solo se registra cuando el día ya ha pasado (`registrarHasta`). Corregido: el texto vacío dice "…el uso se cuenta cuando ese día ya ha pasado" (`HistorialUsos.kt`). Verificado por lectura, no visualmente.
- **O10. Fechas del viaje como texto libre.** Resuelta: el formulario usa `DatePicker` de Material 3 (campos Ida/Vuelta de solo lectura, botón "Borrar" para quitar una fecha; la vuelta sin fecha abre el selector en el mes de la ida) y la lista y el detalle muestran "10 dic → 14 dic 2026 · 5 días" (`formatearFechasViaje`, con el año en cada fecha si cruzan de año). Se mantienen las validaciones (vuelta sin ida, vuelta anterior a la ida) y el almacenamiento ISO. Verificado en emulador con `viaje_crear` y `viaje_editar_eliminar`. Limitación: los textos propios del selector (mes, días de la semana) siguen el idioma del dispositivo.
- **O11. Pestaña Viajes sin viajes cargados: el botón "Nuevo viaje" desaparece de la cabecera** mientras la lista está vacía y solo queda el CTA central. Coherente, solo anotado.
- **O12. El botón "Añadir prenda" de la maleta se deshabilita** cuando ya están todas las prendas del armario en ella, sin explicación. Menor.
- **O13. Tamaño de los botones − / +** del umbral de sucia (texto pequeño, difícil de acertar con el pulgar). Menor.
- **Maestro:** tras `launchApp` hay que esperar a que acabe la pantalla de carga antes de pulsar la barra inferior; con un toque inmediato la app vuelve a "Hoy". Los flujos nuevos llevan `extendedWaitUntil` + `waitForAnimationToEnd`. Para editar un campo de texto con `eraseText` hay que tocar a la derecha del texto (el cursor queda donde se toca).
- Los flujos de calendario asumen noviembre de 2026 (casillas por posición); con otra fecha del emulador, ajustar los puntos.
- No cubierto: iOS (no compilable en Windows), cambio de huso o de fecha del dispositivo con usos ya contados, copia de seguridad con viajes (no probada en emulador).

## Cómo repetir

```
shogun emulator start
./gradlew :androidApp:installDebug
# armario: camiseta + pantalón + outfit (ver maestro/README.md)
maestro test -e DESTINO=Lisboa maestro/viaje_crear.yaml
maestro test -e OUTFIT="Look playa" -e DESTINO=Lisboa maestro/viaje_maleta.yaml
maestro test -e OUTFIT="Look playa" maestro/calendario_asignar.yaml
maestro test -e OUTFIT="Look playa" -e DIA=15 maestro/calendario_usos.yaml
maestro test maestro/sucia_manual.yaml
maestro test -e DESTINO=Lisboa maestro/viaje_sucias.yaml
maestro test -e DESTINO=Lisboa -e NUEVO=Escapada maestro/viaje_editar_eliminar.yaml
```
