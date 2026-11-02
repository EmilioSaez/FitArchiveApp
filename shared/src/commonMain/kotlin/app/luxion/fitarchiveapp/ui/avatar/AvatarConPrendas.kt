package app.luxion.fitarchiveapp.ui.avatar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.luxion.fitarchiveapp.avatar.AjustePrenda
import app.luxion.fitarchiveapp.avatar.Capa
import app.luxion.fitarchiveapp.avatar.CapaAvatar
import app.luxion.fitarchiveapp.avatar.LIENZO_ALTO
import app.luxion.fitarchiveapp.avatar.LIENZO_ANCHO
import app.luxion.fitarchiveapp.avatar.Slot
import app.luxion.fitarchiveapp.avatar.Subtipo
import app.luxion.fitarchiveapp.avatar.ajustar
import app.luxion.fitarchiveapp.avatar.capasOrdenadas
import app.luxion.fitarchiveapp.avatar.encajar
import app.luxion.fitarchiveapp.resources.Res
import app.luxion.fitarchiveapp.resources.avatar_arms
import app.luxion.fitarchiveapp.resources.avatar_body
import app.luxion.fitarchiveapp.resources.avatar_face
import app.luxion.fitarchiveapp.resources.avatar_hair_back
import app.luxion.fitarchiveapp.resources.avatar_hair_front
import org.jetbrains.compose.resources.painterResource

/** Prenda (foto recortada con alfa) y subtipo que decide su slot y su caja. */
class PrendaAvatar(val imagen: ImageBitmap, val subtipo: Subtipo, val ajuste: AjustePrenda = AjustePrenda.Ninguno)

/**
 * Dibuja el avatar y, encima, las [prendas] escaladas a la caja de su slot (más su ajuste manual) en el orden de capas.
 * Si hay varias en el mismo slot gana la última; un vestido oculta top y bottom.
 */
@Composable
fun AvatarConPrendas(prendas: List<PrendaAvatar>, modifier: Modifier = Modifier) {
    val capasAvatar: Map<CapaAvatar, Painter> = mapOf(
        CapaAvatar.HAIR_BACK to painterResource(Res.drawable.avatar_hair_back),
        CapaAvatar.BODY to painterResource(Res.drawable.avatar_body),
        CapaAvatar.FACE to painterResource(Res.drawable.avatar_face),
        CapaAvatar.ARMS to painterResource(Res.drawable.avatar_arms),
        CapaAvatar.HAIR_FRONT to painterResource(Res.drawable.avatar_hair_front),
    )
    val porSlot: Map<Slot, PrendaAvatar> = prendas.associateBy { it.subtipo.slot }
    val capas = capasOrdenadas(porSlot.keys)

    Canvas(modifier.aspectRatio(LIENZO_ANCHO / LIENZO_ALTO)) {
        val escala = size.height / LIENZO_ALTO
        scale(escala, escala, pivot = Offset.Zero) {
            val lienzo = Size(LIENZO_ANCHO, LIENZO_ALTO)
            capas.forEach { capa ->
                when (capa) {
                    is Capa.Avatar -> capasAvatar.getValue(capa.capa).let { with(it) { draw(lienzo) } }
                    is Capa.Prenda -> {
                        val p = porSlot.getValue(capa.slot)
                        val r = ajustar(encajar(p.subtipo.caja, p.imagen.width, p.imagen.height, capa.slot.alineacion), p.ajuste)
                        val dibujar = {
                            drawImage(
                                p.imagen,
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(p.imagen.width, p.imagen.height),
                                dstOffset = IntOffset(0, 0),
                                dstSize = IntSize(1, 1),
                            )
                        }
                        // drawImage solo admite destino entero: se dibuja a 1x1 y se escala con la matriz.
                        translate(r.x, r.y) {
                            scale(r.w, r.h, pivot = Offset.Zero) {
                                if (p.subtipo.reflejar) scale(-1f, 1f, pivot = Offset(0.5f, 0f)) { dibujar() } else dibujar()
                            }
                        }
                    }
                }
            }
        }
    }
}
