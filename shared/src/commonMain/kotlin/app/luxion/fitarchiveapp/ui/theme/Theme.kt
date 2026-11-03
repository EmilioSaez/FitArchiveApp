package app.luxion.fitarchiveapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Burdeos = Color(0xFF6D1A36)
val BurdeosClaro = Color(0xFFF3E4E9)
val Tinta = Color(0xFF1A1A1A)

private val FitArchiveColors = lightColorScheme(
    primary = Burdeos,
    onPrimary = Color.White,
    primaryContainer = BurdeosClaro,
    onPrimaryContainer = Burdeos,
    secondaryContainer = BurdeosClaro,
    onSecondaryContainer = Burdeos,
    background = Color.White,
    onBackground = Tinta,
    surface = Color.White,
    onSurface = Tinta,
    surfaceContainer = Color.White,
    onSurfaceVariant = Color(0xFF5E5A5B),
    outlineVariant = Color(0xFFE3D6DA),
)

private val Serif = FontFamily.Serif

private val Base = Typography()

private val FitArchiveTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Serif),
    displayMedium = Base.displayMedium.copy(fontFamily = Serif),
    displaySmall = Base.displaySmall.copy(fontFamily = Serif),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontFamily = Serif),
    titleSmall = Base.titleSmall.copy(fontFamily = Serif),
    // Texto corrido más aireado, como en una revista.
    bodyLarge = Base.bodyLarge.copy(lineHeight = 26.sp),
    bodyMedium = Base.bodyMedium.copy(lineHeight = 22.sp),
)

@Composable
fun FitArchiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FitArchiveColors,
        typography = FitArchiveTypography,
        content = content,
    )
}
