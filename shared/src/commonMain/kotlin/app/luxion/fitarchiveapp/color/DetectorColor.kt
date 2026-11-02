package app.luxion.fitarchiveapp.color

import app.luxion.fitarchiveapp.model.GarmentColor
import kotlin.math.pow

/** Referencia sRGB (0xRRGGBB) de cada color del enum, usada para encontrar el más cercano. MULTI no tiene referencia. */
private val REFERENCIAS: Map<GarmentColor, Int> = mapOf(
    GarmentColor.BLACK to 0x141414,
    GarmentColor.WHITE to 0xF5F5F5,
    GarmentColor.GREY to 0x808080,
    GarmentColor.BEIGE to 0xDCC8A5,
    GarmentColor.BROWN to 0x6E4628,
    GarmentColor.RED to 0xC81E28,
    GarmentColor.BURGUNDY to 0x6E1428,
    GarmentColor.PINK to 0xF096B4,
    GarmentColor.ORANGE to 0xF0821E,
    GarmentColor.YELLOW to 0xF0D732,
    GarmentColor.GREEN to 0x328C3C,
    GarmentColor.BLUE to 0x326ED2,
    GarmentColor.NAVY to 0x192358,
    GarmentColor.PURPLE to 0x783C96,
)

private const val ALFA_MINIMO = 128
private const val MAX_MUESTRAS = 40_000

/** Si ningún color reúne al menos esta proporción de los píxeles visibles, la prenda es multicolor. */
private const val UMBRAL_DOMINANTE = 0.4

private class Lab(val l: Double, val a: Double, val b: Double)

private fun lineal(c: Int): Double {
    val v = c / 255.0
    return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
}

private fun f(t: Double): Double = if (t > 0.008856) t.pow(1.0 / 3.0) else 7.787 * t + 16.0 / 116.0

private fun aLab(rgb: Int): Lab {
    val r = lineal(rgb shr 16 and 0xFF)
    val g = lineal(rgb shr 8 and 0xFF)
    val b = lineal(rgb and 0xFF)
    val x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
    val y = 0.2126 * r + 0.7152 * g + 0.0722 * b
    val z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883
    return Lab(116 * f(y) - 16, 500 * (f(x) - f(y)), 200 * (f(y) - f(z)))
}

private val REFERENCIAS_LAB: List<Pair<GarmentColor, Lab>> = REFERENCIAS.map { (c, rgb) -> c to aLab(rgb) }

/** El [GarmentColor] (sin MULTI) más cercano a [rgb] (0xRRGGBB) en el espacio Lab. */
fun colorMasCercano(rgb: Int): GarmentColor {
    val lab = aLab(rgb)
    return REFERENCIAS_LAB.minBy { (_, ref) ->
        val dl = lab.l - ref.l
        val da = lab.a - ref.a
        val db = lab.b - ref.b
        dl * dl + da * da + db * db
    }.first
}

/**
 * Color principal de una imagen a partir de sus píxeles ARGB (no premultiplicados).
 * Ignora los píxeles transparentes (el fondo quitado por T07): cada píxel visible vota por el color
 * del enum más cercano y gana el más votado. Si ninguno llega al 40 %, devuelve [GarmentColor.MULTI].
 * Devuelve null si no hay ningún píxel visible.
 */
fun detectarColorPrincipal(pixeles: IntArray): GarmentColor? {
    if (pixeles.isEmpty()) return null
    val paso = maxOf(1, pixeles.size / MAX_MUESTRAS)
    val votos = HashMap<GarmentColor, Int>()
    val cache = HashMap<Int, GarmentColor>()
    var visibles = 0
    var i = 0
    while (i < pixeles.size) {
        val p = pixeles[i]
        if ((p ushr 24) >= ALFA_MINIMO) {
            val rgb = p and 0xFFFFFF
            val c = cache.getOrPut(rgb) { colorMasCercano(rgb) }
            votos[c] = (votos[c] ?: 0) + 1
            visibles++
        }
        i += paso
    }
    if (visibles == 0) return null
    val mejor = votos.maxByOrNull { it.value }!!
    return if (mejor.value.toDouble() / visibles >= UMBRAL_DOMINANTE) mejor.key else GarmentColor.MULTI
}
