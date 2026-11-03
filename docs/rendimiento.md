# Rendimiento con armarios grandes (T47)

## Método

`RendimientoTest` (`shared/src/androidHostTest/.../rendimiento/`) siembra una BD en memoria con **1000 prendas, 400 outfits
(3-4 prendas) y 3 años de calendario** (~3400 usos) y mide la mediana de varias ejecuciones. Se lanza con
`./gradlew :shared:testAndroidHostTest --tests '*RendimientoTest*'` y escribe `shared/build/rendimiento.txt`.

Corre en la JVM del PC con SQLite en memoria: **sirve para comparar antes/después, no como tiempos absolutos de un móvil**
(en ART y con almacenamiento real serán varias veces mayores). La decodificación de imágenes no se puede medir ahí
(`BitmapFactory` es de Android); se razona por memoria y número de decodificaciones.

## Resultados (ms, mediana)

| Caso | Antes | Después |
|---|---:|---:|
| Lista de outfits: primera carga (400 outfits) | 85,4 | 15,0 |
| Lista de outfits: cambiar un favorito | 56,4 | 2,5 |
| Armario: primer estado | 17,4 | 6,7 |
| Armario: marcar favorita → nuevo estado | 16,7 | 5,4 |
| Arranque: contar usos de 3 años pendientes (una vez) | 241 | 90 |
| Arranque: contar usos con todo al día (cada apertura) | 1,5 | 1,4 |
| Outfit del día: de cero a sugerencias | 67,6 | ~60 (y fuera del hilo de UI) |
| Motor de sugerencias (1000 prendas) | 17-48 | 14-22 (ruido de JIT) |
| `Usos.todos()` (3400 filas) | 3,9 | 2,6 |

Sin cuello de botella: `AfinidadAprendida.desde` (~2,5 ms), filtros SQL (<1 ms), lectura de prendas (~5 ms), historial de usos.
El motor ya estaba acotado (máx. 8 prendas por ranura → ≤ ~5800 combinaciones), no hace falta más poda.

## Cambios

- **N+1 en outfits**: `ListaOutfitsViewModel` abría dos listeners por outfit (cada cambio en cualquier outfit relanzaba
  todas las consultas). Nuevo `OutfitRepository.observeAllConPrendas()`: 2 consultas en SQL. `OutfitDelDiaViewModel`
  dejó de llamar a `get(id)` por cada outfit.
- **Fotos**: `OutfitsScreen` decodificaba a 1024 px las fotos de *todos* los outfits al abrir (memoria y tiempo crecían con
  el armario). Ahora cada tarjeta carga solo sus miniaturas al componerse. Nueva `cargarMiniatura` (512 px) con **caché LRU
  de 48 MB** compartida por Armario, Outfits y Maleta: al volver a una celda no se decodifica de nuevo. Detalle y avatar
  siguen a 1024 px. En iOS el parámetro de tamaño aún no reduce la imagen (la caché sí aplica).
- **Armario**: una sola consulta (antes `observeFiltered` + `observeAll`); el filtro se aplica en memoria
  (`GarmentFilter.admite`) y los contadores de pestañas se calculan en una pasada.
- **Motor**: el desempate por ids solo construye texto en empates; la última fecha de uso por prenda se compara como
  texto ISO y solo se interpreta una fecha por prenda (antes una por cada fila del historial).
- **Outfit del día**: el motor y la afinidad se calculan en `Dispatchers.Default` (`calculo`), no en el hilo de UI; un
  recálculo nuevo descarta el anterior.
- **Usos**: `registrarHasta` aplica los usos acumulados con una lectura y una escritura por prenda.
- **Mappers**: `enumFromDb` usa `enumValueOf` (no crea un array por cada columna de cada fila).
- **O18 (tiempo)**: `CacheTiempo` guarda en memoria la última respuesta válida 30 min, por coordenadas aproximadas y por día;
  los fallos no se cachean (Reintentar sigue funcionando). Se crea una vez en `App`. No sobrevive a cerrar el proceso.

## Esquema de BD

Sin cambios (no hay migraciones). Los índices existentes bastan para las consultas medidas.

## Pendiente / no medido

- Tiempos reales en móvil (decodificación, desplazamiento de la cuadrícula): emulador solo para comprobar que la app arranca
  y no falla (con armario vacío). Conviene medir con un armario real en T50.
- Miniaturas persistidas en disco (hoy se decodifica la foto original reducida, una vez por sesión y celda).
