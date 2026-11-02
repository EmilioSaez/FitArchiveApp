# Pruebas v0.2 (T21): resultados y bugs

Cubre T16–T20 (avatar, capas, editor de outfit, lista de outfits, exportar/compartir). Entorno: emulador Android API 36 (Medium_Phone), `androidApp` debug. Armario de prueba: camiseta roja, pantalón azul, vestido verde y una prenda de calzado (fotos de color plano generadas, ver `docs/bugs-v01.md`). Flujos en `maestro/` (`outfit_*`).

## Recorrido

| Paso | Resultado |
|---|---|
| Lista vacía + CTA "Nuevo outfit" | OK |
| Avatar y capas (camiseta sobre cuerpo, pantalón bajo camiseta) | OK |
| Elegir prenda por slot, pestañas con ✓ | OK |
| Vestido sustituye a camiseta + pantalón (y se vuelven a mostrar al quitarlo) | OK |
| Nombre + ocasión + guardar (deshabilitado sin nombre o prendas) | OK |
| Lista: miniatura con prendas, nombre, ocasión | OK |
| Favorito en la lista | OK |
| Reiniciar app: persisten outfit, favorito, ocasión y colocación | OK |
| Abrir outfit y editarlo (carga prendas y ajuste, guarda sin duplicar) | OK |
| Cancelar en el editor | OK |
| Eliminar con confirmación (cancelar conserva; confirmar borra) | OK |
| Compartir desde editor y lista: tarjeta 9:16 y selector del sistema | OK tras B3 y B4 |

Tests: se mantienen los de T16–T20 y se añaden `mostrarErrorYDescartarlo`, `editarOutfitConPrendaEliminadaLaDescartaYConservaElResto` (`ListaOutfitsViewModelTest`) y `vestidoConservaChaquetaYCalzado` (`ColocacionTest`). `:shared:testAndroidHostTest` en verde.

## Bugs corregidos en esta rama

- **B3. El selector de compartir no podía leer la imagen.** Logcat: `Permission Denial: opening provider androidx.core.content.FileProvider ... not exported`; el selector salía sin vista previa y las apps destino podían fallar al leer el PNG. Causa: el permiso de lectura no viajaba en un `ClipData` ni en el intent del selector. Fix: `clipData = ClipData.newRawUri(...)` y `FLAG_GRANT_READ_URI_PERMISSION` también en el chooser (`CompartirImagen.android.kt`). Verificado en emulador: vista previa visible y sin denegaciones. Limitación: no probado con una app destino real (WhatsApp, etc.).
- **B4. Título de la tarjeta solapado con el avatar.** Un nombre que pasa a dos líneas a 84 sp ocupaba más que el hueco hasta el filete y pisaba la cabeza ("Look verano"). Fix: el título reduce su tamaño (84→36 sp, de 6 en 6) hasta caber (`renderizarTarjetaOutfit`).
- **B5. "Compartir" no aparecía en la lista de outfits.** `OutfitsScreen` tenía el enganche `onCompartir` de T19 sin cablear desde `App`, así que solo se podía compartir desde el editor. Fix: la pantalla usa `rememberCompartirOutfit` directamente (las miniaturas ya tienen las fotos cargadas) y muestra el error si falla. En la tarjeta, las tres acciones pasan a `FlowRow` para que no se corte "Eliminar" en media pantalla.
- **B6. Cabecera del editor bajo la barra de estado.** El editor se pinta fuera del `Scaffold` y no aplicaba insets: "Cancelar/Guardar" quedaban sobre el reloj. Fix: `windowInsetsPadding(WindowInsets.systemBars)`.
- **B7. Iconos de la barra de estado ilegibles en toda la app** (blancos sobre fondo claro). Fix: `enableEdgeToEdge()` en `MainActivity`.

## Observaciones corregidas

- **O6. Pantallas fuera del `Scaffold` sin insets.** Añadir prenda y detalle de prenda se pintaban bajo las barras de estado/navegación. Fix: `windowInsetsPadding(WindowInsets.systemBars)` tras el fondo en `AnadirPrendaFlow` y `DetallePrendaScreen`, como el editor. El resto de pantallas (Armario, Perfil, Outfits, Calendario, Hoy) van dentro del `Scaffold`, que ya aplica insets; `PantallaCarga` es un splash centrado. Verificado en emulador solo la pantalla inicial de añadir prenda (centrada, sin cambio visible); formulario y detalle no comprobados visualmente (armario vacío).
- **O7. "Compartir" pulsable antes de cargar las fotos.** Fix: se habilita solo si todas las prendas elegidas tienen la foto cargada (`puedeCompartir`, test `PuedeCompartirTest`). No verificado en emulador (ventana de milisegundos).

## Observaciones pendientes

- **O8.** `subtipoPorDefecto` fija un subtipo por tipo (camiseta siempre manga corta, pantalón siempre largo); el avatar no refleja cortes distintos hasta que la BD guarde subtipo.
- **O9.** El deslizador de tamaño del editor se ve con el pulgar como barra vertical (estilo Material 3 actual); funcional.
- No cubierto: iOS (compartir y render sin compilar en Windows), gestos de arrastrar/pellizcar en el avatar (verificados solo por tests del ViewModel, no con Maestro).

## Cómo repetir

```
shogun emulator start
./gradlew :androidApp:installDebug
# armario: camiseta + pantalón (ver maestro/README.md)
maestro test -e NOMBRE="Look verano" maestro/outfit_crear.yaml
maestro test -e NOMBRE="Look verano" maestro/outfit_lista.yaml
maestro test maestro/outfit_compartir.yaml
```
