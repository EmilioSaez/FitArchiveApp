# Formato de las tablas de tallas (T27)

Ficheros: `shared/src/commonMain/composeResources/files/tallas/<id>.json` (recursos de la app; fuente única de datos).
Ids: `generica-eu`, `zara`, `pullbear`, `bershka`, `stradivarius`, `massimo_dutti`, `mango`, `hm`, `brownie`, `scalpers`, `hollister`, `brandy_melville` (lista en `CargadorTallas.IDS`).

## Esquema

```json
{
  "marca": "H&M",
  "fuente": "url o texto",          // cadena o lista de cadenas
  "fecha": "2026-11-02",
  "unidad": "cm",                   // también se acepta "unidades"; por defecto cm
  "tipo_valor": "rango|punto",      // opcional, informativo
  "talla_unica": true,              // opcional; marca de talla única (Brandy Melville)
  "notas": ["..."],                 // cadena o lista (también "nota")
  "pendiente": ["..."],             // qué falta por recoger; cadena o lista
  "equivalencias": {"M": [38, 40]}, // opcional (genérica): letra -> numeración EU
  "prendas": {
    "<clave de prenda>": {
      "<etiqueta de talla>": {
        "pecho": [min, max],        // medidas opcionales: pecho, cintura, cadera, pie
        "cintura": [min, max],
        "eur": "36-38"              // opcional; "talla_eu" también vale (informativo)
      }
    }
  }
}
```

- Medidas del **cuerpo** en cm. Cada talla trae solo las que la marca publica (p. ej. Hollister parte superior solo `pecho`; calzado solo `pie`).
- Rango `[min, max]`. Un valor único se guarda como `[v, v]` (Brownie, Scalpers); el cargador también acepta `[v]` o un número suelto.
- Orden de tallas = orden del JSON (de menor a mayor).
- Una tabla vacía (`"prendas": {}`) es válida: marca pendiente de datos (Inditex, Mango) o sin guía de medidas corporales (Brandy Melville, `talla_unica`). Nunca se inventan datos; lo que falta va en `pendiente`.
- Las claves de prenda son libres por marca (`camiseta`, `parte_superior`, `parte_inferior_numerica_us`, `vestidos`, `abrigos_chaquetas`…). T28 debe mapear el tipo de prenda de la app a la clave de cada marca; no se han renombrado para no tocar datos recogidos.

## Modelo Kotlin (`app.luxion.fitarchiveapp.tallas`)

| Tipo | Contenido |
|---|---|
| `TablaTallas` | marca, fuentes, fecha, unidad, tipoValor, tallaUnica, notas, pendiente, equivalencias, `prendas: Map<String, TablaPrenda>`, `avisos`; `estaVacia`, `prenda(clave)` |
| `TablaPrenda` | clave y `tallas: List<TallaMarca>`; `talla(etiqueta)` |
| `TallaMarca` | etiqueta, `medidas: Map<Medida, Rango>` (solo las presentes), `equivalenciaEu` |
| `Medida` | `PECHO`, `CINTURA`, `CADERA`, `PIE` |
| `Rango` | `min`, `max`; `esValorUnico`, `centro`, `contains(valor)` |

## Carga

- `CargadorTallas.cargar(id)` / `cargarTodas()` (suspend) leen el recurso con `Res.readBytes`.
- `CargadorTallas.parsear(texto)` es puro y es lo que usan los tests.
- Tolerante: acepta las variantes anteriores, ignora campos desconocidos y descarta entradas mal formadas anotándolas en `TablaTallas.avisos` (vacío en todos los JSON actuales; hay un test). Solo lanza excepción si el texto no es un objeto JSON.

## Cambios en los datos

Solo se han movido de `data/tallas/` a la carpeta de recursos; el contenido no se ha modificado.

## Algoritmo de ajuste (T28)

`AjusteTalla.calcular(medidas, marca, tipo, talla, tablas, opciones)` en `tallas/AjusteTalla.kt`; lógica pura en commonMain.

- **Tabla**: la de la marca (el texto libre se normaliza: `Pull&Bear` → `pullbear`) si tiene datos para el tipo de prenda; si no (marca sin tabla, vacía/pendiente o sin ese tipo), la genérica EU (`esGenerica = true`).
- **Clave de prenda** (`clavesPara`): camiseta/camisa/jersey → `camiseta`, `parte_superior`; chaqueta/abrigo → `chaqueta`, `abrigos_chaquetas`; pantalón/vaquero/short → `pantalon`, `parte_inferior`, `parte_inferior_numerica_us`; falda → `falda`, `parte_inferior`; vestido/mono → `vestido`, `vestidos`; calzado → `calzado`; bolso/accesorio → sin talla (`NO_APLICA`). Se usa la primera clave que contiene la talla indicada (así `5` de Hollister va a la numérica US).
- **Etiqueta por zona** (pecho, cintura, cadera; pie en calzado), con cuerpo `c` y rango `[min, max]` de la talla: `c > max + 1` → ajustada; `c < min - 3` → holgada; si no, bien. Valores únicos se tratan como `v ± 2`. Pie: `+0,3` / `-1,0` y `± 0,4`. Valores en `OpcionesAjuste`.
- **Global**: ajustada si alguna zona lo es; si no, holgada si alguna lo es; si no, bien.
- **Talla ideal**: la talla de la tabla con menor penalización (cm fuera de rango; quedarse corta pesa ×2), desempate por cercanía al centro y luego por orden de tabla.
- **Medidas ausentes**: solo se evalúan las zonas con medida y dato en la talla; sin ninguna → `SIN_MEDIDAS`. Talla no indicada o inexistente → `TALLA_NO_ENCONTRADA` (con talla ideal). `Measurements.footCm` guarda el largo del pie (cm) y `MedidasUsuaria.de(m)` lo usa para el calzado.
- **T29**: el corte se añadirá a `OpcionesAjuste` / `ResultadoAjuste` sin cambiar la firma.
