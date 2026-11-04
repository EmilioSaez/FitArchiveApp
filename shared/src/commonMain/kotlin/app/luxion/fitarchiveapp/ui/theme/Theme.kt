package app.luxion.fitarchiveapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.luxion.fitarchiveapp.resources.Res
import app.luxion.fitarchiveapp.resources.dm_sans
import app.luxion.fitarchiveapp.resources.playfair_display
import app.luxion.fitarchiveapp.resources.playfair_display_italic
import org.jetbrains.compose.resources.Font

val Burdeos = Color(0xFF6D1A36)
val BurdeosClaro = Color(0xFFF3E4E9)
val Tinta = Color(0xFF1A1A1A)

/** Madera de perchas, armario y maleta (secondary). */
val Madera = Color(0xFF7A5A43)
val MaderaClara = Color(0xFFEFE3D8)
val MaderaOscura = Color(0xFF3B2A1E)

/** Prenda metida en la maleta (tertiary). */
val Verde = Color(0xFF2F6B4F)

/** Papel: fondo de pantalla. */
val Papel = Color(0xFFFBF8F6)

private val FitArchiveColors = lightColorScheme(
    primary = Burdeos,
    onPrimary = Color.White,
    primaryContainer = BurdeosClaro,
    onPrimaryContainer = Color(0xFF3A0A1B),
    secondary = Madera,
    onSecondary = Color.White,
    secondaryContainer = MaderaClara,
    onSecondaryContainer = MaderaOscura,
    tertiary = Verde,
    onTertiary = Color.White,
    background = Papel,
    onBackground = Tinta,
    surface = Color.White,
    onSurface = Tinta,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F0EE),
    surfaceContainer = Color(0xFFEFE7E4),
    surfaceContainerHigh = Color(0xFFE6DCD8),
    onSurfaceVariant = Color(0xFF5E5A5B),
    outline = Color(0xFF8A7F82),
    outlineVariant = Color(0xFFE3D6DA),
)

@OptIn(ExperimentalTextApi::class)
@Composable
private fun familiaPlayfair(): FontFamily {
    val normal = Res.font.playfair_display
    val cursiva = Res.font.playfair_display_italic
    val pesos = listOf(FontWeight.Normal, FontWeight.Bold)
    val fuentes = pesos.map { Font(normal, weight = it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight))) } +
        Font(cursiva, weight = FontWeight.Normal, style = FontStyle.Italic, variationSettings = FontVariation.Settings(FontVariation.weight(400)))
    return remember(fuentes) { FontFamily(fuentes) }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun familiaDmSans(): FontFamily {
    val recurso = Res.font.dm_sans
    val pesos = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
    val fuentes = pesos.map { Font(recurso, weight = it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight))) }
    return remember(fuentes) { FontFamily(fuentes) }
}

private fun tipografia(display: FontFamily, texto: FontFamily) = Typography(
    displaySmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = texto, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = texto, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = texto, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = texto, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = texto, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = texto, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.6.sp),
)

/** Cartela de la galería: Playfair 15/20 cursiva. */
@Composable
fun estiloCartela(): TextStyle = TextStyle(
    fontFamily = familiaPlayfair(),
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 20.sp,
)

@Composable
fun FitArchiveTheme(content: @Composable () -> Unit) {
    val tipos = tipografia(familiaPlayfair(), familiaDmSans())
    MaterialTheme(
        colorScheme = FitArchiveColors,
        typography = tipos,
        content = content,
    )
}
