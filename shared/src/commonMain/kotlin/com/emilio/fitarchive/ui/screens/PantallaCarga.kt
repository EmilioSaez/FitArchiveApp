package com.emilio.fitarchive.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emilio.fitarchive.ui.theme.Burdeos
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private const val MS_TITULO = 350
private const val MS_PAUSA = 250
private const val MS_PUERTAS = 650

/**
 * Pantalla de carga: dos puertas de armario cerradas con el título; el título aparece,
 * las puertas se abren hacia los lados y se llama a [onTerminado] (~1,3 s en total).
 */
@Composable
fun PantallaCarga(onTerminado: () -> Unit, modifier: Modifier = Modifier) {
    val alTerminar = rememberUpdatedState(onTerminado)
    val titulo = remember { Animatable(0f) }
    val apertura = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        titulo.animateTo(1f, tween(MS_TITULO, easing = LinearEasing))
        kotlinx.coroutines.delay(MS_PAUSA.toLong())
        coroutineScope {
            val puertas = async { apertura.animateTo(1f, tween(MS_PUERTAS, easing = FastOutSlowInEasing)) }
            val fundido = async { titulo.animateTo(0f, tween(MS_PUERTAS / 2, easing = LinearEasing)) }
            puertas.await()
            fundido.await()
        }
        alTerminar.value()
    }

    BoxWithConstraints(modifier.fillMaxSize().background(Color.White)) {
        val mitad = maxWidth / 2
        // Cada puerta se desplaza fuera de la pantalla hacia su lado.
        Puerta(
            modifier = Modifier.align(Alignment.CenterStart).width(mitad),
            desplazamiento = { -apertura.value * size.width },
            bisagraIzquierda = true,
        )
        Puerta(
            modifier = Modifier.align(Alignment.CenterEnd).width(mitad),
            desplazamiento = { apertura.value * size.width },
            bisagraIzquierda = false,
        )
        Text(
            "FitArchive",
            modifier = Modifier.align(Alignment.Center).graphicsLayer { alpha = titulo.value },
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun Puerta(
    modifier: Modifier,
    desplazamiento: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Float,
    bisagraIzquierda: Boolean,
) {
    Box(
        modifier
            .fillMaxHeight()
            .graphicsLayer { translationX = desplazamiento() }
            .background(Burdeos),
    ) {
        // Tirador y filo central para que se lean como puertas de armario.
        Box(
            Modifier
                .align(if (bisagraIzquierda) Alignment.CenterEnd else Alignment.CenterStart)
                .fillMaxHeight()
                .width(2.dp)
                .background(Color.White.copy(alpha = 0.35f)),
        )
        Box(
            Modifier
                .align(if (bisagraIzquierda) Alignment.CenterEnd else Alignment.CenterStart)
                .width(6.dp)
                .fillMaxHeight(0.12f)
                .background(Color.White.copy(alpha = 0.7f)),
        )
    }
}
