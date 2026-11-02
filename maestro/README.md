# Flujos Maestro (T15)
Emulador: `shogun emulator start`; instalar `:androidApp:installDebug`.
Fotos de prueba: `adb push camiseta_*.jpg /sdcard/Pictures/` (camisetas de color plano; ver docs/bugs-v01.md).
Ejemplo: `maestro test -e FOTO_X=16% -e TIPO=Camiseta -e ESTACION=Verano -e USO=Casual maestro/anadir_prenda.yaml`

Flujos: `limpiar_e_iniciar` (estado limpio), `anadir_prenda` (galería, parametrizado), `filtro`, `favoritas`, `eliminar`, `camara`, `reiniciar_app` (persistencia).
Orden usado en T15: limpiar → 3× anadir_prenda (FOTO_X=16%, 50%, 83%) → filtro → favoritas → reiniciar_app → eliminar. `favoritas` alterna el favorito del jersey; `filtro`/`eliminar` suponen las prendas de prueba (rojo/Jersey/Invierno/Trabajo, azul/Camiseta/Verano/Casual, verde/Abrigo/Entretiempo/Salir).
Observaciones menores: `filtro_aplicar_fijo` (O1), `favorita_visible` (O2), `recorte_fallido` (O4; solo con un build que haga fallar el recorte, con `FOTO_X`).
