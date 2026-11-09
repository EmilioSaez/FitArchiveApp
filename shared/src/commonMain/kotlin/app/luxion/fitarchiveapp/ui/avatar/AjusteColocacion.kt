package app.luxion.fitarchiveapp.ui.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.avatar.AjustePrenda
import app.luxion.fitarchiveapp.avatar.LIENZO_ALTO
import app.luxion.fitarchiveapp.avatar.LIENZO_ANCHO
import app.luxion.fitarchiveapp.avatar.ajusteManual
import app.luxion.fitarchiveapp.avatar.subtipoEfectivo
import app.luxion.fitarchiveapp.model.Garment

/**
 * Colocación manual de una [prenda] sobre el avatar: se arrastra con un dedo y se escala con el gesto de pellizco.
 * Los cambios son locales hasta pulsar "Guardar colocación" ([onGuardar]); "Restablecer" ([onRestablecer])
 * vuelve a la colocación automática y la borra de la prenda.
 */
@Composable
fun AjusteColocacion(
    prenda: Garment,
    imagen: ImageBitmap,
    onGuardar: (AjustePrenda) -> Unit,
    onRestablecer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val caja = prenda.subtipoEfectivo.caja
    val guardado = prenda.ajusteManual
    var ajuste by remember(prenda.id, guardado) { mutableStateOf(guardado) }
    var tamano by remember { mutableStateOf(IntSize.Zero) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Arrastra la prenda y pellizca para cambiar su tamaño.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .height(AJUSTE_ALTO)
                    .aspectRatio(LIENZO_ANCHO / LIENZO_ALTO)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .onSizeChanged { tamano = it }
                    .pointerInput(caja) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (tamano.height == 0) return@detectTransformGestures
                            val unidadesPorPixel = LIENZO_ALTO / tamano.height
                            ajuste = AjustePrenda(
                                dx = ajuste.dx + pan.x * unidadesPorPixel,
                                dy = ajuste.dy + pan.y * unidadesPorPixel,
                                escala = ajuste.escala * zoom,
                            ).limitado(caja)
                        }
                    },
            ) {
                AvatarConPrendas(
                    listOf(PrendaAvatar.de(prenda.copy(ajuste = null), imagen, ajuste)),
                    Modifier.fillMaxSize(),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { ajuste = AjustePrenda.Ninguno; onRestablecer() },
                enabled = ajuste != AjustePrenda.Ninguno || prenda.ajuste != null,
                modifier = Modifier.weight(1f),
            ) { Text("Restablecer") }
            Button(
                onClick = { onGuardar(ajuste) },
                enabled = ajuste != guardado,
                modifier = Modifier.weight(1f),
            ) { Text("Guardar colocación") }
        }
    }
}

private val AJUSTE_ALTO = 420.dp
