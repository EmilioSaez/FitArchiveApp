# Flujos Maestro (T15)
Emulador: `shogun emulator start`; instalar `:androidApp:installDebug`.
Fotos de prueba: `adb push camiseta_*.jpg /sdcard/Pictures/` (camisetas de color plano; ver docs/bugs-v01.md).
Ejemplo: `maestro test -e FOTO_X=16% -e TIPO=Camiseta -e ESTACION=Verano -e USO=Casual maestro/anadir_prenda.yaml`

Flujos: `limpiar_e_iniciar` (estado limpio), `anadir_prenda` (galería, parametrizado), `filtro`, `favoritas`, `eliminar`, `camara`, `reiniciar_app` (persistencia).
Orden usado en T15: limpiar → 3× anadir_prenda (FOTO_X=16%, 50%, 83%) → filtro → favoritas → reiniciar_app → eliminar. `favoritas` alterna el favorito del jersey; `filtro`/`eliminar` suponen las prendas de prueba (rojo/Jersey/Invierno/Trabajo, azul/Camiseta/Verano/Casual, verde/Abrigo/Entretiempo/Salir).

## Tallas (T31)
`medidas_perfil` (guarda el cuerpo M), `anadir_prenda_talla` (galería + marca + talla + corte; env `FOTO_X TIPO ESTACION USO MARCA TALLA CORTE`), `detalle_talla` (comprueba etiqueta y talla ideal de la 1ª prenda de la pestaña; env `ETIQUETA IDEAL`). Resultados en `docs/pruebas-tallas.md`. La galería del emulador nuevo está vacía: empujar JPG a `/sdcard/Pictures/` y lanzar `MEDIA_SCANNER_SCAN_FILE`.

