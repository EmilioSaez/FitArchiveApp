package app.luxion.fitarchiveapp.avatar

/**
 * Núcleo de una prenda: la zona ancha del recorte, sin lo fino que sobresale (tiras, lazos, cordones).
 * Coordenadas en píxeles de la imagen; los extremos [filaFin] y [colFin] son exclusivos.
 */
data class Nucleo(val colInicio: Int, val filaInicio: Int, val colFin: Int, val filaFin: Int) {
    val ancho get() = colFin - colInicio
    val alto get() = filaFin - filaInicio
}

/** Solo las prendas de cuerpo se colocan por su núcleo; calzado y accesorios usan el recorte entero. */
val Slot.usaNucleo get() = this == Slot.TOP || this == Slot.DRESS || this == Slot.OUTER || this == Slot.BOTTOM

/** Una fila pertenece al núcleo si su ancho llega a esta fracción del ancho máximo de fila. */
const val UMBRAL_NUCLEO = 0.25f

/** Un píxel cuenta como prenda a partir de este alfa (0..255); descarta el halo suave del recorte. */
const val ALFA_MINIMO_NUCLEO = 32

/**
 * Calcula el [Nucleo] de una máscara alfa de [ancho] x [alto] (fila a fila, un byte 0..255 por píxel).
 *
 * El ancho de cada fila es su número de píxeles opacos. El núcleo son las filas, de la primera a la última,
 * cuyo ancho es >= [umbral] x el máximo; y sus columnas, el rango opaco dentro de esas filas. Lo fino (una tira de
 * un top, un lazo) tiene un ancho muy por debajo del umbral y queda fuera. Con el 0,25 por defecto un tirante
 * suelto (~5 % del ancho) sale, y los hombros de un top, aunque queden separados por el escote, entran.
 *
 * Devuelve `null` si la máscara no es válida (dimensiones o tamaño incoherentes) o no tiene píxeles opacos.
 */
fun nucleoPrenda(
    alfa: ByteArray,
    ancho: Int,
    alto: Int,
    umbral: Float = UMBRAL_NUCLEO,
    alfaMinimo: Int = ALFA_MINIMO_NUCLEO,
): Nucleo? {
    if (ancho <= 0 || alto <= 0 || alfa.size < ancho.toLong() * alto) return null
    val anchoFila = IntArray(alto)
    var maximo = 0
    for (f in 0 until alto) {
        var n = 0
        val base = f * ancho
        for (c in 0 until ancho) if ((alfa[base + c].toInt() and 0xFF) >= alfaMinimo) n++
        anchoFila[f] = n
        if (n > maximo) maximo = n
    }
    if (maximo == 0) return null
    val minimo = maxOf(1f, maximo * umbral.coerceIn(0f, 1f))
    val primera = anchoFila.indexOfFirst { it >= minimo }
    val ultima = anchoFila.indexOfLast { it >= minimo }
    var colInicio = ancho
    var colFin = 0
    for (f in primera..ultima) {
        val base = f * ancho
        for (c in 0 until ancho) {
            if ((alfa[base + c].toInt() and 0xFF) >= alfaMinimo) {
                if (c < colInicio) colInicio = c
                if (c >= colFin) colFin = c + 1
            }
        }
    }
    return Nucleo(colInicio, primera, colFin, ultima + 1)
}
