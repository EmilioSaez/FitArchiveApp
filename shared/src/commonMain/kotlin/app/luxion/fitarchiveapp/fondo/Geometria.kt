package app.luxion.fitarchiveapp.fondo

import kotlin.math.max
import kotlin.math.roundToInt

/** Lógica pura de tamaños y recorte, compartida por las implementaciones de plataforma. */
object Geometria {
    /** Lado mayor máximo al que se segmenta: las fotos de cámara (12+ Mpx) sobran y gastan memoria. */
    const val LADO_MAXIMO = 1536

    /** Tamaño de salida: conserva la proporción y no supera [ladoMaximo] en el lado mayor. Nunca amplía. */
    fun ajustar(ancho: Int, alto: Int, ladoMaximo: Int = LADO_MAXIMO): Pair<Int, Int> {
        require(ancho > 0 && alto > 0) { "Dimensiones inválidas: ${ancho}x$alto" }
        require(ladoMaximo > 0) { "ladoMaximo debe ser positivo" }
        val lado = max(ancho, alto)
        if (lado <= ladoMaximo) return ancho to alto
        val f = ladoMaximo.toDouble() / lado
        return (ancho * f).roundToInt().coerceAtLeast(1) to (alto * f).roundToInt().coerceAtLeast(1)
    }

    /** Rectángulo [izq, arr, der, aba) (der/aba exclusivos). */
    data class Caja(val izq: Int, val arr: Int, val der: Int, val aba: Int) {
        val ancho get() = der - izq
        val alto get() = aba - arr
    }

    /**
     * Caja mínima con píxeles de alfa > [umbral] en una imagen ARGB de [ancho]x[alto]
     * (`pixeles` en orden de filas, alfa en el byte alto). `null` si no hay ninguno.
     */
    fun cajaOpaca(pixeles: IntArray, ancho: Int, alto: Int, umbral: Int = 8): Caja? {
        require(pixeles.size == ancho * alto) { "pixeles no coincide con ${ancho}x$alto" }
        var izq = ancho
        var der = -1
        var arr = alto
        var aba = -1
        for (y in 0 until alto) {
            for (x in 0 until ancho) {
                if ((pixeles[y * ancho + x] ushr 24) > umbral) {
                    if (x < izq) izq = x
                    if (x > der) der = x
                    if (y < arr) arr = y
                    aba = y
                }
            }
        }
        return if (aba < 0) null else Caja(izq, arr, der + 1, aba + 1)
    }

    /** Amplía [caja] en [margen] píxeles por lado sin salirse de la imagen. */
    fun conMargen(caja: Caja, margen: Int, ancho: Int, alto: Int) = Caja(
        (caja.izq - margen).coerceAtLeast(0),
        (caja.arr - margen).coerceAtLeast(0),
        (caja.der + margen).coerceAtMost(ancho),
        (caja.aba + margen).coerceAtMost(alto),
    )
}
