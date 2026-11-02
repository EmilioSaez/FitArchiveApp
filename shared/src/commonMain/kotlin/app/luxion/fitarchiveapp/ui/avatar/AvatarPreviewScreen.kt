package app.luxion.fitarchiveapp.ui.avatar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.avatar.Subtipo

/** Preview mínima (T17): avatar con prendas de ejemplo; T18 la sustituye por el editor. */
@Composable
fun AvatarPreviewScreen(modifier: Modifier = Modifier) {
    val ejemplos = remember { prendasDeEjemplo() }
    var activas by remember { mutableStateOf(setOf(Subtipo.TOP_MANGA_CORTA, Subtipo.BOTTOM_PANTALON_LARGO, Subtipo.SHOES_PAR)) }
    Row(modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AvatarConPrendas(
            prendas = activas.mapNotNull { s -> ejemplos[s]?.let { PrendaAvatar(it, s) } },
            modifier = Modifier.fillMaxHeight().weight(1f).align(Alignment.CenterVertically),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ejemplos.keys.forEach { s ->
                FilterChip(
                    selected = s in activas,
                    onClick = { activas = if (s in activas) activas - s else activas + s },
                    label = { Text(s.name.lowercase()) },
                )
            }
        }
    }
}

private fun prendasDeEjemplo(): Map<Subtipo, ImageBitmap> = mapOf(
    Subtipo.TOP_MANGA_CORTA to rect(188, 270, Color(0xFF6B8FB3)),
    Subtipo.TOP_SIN_MANGAS_CORTO to rect(160, 260, Color(0xFFE0B04F)),
    Subtipo.OUTER_CHAQUETA to rect(232, 330, Color(0xFF7A5C45)),
    Subtipo.BOTTOM_PANTALON_LARGO to rect(124, 550, Color(0xFF3B4A63)),
    Subtipo.BOTTOM_FALDA_MIDI to rect(132, 310, Color(0xFFB5586B)),
    Subtipo.DRESS_MIDI to rect(192, 500, Color(0xFF5E8C6A)),
    Subtipo.SHOES_PAR to rect(104, 72, Color(0xFF2F2A2A)),
    Subtipo.ACC_SOMBRERO_GORRO to rect(124, 76, Color(0xFFC9A66B)),
    Subtipo.ACC_GAFAS to rect(80, 28, Color(0xFF222222)),
    Subtipo.ACC_COLLAR to rect(64, 70, Color(0xFFD4AF37)),
    Subtipo.ACC_CINTURON to rect(108, 36, Color(0xFF8B4513)),
    Subtipo.ACC_BOLSO_MANO_DER to rect(110, 150, Color(0xFFA34A5E)),
)

/** Imagen con alfa: rectángulo redondeado de color sobre fondo transparente (como una foto recortada). */
private fun rect(ancho: Int, alto: Int, color: Color): ImageBitmap {
    val bmp = ImageBitmap(ancho, alto)
    Canvas(bmp).drawRoundRect(
        0f, 0f, ancho.toFloat(), alto.toFloat(), 12f, 12f,
        Paint().apply { this.color = color },
    )
    return bmp
}
