package app.luxion.fitarchiveapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val Cacao = Color(0xFF1B1517)

private val EsquemaOscuroBase = darkColorScheme(
    secondary = Color(0xFFD9B79C),
    onSecondary = Color(0xFF3B2A1E),
    secondaryContainer = Color(0xFF53402F),
    onSecondaryContainer = MaderaClara,
    tertiary = Color(0xFF8FCDAB),
    onTertiary = Color(0xFF0B3823),
    background = Cacao,
    onBackground = Color(0xFFEDE3E5),
    surface = Color(0xFF221B1D),
    onSurface = Color(0xFFEDE3E5),
    surfaceContainerLowest = Color(0xFF150F11),
    surfaceContainerLow = Color(0xFF1F191B),
    surfaceContainer = Color(0xFF2A2326),
    surfaceContainerHigh = Color(0xFF352D30),
    onSurfaceVariant = Color(0xFFC9BEC1),
    outline = Color(0xFF978C8F),
    outlineVariant = Color(0xFF4A4144),
)

/** Esquema claro: el original con el [acento] sustituyendo a primary (el burdeos queda idéntico). */
internal fun esquemaClaro(base: ColorScheme, acento: Acento): ColorScheme {
    if (acento == Acento.BURDEOS) return base
    val primary = acentoLegible(acento.color, fondo = Color.White, oscuro = false)
    val (h, s, _) = acento.color.aHsl()
    return base.copy(
        primary = primary,
        onPrimary = textoSobre(primary),
        primaryContainer = hsl(h, min(s, 0.6f), 0.92f),
        onPrimaryContainer = hsl(h, min(s, 0.6f), 0.14f),
    )
}

internal fun esquemaOscuro(acento: Acento): ColorScheme {
    val primary = acentoLegible(acento.color, fondo = EsquemaOscuroBase.surface, oscuro = true)
    val (h, s, _) = acento.color.aHsl()
    return EsquemaOscuroBase.copy(
        primary = primary,
        onPrimary = textoSobre(primary),
        primaryContainer = hsl(h, min(s, 0.6f), 0.28f),
        onPrimaryContainer = hsl(h, min(s, 0.6f), 0.90f),
    )
}

/** Mueve la luminosidad del color (más clara en oscuro, más oscura en claro) hasta contraste >= 4.5 sobre [fondo]. */
private fun acentoLegible(color: Color, fondo: Color, oscuro: Boolean): Color {
    val (h, s, l0) = color.aHsl()
    var l = l0
    var c = hsl(h, s, l)
    repeat(50) {
        if (contraste(c, fondo) >= 4.5f) return c
        l = (l + if (oscuro) 0.02f else -0.02f).coerceIn(0f, 1f)
        c = hsl(h, s, l)
    }
    return c
}

private fun textoSobre(fondo: Color) =
    if (contraste(Color.White, fondo) >= contraste(Color.Black, fondo)) Color.White else Color.Black

private fun contraste(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

private fun Color.aHsl(): Triple<Float, Float, Float> {
    val mx = max(red, max(green, blue))
    val mn = min(red, min(green, blue))
    val l = (mx + mn) / 2f
    val d = mx - mn
    if (d == 0f) return Triple(0f, 0f, l)
    val s = d / (1f - abs(2f * l - 1f))
    val h = when (mx) {
        red -> ((green - blue) / d).mod(6f)
        green -> (blue - red) / d + 2f
        else -> (red - green) / d + 4f
    } * 60f
    return Triple(h, s, l)
}

private fun hsl(h: Float, s: Float, lum: Float): Color {
    val l = lum.coerceIn(0f, 1f)
    val c = (1f - abs(2f * l - 1f)) * s.coerceIn(0f, 1f)
    val x = c * (1f - abs((h / 60f).mod(2f) - 1f))
    val m = l - c / 2f
    val (r, g, b) = when ((h / 60f).toInt().mod(6)) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
