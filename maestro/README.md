# Flujos Maestro (T15)
Emulador: `shogun emulator start`; instalar `:androidApp:installDebug`.
Fotos de prueba: `adb push camiseta_*.jpg /sdcard/Pictures/` (camisetas de color plano; ver docs/bugs-v01.md).
Ejemplo: `maestro test -e FOTO_X=16% -e TIPO=Camiseta -e ESTACION=Verano -e USO=Casual maestro/anadir_prenda.yaml`

Flujos: `limpiar_e_iniciar` (estado limpio), `anadir_prenda` (galería, parametrizado), `filtro`, `favoritas`, `eliminar`, `camara`, `reiniciar_app` (persistencia).
Orden usado en T15: limpiar → 3× anadir_prenda (FOTO_X=16%, 50%, 83%) → filtro → favoritas → reiniciar_app → eliminar. `favoritas` alterna el favorito del jersey; `filtro`/`eliminar` suponen las prendas de prueba (rojo/Jersey/Invierno/Trabajo, azul/Camiseta/Verano/Casual, verde/Abrigo/Entretiempo/Salir).
Observaciones menores: `filtro_aplicar_fijo` (O1), `favorita_visible` (O2), `recorte_fallido` (O4; solo con un build que haga fallar el recorte, con `FOTO_X`).

## Tallas (T31)
`medidas_perfil` (guarda el cuerpo M), `anadir_prenda_talla` (galería + marca + talla + corte; env `FOTO_X TIPO ESTACION USO MARCA TALLA CORTE`), `detalle_talla` (comprueba etiqueta y talla ideal de la 1ª prenda de la pestaña; env `ETIQUETA IDEAL`). Resultados en `docs/pruebas-tallas.md`. La galería del emulador nuevo está vacía: empujar JPG a `/sdcard/Pictures/` y lanzar `MEDIA_SCANNER_SCAN_FILE`.

## Outfits (T21)
`outfit_crear` (camiseta + pantalón, env `NOMBRE`), `outfit_lista` (favorito, reinicio, cancelar/confirmar eliminar; env `NOMBRE`), `outfit_compartir` (selector del sistema con vista previa). Suponen un armario con una camiseta y un pantalón (fotos de color plano). Resultados en `docs/bugs-v02.md`.

## v0.4 (T38)
`calendario_asignar` (outfit en hoy, nota, cambio de mes, reinicio; env `OUTFIT`), `calendario_usos` (outfit en un día del mes anterior y comprobar el historial en el detalle; env `OUTFIT DIA`), `sucia_manual` (limpiar/ensuciar, umbral − / +, insignia en el Armario), `viaje_crear` (validación de fechas con DatePicker y alta del viaje 10–14 dic 2026; env `DESTINO`), `viaje_maleta` (outfit, prenda suelta, casillas, reinicio; env `OUTFIT DESTINO`), `viaje_sucias` (aviso de sucias; env `DESTINO`), `viaje_editar_eliminar` (borra la vuelta y la cambia al 12 dic; env `DESTINO NUEVO`). Suponen camiseta + pantalón + un outfit con ambas y un emulador con fecha en noviembre de 2026 (casillas por posición). Orden usado: limpiar → 2× anadir_prenda → outfit_crear → viaje_crear → viaje_maleta → calendario_asignar → calendario_usos → sucia_manual → viaje_sucias → viaje_editar_eliminar. Tras `launchApp` esperar a que termine la pantalla de carga antes de pulsar la barra inferior. Resultados en `docs/bugs-v04.md`.
