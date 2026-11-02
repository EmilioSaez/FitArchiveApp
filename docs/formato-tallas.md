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
