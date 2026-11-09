# Avatar 2D (T16)

Maniquí abstracto de boutique: silueta lisa, sin cara ni pelo, líneas limpias y un único tono neutro suave (relleno `#E2DBD4` con degradado lateral a `#C4BBB2`, contorno fino `#A39A92`), legible en tema claro y oscuro. Pensado para recibir prendas por capas (T17). En runtime se dibuja por código (`ui/avatar/CuerpoParametrico.kt`, ajustado a las medidas); los drawables/SVG son la referencia para las proporciones por defecto.

## Recursos
Todo en `shared/src/commonMain/composeResources/`:

| Ruta | Uso |
|---|---|
| `drawable/avatar_{body,arms}.xml` | Capas del maniquí como vector drawable (referencia con las medidas por defecto; el avatar real se dibuja por código). |
| `files/avatar/*.svg` | Mismas capas en SVG (fuente editable; no cargar en runtime en Android). |
| `files/avatar/anchors.json` | Puntos de referencia, orden de capas y cajas de colocación por tipo de prenda. Fuente de verdad para T17. |
| `files/avatar/avatar_preview.svg` | Avatar completo. |
| `files/avatar/avatar_guides.svg` | Avatar con puntos de referencia y cajas dibujados (para revisar). |

Los `.xml` y `.svg` comparten los mismos trazados; si se edita uno, hay que actualizar el otro.

## Sistema de coordenadas
- Lienzo de **400 × 1000** unidades (viewBox `0 0 400 1000`), origen arriba a la izquierda, `y` crece hacia abajo.
- Eje de simetría: `x = 200`. «Izquierda/derecha» se refiere a la usuaria (L = `x < 200`).
- Todas las capas (avatar y prendas) se dibujan en este mismo lienzo y se escalan a la vez: se usa un único factor `escala = altoDisponible / 1000`.
- Proporciones: cabeza 44–142, hombros y 190, busto 262, cintura 352, cadera 456, entrepierna 482, rodillas 690, tobillos 902, suelo 968.

Puntos de referencia (`landmarks` en `anchors.json`): `headTop`, `chin`, `neckBase`, `shoulderL/R`, `bust`, `waist`, `hip`, `crotch`, `kneeL/R`, `ankleL/R`, `handL/R`, `floor`.

## Capas y orden de apilado
De abajo arriba (`z` ascendente):

| z | Capa | Tipo | Contenido |
|---|---|---|---|
| 10 | `body` | avatar | cabeza lisa (sin rasgos), cuello, torso, piernas, pies |
| 14 | `arms` | avatar | brazos (sin manos, punta redondeada) |
| 20 | `shoes` | prenda | calzado |
| 30 | `bottom` | prenda | pantalón, falda, shorts |
| 40 | `top` | prenda | camiseta, camisa, jersey |
| 45 | `dress` | prenda | vestido; **excluye** `top` y `bottom` (si hay vestido, no se pintan) |
| 50 | `outer` | prenda | chaqueta, abrigo (categoría «arriba») |
| 60 | `acc_waist` | accesorio | cinturón |
| 62 | `acc_neck` | accesorio | collar, pañuelo |
| 72 | `acc_face` | accesorio | gafas, pendientes |
| 80 | `acc_head` | accesorio | sombrero, gorro, diadema |
| 90 | `acc_hand` | accesorio | bolsos |

Las prendas de `top`, `dress` y `outer` cubren los brazos (z mayor que `arms`), así que las mangas tapan la piel. Las del calzado quedan sobre los pies; un pantalón largo (z 30) cae sobre el calzado, como en la realidad.

## Colocación de prendas
Cada prenda es una imagen recortada (sin fondo, T07). Para colocarla:

1. Elegir la **caja** del slot según el subtipo (`slots.<slot>.boxes.<subtipo>` en `anchors.json`): `{x, y, w, h}` en unidades del lienzo.
2. Escalar la imagen con regla *contain*: mantener proporción hasta que quepa en la caja.
3. Alinear según `align`: `top-center` (arriba y centrada en la caja; ropa), `bottom-center` (calzado, sombreros), `center` (gafas, cinturón).
4. Convertir a píxeles multiplicando por la escala del lienzo.

Resumen de cajas (`x, y, w, h`):

- **top**: `sin_mangas_corto` 120,172,160,260 · `manga_corta` 106,172,188,270 · `manga_larga` 88,172,224,340 · `largo_cadera` 88,172,224,410
- **outer**: `chaqueta` 84,170,232,330 · `abrigo` 80,170,240,560
- **bottom** (cintura y = 350): `shorts` h130 · `falda_mini` h150 · `falda_midi` h310 · `falda_larga` h550 · `pantalon_corto` h300 · `pantalon_largo` h550 (x 132–140, anchos 120–136)
- **dress**: `corto` 106,172,188,330 · `midi` 104,172,192,500 · `largo` 102,172,196,730
- **shoes**: `par` 148,900,104,72 · `bota_alta` 148,760,104,212 · un solo zapato: `footR` 206,900,44,72 y `footL` 150,900,44,72 (reflejo horizontal)
- **accesorios**: `sombrero_gorro` 138,8,124,76 · `diadema` 158,36,84,30 · `gafas` 160,76,80,28 · `pendientes` 150,100,100,50 · `collar` 168,150,64,70 · `panuelo` 156,150,88,90 · `cinturon` 146,335,108,36 · `bolso_mano_der` 268,470,110,150 · `bolso_hombro_izq` 40,230,90,170

Las cajas son una base; T17 puede permitir ajuste manual (arrastrar/escalar) partiendo de ellas. Cada caja se ve dibujada en `avatar_guides.svg`.

## Mapa tipo de prenda → slot
| Tipo del armario | Slot |
|---|---|
| camisetas, tops, camisas, jerseis | `top` |
| chaquetas, abrigos | `outer` |
| pantalones, faldas, shorts | `bottom` |
| vestidos | `dress` |
| calzado | `shoes` |
| accesorios | `acc_*` según subtipo |

## Para T17
- Dibujar las dos capas del avatar (`body`, `arms`) con `dibujarCapaAvatar` e intercalarlas con las prendas en el orden de la tabla, todas en el mismo lienzo (`aspectRatio(0.4f)`).
- Leer `anchors.json` (por ejemplo con `Res.readBytes("files/avatar/anchors.json")` y kotlinx.serialization) o copiar las cajas a constantes Kotlin.
- Ordenar las prendas del outfit por `z` de su slot; si hay `dress`, omitir `top` y `bottom`.
