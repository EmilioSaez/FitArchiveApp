package app.luxion.fitarchiveapp.ui.viaje

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.foto.cargarMiniatura
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.resources.Res
import app.luxion.fitarchiveapp.resources.maleta_cuerpo
import app.luxion.fitarchiveapp.resources.maleta_tapa
import app.luxion.fitarchiveapp.ui.theme.rememberReducirMovimiento
import org.jetbrains.compose.resources.painterResource

private val CurvaTapa = CubicBezierEasing(0.3f, 0.8f, 0.3f, 1f)

/**
 * Maleta de 206 × 290 dp. Al abrir, la tapa gira sobre su borde superior (rotationX 0 → -62°) y sube 196 dp en 800 ms;
 * después las fotos de las [prendas] guardadas entran en el forro (2 columnas), con fundido y subida de 24 dp,
 * escalonadas 70 ms a partir de los 350 ms. Cerrada, la tapa lo cubre todo.
 */
@Composable
fun MaletaAnimada(prendas: List<Garment>, abierta: Boolean, onAlternar: () -> Unit, modifier: Modifier = Modifier) {
    val reducir = rememberReducirMovimiento()
    val tapa = remember { Animatable(0f) }
    LaunchedEffect(abierta, reducir) {
        if (reducir) tapa.snapTo(if (abierta) 1f else 0f) else tapa.animateTo(if (abierta) 1f else 0f, tween(800, easing = CurvaTapa))
    }

    Box(
        modifier
            .size(206.dp, 290.dp)
            .semantics { contentDescription = if (abierta) "Cerrar la maleta" else "Abrir la maleta" }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onAlternar),
    ) {
        Image(painterResource(Res.drawable.maleta_cuerpo), contentDescription = null, modifier = Modifier.size(206.dp, 290.dp))
        // Forro: x = 18..188, y = 42..252 (170 × 210 dp), rejilla de 2 columnas.
        Column(
            Modifier.offset(18.dp, 42.dp).size(170.dp, 210.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            prendas.take(6).chunked(2).forEachIndexed { fila, par ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    par.forEachIndexed { col, prenda ->
                        FotoEnForro(prenda, abierta, indice = fila * 2 + col, reducir = reducir)
                    }
                }
            }
        }
        Image(
            painterResource(Res.drawable.maleta_tapa),
            contentDescription = null,
            modifier = Modifier
                .offset(y = 24.dp)
                .size(206.dp, 250.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    cameraDistance = 16f * density
                    rotationX = -62f * tapa.value
                    // El borde inferior de la tapa queda sobre el superior de la carcasa, como una bisagra.
                    translationY = -112.dp.toPx() * tapa.value
                },
        )
    }
}

@Composable
private fun FotoEnForro(prenda: Garment, visible: Boolean, indice: Int, reducir: Boolean) {
    val imagen by produceState<ImageBitmap?>(null, prenda.photoPath) { value = cargarMiniatura(prenda.photoPath) }
    val progreso by animateFloatAsState(
        if (visible) 1f else 0f,
        if (reducir) tween(0) else tween(350, delayMillis = if (visible) 350 + 70 * indice else 0),
    )
    Box(
        Modifier
            .size(82.dp, 100.dp)
            .graphicsLayer {
                alpha = progreso
                translationY = 24.dp.toPx() * (1f - progreso)
            }
            .clip(RoundedCornerShape(8.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
    ) {
        imagen?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}
