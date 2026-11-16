package app.luxion.fitarchiveapp.foto

import kotlin.math.max
import kotlin.math.roundToInt

/** Lado mayor de la foto que se envía a la IA: con más resolución solo se paga más sin catalogar mejor. */
const val LADO_IA = 1024

/** Calidad JPEG (0-100) de la foto que se envía a la IA. */
const val CALIDAD_JPEG_IA = 80

/**
 * Prepara una foto para enviarla a la IA: orientación aplicada, lado mayor de [LADO_IA] como máximo, fondo blanco
 * donde haya transparencia y JPEG de calidad [CALIDAD_JPEG_IA]. El resultado no lleva EXIF (ubicación, fecha,
 * cámara…) y solo vive en memoria. Null si [bytes] no se puede decodificar.
 */
expect suspend fun reducirParaIa(bytes: ByteArray): ByteArray?

/** Dimensiones (ancho, alto) tras reducir una imagen de [ancho]×[alto] para que su lado mayor no pase de [lado]; nunca amplía. */
fun dimensionesReducidas(ancho: Int, alto: Int, lado: Int = LADO_IA): Pair<Int, Int> {
    val mayor = max(ancho, alto)
    if (mayor <= lado) return ancho to alto
    val escala = lado.toDouble() / mayor
    return max(1, (ancho * escala).roundToInt()) to max(1, (alto * escala).roundToInt())
}
