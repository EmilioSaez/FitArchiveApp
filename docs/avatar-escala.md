# Avatar 2D a escala real y personalizable

Sustituye al avatar fijo de `docs/avatar.md` (los `drawable/avatar_*.xml` ya no se cargan; se conservan como referencia de estilo).

## 1. Medidas → cuerpo
- `PerfilCorporal.de(Measurements?)` (`avatar/ModeloCorporal.kt`): altura, pecho, cintura, cadera, hombros, brazo, pierna y pie en cm. Lo que falta o está fuera de rango se rellena con una referencia (165 cm, 90-72-98…); `reales` indica qué medidas vinieron de la usuaria.
- `EscalaAvatar(cuerpo)`: `unidadesPorCm = (968 − 44) / altura`. La cabeza siempre ocupa 44–169; una persona más alta tiene menos unidades por cm. Da las y de hombros, pecho, cintura, cadera, entrepierna, rodilla y tobillo.
- `ui/avatar/CuerpoParametrico.kt` dibuja la silueta con Béziers a partir de esos puntos (ancho frontal ≈ 0,30–0,31 × contorno), brazos según largo de brazo y pierna según entrepierna.

## 2. Prendas a escala real
- `DimensionesPrendaCalc.calcular(subtipo, tipo, talla, marca, corte, cuerpo, tablas)` → `DimensionesPrenda` (contornos de pecho/cintura/cadera, largo, manga, en cm). Los contornos salen del centro del rango de la talla en la tabla de la marca (o la genérica) + holgura del corte (slim/normal/oversize); sin tabla, de las medidas de la usuaria; sin ellas, de la referencia. El largo es un valor típico por subtipo.
- `colocacionReal(...)` escala la foto para que su alto (o ancho en calzado) mida lo que la prenda en cm y la ancla al cuerpo (cuello, cintura, suelo). Una foto plana se comprime horizontalmente `ESCORZO_PRENDA` (0,72) porque puesta de frente se ve más estrecha.
- Accesorios (sombrero, gafas, collar, cinturón, bolsos): no van a escala real; usan su caja base adaptada al cuerpo (`cajaAccesorio`).
- No se añaden campos a `Garment`: las dimensiones se calculan al dibujar. El ajuste manual (`AjustePrenda`) se aplica encima.

## 3. Personalización
- `AspectoAvatar`: tono de piel (8), color de pelo (8), peinado (largo, media melena, corto, recogido, sin pelo) y foto de la cara. Con foto, se recorta al óvalo de la cabeza y el pelo dibujado se omite.
- Persistencia: tabla `Avatar` (una fila) con migración `1.sqm` (esquema v2). La foto se guarda en el almacén privado de fotos; **no entra todavía en la copia de seguridad**.
- UI: sección «Tu avatar» en Perfil (`SeccionAvatar`).
- `ProveedorAvatar` (en `App`) expone el `ContextoAvatar` (cuerpo, aspecto, rostro, tablas) por `LocalAvatar`; `AvatarConPrendas` y la tarjeta de compartir lo leen.

## Pendiente
- Revisión visual en dispositivo/emulador del dibujo para distintas medidas.
- Foto de cuerpo entero (ahora solo cara) y copia de seguridad de la foto del avatar.
- Rediseño de pantallas en Stitch si se quiere afinar el estilo de la sección.
