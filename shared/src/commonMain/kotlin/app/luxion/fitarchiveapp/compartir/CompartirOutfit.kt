package app.luxion.fitarchiveapp.compartir

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import app.luxion.fitarchiveapp.avatar.CapaAvatar
import app.luxion.fitarchiveapp.avatar.LIENZO_ALTO
import app.luxion.fitarchiveapp.ui.avatar.PrendaAvatar
import app.luxion.fitarchiveapp.ui.avatar.dibujarAvatarConPrendas
import app.luxion.fitarchiveapp.ui.avatar.rememberCapasAvatar
import app.luxion.fitarchiveapp.ui.theme.Tinta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Gris = Color(0xFF8A8A8A)

/**
 * Dibuja la tarjeta del outfit (estilo revista): fondo blanco, nombre en serif, avatar con las prendas y
 * "FitArchive" como marca discreta. Usa densidad 1 (sp = px) para que el resultado no dependa de la pantalla.
 */
fun renderizarTarjetaOutfit(
    nombre: String,
    prendas: List<PrendaAvatar>,
    capasAvatar: Map<CapaAvatar, Painter>,
    medidor: TextMeasurer,
): ImageBitmap {
    val d = disposicionTarjeta()
    val bitmap = ImageBitmap(d.ancho, d.alto)
    val tamano = Size(d.ancho.toFloat(), d.alto.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), tamano) {
        drawRect(Color.White)

        // El título baja de tamaño hasta caber entre su borde superior y el filete (nombres largos ocupan 2 líneas).
        val alturaTitulo = d.filete.y - d.tituloY - 12f
        var tamanoTitulo = TITULO_MAX_SP
        var titulo = medirTitulo(medidor, nombre, tamanoTitulo, d.anchoTitulo)
        while ((titulo.size.height > alturaTitulo || titulo.hasVisualOverflow) && tamanoTitulo > TITULO_MIN_SP) {
            tamanoTitulo -= 6
            titulo = medirTitulo(medidor, nombre, tamanoTitulo, d.anchoTitulo)
        }
        drawText(titulo, topLeft = Offset((tamano.width - titulo.size.width) / 2f, d.tituloY))

        drawRect(Tinta, Offset(d.filete.x, d.filete.y), Size(d.filete.w, d.filete.h))

        translate(d.avatar.x, d.avatar.y) {
            dibujarAvatarConPrendas(capasAvatar, prendas, d.avatar.h / LIENZO_ALTO)
        }

        val marca = medidor.measure(
            MARCA.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 30.sp,
                letterSpacing = 10.sp,
                color = Gris,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
        drawText(marca, topLeft = Offset((tamano.width - marca.size.width) / 2f, d.marcaY - marca.size.height / 2f))
    }
    return bitmap
}

private const val TITULO_MAX_SP = 84
private const val TITULO_MIN_SP = 36

private fun medirTitulo(medidor: TextMeasurer, nombre: String, tamanoSp: Int, anchoMax: Float) = medidor.measure(
    tituloTarjeta(nombre),
    style = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = tamanoSp.sp,
        color = Tinta,
        textAlign = TextAlign.Center,
    ),
    overflow = TextOverflow.Ellipsis,
    maxLines = 2,
    constraints = Constraints(maxWidth = anchoMax.toInt()),
)

/** Exporta un outfit como imagen y abre el share sheet del sistema. */
class CompartirOutfit internal constructor(
    private val ejecutar: (nombre: String, prendas: List<PrendaAvatar>, alFallar: () -> Unit) -> Unit,
) {
    /**
     * Renderiza [prendas] sobre el avatar, con [nombre] como título, y abre el selector de apps.
     * [alFallar] se llama si no se pudo generar o abrir la imagen.
     */
    fun compartir(nombre: String, prendas: List<PrendaAvatar>, alFallar: () -> Unit = {}) =
        ejecutar(nombre, prendas, alFallar)
}

/** Callback de compartir para el editor (T20) y la lista de outfits (T19). */
@Composable
fun rememberCompartirOutfit(): CompartirOutfit {
    val capasAvatar by rememberUpdatedState(rememberCapasAvatar())
    val medidor = rememberTextMeasurer()
    val imagen = rememberCompartirImagen()
    val scope = rememberCoroutineScope()
    return remember(medidor, imagen, scope) {
        CompartirOutfit { nombre, prendas, alFallar ->
            scope.launch {
                try {
                    val bitmap = renderizarTarjetaOutfit(nombre, prendas, capasAvatar, medidor)
                    val png = withContext(Dispatchers.Default) { bitmap.aPng() }
                    imagen.compartirPng(png, nombreArchivoOutfit(nombre))
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    alFallar()
                }
            }
        }
    }
}
