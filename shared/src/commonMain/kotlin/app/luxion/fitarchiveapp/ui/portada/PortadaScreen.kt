package app.luxion.fitarchiveapp.ui.portada

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import app.luxion.fitarchiveapp.foto.rememberMiniaturas
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.ui.theme.rememberReducirMovimiento
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.luxion.fitarchiveapp.resources.Res
import app.luxion.fitarchiveapp.resources.armario_cuerpo
import app.luxion.fitarchiveapp.resources.armario_puerta
import app.luxion.fitarchiveapp.ui.theme.estiloCartela
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private val CurvaPuerta = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)
private const val ANGULO_PUERTA = 108f

/**
 * Entrada de la app: el armario se abre solo 600 ms después de mostrarse; tocarlo lo cierra o lo abre de nuevo.
 * [prendas] y [looks] son los totales que se leen bajo el título.
 */
@Composable
fun PortadaScreen(
    prendas: List<Garment>,
    looks: Int,
    onVerOutfitDeHoy: () -> Unit,
    onAnadirPrenda: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducir = rememberReducirMovimiento()
    val apertura = remember { Animatable(0f) }
    var abierto by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val hayPrendas = prendas.isNotEmpty()

    LaunchedEffect(reducir) {
        if (reducir) {
            // Con "reducir movimiento" la portada sale ya abierta.
            abierto = true
            apertura.snapTo(1f)
        } else {
            delay(600)
            abierto = true
            apertura.animateTo(1f, tween(900, easing = CurvaPuerta))
        }
    }
    // Hasta 5 fotos cuelgan de la barra: favoritas primero y, si no, las más usadas.
    val colgadas = remember(prendas) {
        prendas.sortedWith(compareByDescending<Garment> { it.favorite }.thenByDescending { it.totalWears }).take(5)
    }
    val miniaturas = rememberMiniaturas(colgadas.map { it.photoPath })

    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 32.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("FITARCHIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Tu armario", style = MaterialTheme.typography.displaySmall)
            Text(
                if (hayPrendas) "${prendas.size} prendas · $looks looks guardados" else "Empieza añadiendo tu primera prenda",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val descripcion = if (abierto) "Cerrar el armario" else "Abrir el armario"
            Box(
                Modifier
                    .size(272.dp, 440.dp)
                    .semantics { contentDescription = descripcion }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        abierto = !abierto
                        scope.launch { apertura.animateTo(if (abierto) 1f else 0f, tween(900, easing = CurvaPuerta)) }
                    },
            ) {
                Image(painterResource(Res.drawable.armario_cuerpo), contentDescription = null, modifier = Modifier.size(272.dp, 440.dp))
                // Prendas colgadas de la barra superior (y = 62): miniaturas de 40×52, radio 4, giro alterno.
                Row(Modifier.padding(start = 20.dp, top = 66.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colgadas.forEachIndexed { i, prenda ->
                        Box(
                            Modifier.size(40.dp, 52.dp).rotate(listOf(-4f, 0f, 4f)[i % 3])
                                .clip(RoundedCornerShape(4.dp)).background(Color.White),
                        ) {
                            miniaturas[prenda.photoPath]?.let {
                                Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
                // Puerta izquierda (x = 10) y derecha espejada (x = 146); y = 28.
                Puerta(Modifier.padding(start = 10.dp, top = 28.dp), apertura.value, izquierda = true)
                Puerta(Modifier.padding(start = 136.dp, top = 28.dp), apertura.value, izquierda = false)
            }
        }

        Column(
            Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (abierto) {
                Button(onClick = if (hayPrendas) onVerOutfitDeHoy else onAnadirPrenda, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(if (hayPrendas) "Ver el outfit de hoy" else "Añadir mi primera prenda", style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    "Toca el armario para volver a verlo abrirse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text("Abriendo tu armario…", style = estiloCartela().copy(fontSize = 17.sp))
            }
        }
    }
}

@Composable
private fun Puerta(modifier: Modifier, progreso: Float, izquierda: Boolean) {
    Box(
        modifier
            .size(126.dp, 380.dp)
            .graphicsLayer {
                cameraDistance = 11f * density
                transformOrigin = TransformOrigin(if (izquierda) 0f else 1f, 0.5f)
                rotationY = (if (izquierda) -ANGULO_PUERTA else ANGULO_PUERTA) * progreso
            },
    ) {
        Image(
            painterResource(Res.drawable.armario_puerta),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { if (!izquierda) scaleX = -1f },
        )
    }
}
