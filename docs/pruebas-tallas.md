# Pruebas del algoritmo de tallas (T31)

Casos con las tablas reales (`composeResources/files/tallas/`) y el algoritmo de `AjusteTalla` (T28 + corte T29). Los resultados salen de ejecutar `AjusteTalla.calcular`; los que corrigen un bug están cubiertos por tests de regresión en `AjusteTallaTest` (sección «T31») y `BorradorPrendaTest`.

Cuerpos de prueba (pecho / cintura / cadera / pie, cm):

| Cuerpo | Medidas |
|---|---|
| XS | 80 / 62 / 88 / 22,5 |
| S | 84 / 66 / 91 / 23,5 |
| M | 90 / 72 / 98 / 24,5 |
| L | 98 / 80 / 106 / 25,5 |
| XL | 104 / 86 / 112 / 26,5 |

«Ideal» es la talla ideal. Tabla genérica = `gen`. La columna *Obtenido* es el resultado tras las correcciones; si cambió, se indica el antes.

## Casos

| # | Caso | Esperado razonable | Obtenido |
|---|---|---|---|
| 1 | Cuerpo XS, camiseta genérica XS | bien, ideal XS | bien, ideal XS |
| 2 | Cuerpo XS, camiseta genérica M | holgada | holgada, ideal XS |
| 3 | Cuerpo S, camiseta XS / S / M | ajustada / bien / bien | ajustada / bien / bien, ideal S |
| 4 | Cuerpo M, camiseta S / M / L | ajustada / bien / holgada | ajustada / bien / holgada, ideal M |
| 5 | Cuerpo L, camiseta M / L / XL | ajustada / bien / holgada | ajustada / bien / holgada, ideal L |
| 6 | Cuerpo XL, camiseta L / XL | ajustada / bien | ajustada / bien, ideal XL |
| 7 | Cuerpo M, Zara (tabla vacía) camiseta M | bien con aviso «tabla genérica» | bien, `gen`, ideal M |
| 8 | Cuerpo M, Brandy Melville (talla única, tabla vacía) camiseta M | bien con aviso | bien, `gen` |
| 9 | Cuerpo M, abrigo Zara M | bien (genérica) | bien, clave `chaqueta`, ideal M |
| 10 | Cuerpo M, H&M camiseta S / M / L | S y M valen; L holgada | bien / bien / holgada, ideal S |
| 11 | Cuerpo XL, H&M camiseta M | ajustada, ideal L o XL | ajustada, ideal L |
| 12 | Cuerpo M, Brownie (valores únicos) S / M / L | ajustada / bien / bien o holgada leve | ajustada / bien / bien, ideal M |
| 13 | Cuerpo M, Scalpers S / M / L | ajustada / bien / bien o holgada leve | ajustada / bien / bien, ideal M |
| 14 | Cuerpo M, Hollister camiseta (solo pecho) S / M | ajustada / bien | ajustada / bien, ideal M |
| 15 | Cuerpo XS, Hollister camiseta S | bien o holgada leve | bien, ideal XS |
| 16 | Cuerpo M, Hollister pantalón numérico US «5» | ajustada (la 5 es ≈ cintura 66-68) | ajustada, ideal 9 |
| 17 | Cuerpo XS, Brownie camiseta | talla pequeña de Brownie | S bien en pecho pero holgada en cintura; ideal `XXS2` (talla real de la guía de Brownie) |
| 18 | Medida justo entre S y M (pecho 86 / cintura 68), genérica | S y M valen | bien en las dos, ideal S |
| 19 | Brownie con pecho 83 (entre XS=81 y S=85) | XS y S valen | bien en las dos |
| 20 | Pecho de M y cintura de S (92 / 64 / 98), vestido M / S | M: holgada por la cintura; S: ajustada por el pecho | holgada / ajustada, ideal M |
| 21 | Mismo cuerpo, pantalón M | solo cintura y cadera: holgada por la cintura | holgada |
| 22 | Pecho 130 / cintura 110 (fuera de tabla), camiseta XL | ajustada, ideal la mayor (XXL) | ajustada, ideal XXL |
| 23 | Pecho 66 / cintura 50 (fuera de tabla), camiseta XS | holgada, ideal la menor (XS) | holgada, ideal XS |
| 24 | Solo cintura, camiseta M | valora solo la cintura | bien, ideal M |
| 25 | Solo pecho, pantalón | sin medidas útiles → invitar a rellenar Perfil | `SIN_MEDIDAS` |
| 26 | Talla fuera de tabla («Q») en Hollister | aviso con ideal | `TALLA_NO_ENCONTRADA`, ideal M |
| 27 | Sin talla indicada | solo ideal | `TALLA_NO_ENCONTRADA`, ideal M |
| 28 | Bolso / accesorio | sin resultado | `NO_APLICA` |
| 29 | Pie 22,5 cm, genérica calzado 36 / 37 / 38 | bien / bien (un número de más) / holgada (dos de más) | bien / bien / holgada, ideal 36 |
| 30 | Pie 24,15 cm, calzado 37 / 38 / 39 / 40 | ajustada / bien / bien / holgada | ajustada / bien / bien / holgada, ideal 39 |
| 31 | Pie 24,5 cm, calzado 38 / 39 | ajustada / bien | ajustada / bien, ideal 39 |
| 32 | Pie 21 cm / 31 cm (fuera de tabla) | holgada ideal 35 / ajustada ideal 46 | igual |
| 33 | Corte slim, cuerpo M (90 / 72), camiseta S / M / L | ajustada / bien / holgada | ajustada / bien / holgada, ideal M |
| 34 | Corte slim, pecho 93 / cintura 74, camiseta M | ideal una talla más: L | ideal L (normal: M) |
| 35 | Corte oversize, cuerpo M, camiseta S / M / L | tolerante: las tres bien; ideal una menos (S) | bien / bien / bien, ideal S |
| 36 | Corte oversize, cuerpo XL, pantalón S / L | S ajustada; L bien; ideal una menos (L) | ajustada / bien, ideal L |
| 37 | Corte oversize en calzado | sin efecto | igual que normal |

## Bugs encontrados y corregidos

| # | Bug | Antes | Ahora | Test de regresión |
|---|---|---|---|---|
| B1 | **La talla ideal mezclaba letras y numeración EU.** La genérica trae `XS…XXL` y `34…48`; ganaba el empate la de centro más cercano, aunque fuese de otro sistema | Camiseta «36» → ideal `S`; camiseta `S` slim → ideal `40`; solo cintura → ideal `38` para una `M` | La ideal sale en el sistema de la talla de la prenda (sin talla: el de la primera talla de la tabla) | `laTallaIdealSeDaEnElSistemaDeLaTallaDeLaPrenda`, `slimRecomiendaUnaTallaMas` |
| B2 | **Una letra en una tabla numérica daba «talla no encontrada».** El formulario solo ofrecía `XS…XL` y los pantalones de H&M y Brownie son numéricos (32-60), así que esas prendas nunca mostraban resultado | H&M/Brownie vaquero `M` → `TALLA_NO_ENCONTRADA` | Si la marca no tiene esa talla pero la genérica sí, se usa la genérica (con su aviso). Si no existe en ninguna, se mantiene el aviso con la ideal de la marca | `letraEnMarcaNumericaUsaLaGenericaEnVezDeFallar` |
| B3 | **El formulario no ofrecía las tallas reales.** Solo `XS…XL`: sin numeración EU ni calzado, y el calzado nunca podía dar etiqueta | Zapato sin talla posible | `tallasPara(tipo)`: ropa `XXS…XXL` + `32…48`; calzado `35…46`; cambiar de tipo descarta una talla que el nuevo tipo no ofrece | `tallasSegunElTipo` |
| B4 | **Calzado: dos números de más salía «bien».** El margen de holgura (1,0 cm) era más de un número (≈ 0,65 cm) | Pie 22,5 con un 38 → bien | Margen 0,7 cm: un número de más bien, dos de más holgada | `calzadoUnNumeroDeMasEsBienYDosDeMasEsHolgado` |
| B5 | La talla se comparaba sin ignorar mayúsculas | `m` → no encontrada | `m` = `M` | `laTallaNoDistingueMayusculas` |
| B6 | Una medida a 0 o negativa (copia vieja o corrupta) se trataba como dato real | Pecho 0 → «holgada», ideal XS | Cuenta como ausente (`SIN_MEDIDAS` si no queda ninguna) | `medidasACeroONegativasCuentanComoAusentes` |

Encontrados en la prueba en emulador (no los veían los tests unitarios):

| # | Bug | Antes | Ahora |
|---|---|---|---|
| B7 | **Las tablas de tallas no entraban en el APK.** Con el plugin `androidKotlinMultiplatformLibrary` hay que activar `androidResources` para empaquetar los recursos de Compose. `SeccionTalla` tragaba el error (`runCatching…getOrNull`) y no mostraba nada | Logcat: `MissingResourceException: composeResources/…/files/tallas/generica-eu.json`; el detalle no enseñaba talla | `androidResources { enable = true }` en `shared/build.gradle.kts`; las 12 tablas están en el APK |
| B8 | **`SeccionTalla` no estaba enlazada en el detalle**: la fusión de T34 con T30 en `DetallePrenda.kt` dejó el hueco vacío | Sin bloque de talla | `DetalleContenido` vuelve a llamar a `SeccionTalla(prenda, measurements, onAbrirPerfil)` |
| B9 | «Talla ideal en generica» (nombre crudo del JSON) | `Talla ideal en generica: 39` | `Talla ideal (tabla genérica): 39` (`laTallaIdealDeLaGenericaNoMuestraElNombreCrudo`) |

Además se arregló un error de compilación que ya estaba en `develop` (fusión de T18 y T34 en `ArmarioScreen.kt`: `FotoPrenda` aceptaba `onAlternarFavorita` nulo pero el botón de favorita no comprobaba el null).

## Revisado y dejado como está

- **Umbrales** (ajustada > máx + 1 cm; holgada < mín − 3 cm; valor único ± 2 cm): los casos 1-16 dan resultados razonables y las tallas de paso 4 cm de Brownie y Scalpers quedan contiguas.
- **Corte oversize**: tolera tallas muy grandes (la `L` de un cuerpo `M` sale «bien»). Es lo buscado en una prenda oversize; el efecto principal es bajar la ideal una talla.
- **Fuera de tabla**: no hay aviso específico; el cuerpo mayor que la talla más grande da «ajustada» con la mayor como ideal (y al revés). Es orientativo, y la talla ideal ya es la extrema.
- **Tallas intermedias de Brownie** (`XXS2`, `XS-S`, `M-L`): son tallas reales de su guía y pueden salir como ideal; el formulario no permite elegirlas como talla de la prenda.
- **Empate entre tallas**: gana la de centro más cercano y luego la primera de la tabla (la menor). Medidas justo en el límite entre dos tallas dan la menor como ideal y «bien» en ambas.
- Hollister, Scalpers y Brownie dan medidas de **cuerpo**, no de prenda, como la genérica; Brandy Melville y las marcas de Inditex/Mango siguen vacías (caen en la genérica con aviso) a la espera de datos.

## Prueba de extremo a extremo en emulador

Emulador (`shogun emulator start`, `:androidApp:installDebug`, estado limpio con `pm clear`). Fotos de prueba: 4 camisetas de color plano generadas a mano y empujadas a `/sdcard/Pictures` (la galería del emulador nuevo está vacía).

1. **Perfil**: se guardan las medidas del cuerpo M (altura 165, pecho 90, cintura 72, cadera 98, pie 24,5) → «Medidas guardadas.». Flujo: `maestro/medidas_perfil.yaml`.
2. **Añadir prendas** con `maestro/anadir_prenda_talla.yaml` (galería + marca + talla + corte) y comprobación en el detalle:

| Prenda (marca, talla, corte) | Mostrado en el detalle | Esperado (caso) |
|---|---|---|
| Camiseta H&M, S, normal | «Te queda bien»; pecho/cintura/cadera bien; «Talla ideal en H&M: S» | igual (caso 10) |
| Vaqueros Brownie, M, normal | «Te queda bien»; cintura/cadera bien; ideal M; nota «Brownie no tiene este tipo de prenda o esa talla en su tabla: usamos la genérica europea» | igual (B2) |
| Camiseta Scalpers, S, slim | «Ajustada»; pecho y cintura ajustada; «Corte: slim»; «Talla ideal en Scalpers: L» | igual (caso 13 con corte slim) |
| Calzado Zara, 38, normal | «Ajustada»; pie ajustada; ideal 39; nota «Tabla de Zara pendiente de datos: usamos la genérica europea» | igual (caso 31) |

   El formulario ofrece ahora las tallas por tipo (calzado 35-46; ropa letras y 32-48). `maestro/detalle_talla.yaml` comprueba etiqueta y talla ideal de la primera prenda de la pestaña (probado con la Scalpers: «Ajustada», «Talla ideal en Scalpers: L»).
3. Antes de B7/B8 el detalle no mostraba bloque de talla: la prueba en emulador los destapó.

No se probó: sin medidas guardadas («Añade tus medidas…», cubierto por `PresentacionTallaTest`), edición de la prenda cambiando el tipo, ni iOS.
