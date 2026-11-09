package app.luxion.fitarchiveapp.ui.avatar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.compositionLocalOf
import app.luxion.fitarchiveapp.avatar.AjustePrenda
import app.luxion.fitarchiveapp.avatar.DimensionesPrendaCalc
import app.luxion.fitarchiveapp.avatar.EscalaAvatar
import app.luxion.fitarchiveapp.avatar.PerfilCorporal
import app.luxion.fitarchiveapp.avatar.Nucleo
import app.luxion.fitarchiveapp.avatar.Rect
import app.luxion.fitarchiveapp.avatar.cajaAccesorio
import app.luxion.fitarchiveapp.avatar.colocacionReal
import app.luxion.fitarchiveapp.model.Fit
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.tallas.TablaTallas
import app.luxion.fitarchiveapp.avatar.Capa
import app.luxion.fitarchiveapp.avatar.LIENZO_ALTO
import app.luxion.fitarchiveapp.avatar.LIENZO_ANCHO
import app.luxion.fitarchiveapp.avatar.Slot
import app.luxion.fitarchiveapp.avatar.Subtipo
import app.luxion.fitarchiveapp.avatar.ajustar
import app.luxion.fitarchiveapp.avatar.capasOrdenadas
import app.luxion.fitarchiveapp.avatar.encajar
import app.luxion.fitarchiveapp.avatar.nucleoPrenda
import app.luxion.fitarchiveapp.avatar.usaNucleo

/** Datos de la prenda necesarios para calcular su tamaño real (cm). */
class DatosPrenda(val tipo: GarmentType, val talla: String?, val marca: String?, val corte: Fit)

/**
 * Prenda (foto recortada con alfa) y subtipo que decide su slot. Con [datos] se coloca a escala real;
 * sin ellos, en la caja base del subtipo.
 */
class PrendaAvatar(
    val imagen: ImageBitmap,
    val subtipo: Subtipo,
    val ajuste: AjustePrenda = AjustePrenda.Ninguno,
    val datos: DatosPrenda? = null,
) {
    /** Zona ancha del recorte (sin tiras ni lazos) con la que se coloca; solo en prendas de cuerpo. */
    val nucleo: Nucleo? by lazy { if (subtipo.slot.usaNucleo) imagen.nucleo() else null }

    companion object {
        fun de(prenda: Garment, imagen: ImageBitmap, subtipo: Subtipo, ajuste: AjustePrenda = AjustePrenda.Ninguno) =
            PrendaAvatar(imagen, subtipo, ajuste, DatosPrenda(prenda.type, prenda.size, prenda.brand, prenda.fit))
    }
}

/** Cuerpo y tablas de tallas con los que se dibuja el avatar; lo provee [ProveedorAvatar]. */
class ContextoAvatar(
    val cuerpo: PerfilCorporal = PerfilCorporal(),
    val tablas: Map<String, TablaTallas> = emptyMap(),
) {
    val escala = EscalaAvatar(cuerpo)
}

val LocalAvatar = compositionLocalOf { ContextoAvatar() }

/**
 * Dibuja el avatar y, encima, las [prendas] a escala real (o en su caja si no hay datos) en el orden de capas.
 * Si hay varias en el mismo slot gana la última; un vestido oculta top y bottom.
 */
@Composable
fun AvatarConPrendas(prendas: List<PrendaAvatar>, modifier: Modifier = Modifier) {
    val ctx = LocalAvatar.current
    Canvas(modifier.aspectRatio(LIENZO_ANCHO / LIENZO_ALTO)) {
        dibujarAvatarConPrendas(ctx, prendas, size.height / LIENZO_ALTO)
    }
}

/** Rectángulo (unidades de lienzo) de [p] antes del ajuste manual. */
internal fun rectBase(ctx: ContextoAvatar, p: PrendaAvatar): Rect {
    val slot = p.subtipo.slot
    p.datos?.let { d ->
        val dim = DimensionesPrendaCalc.calcular(p.subtipo, d.tipo, d.talla, d.marca, d.corte, ctx.cuerpo, ctx.tablas)
        colocacionReal(p.subtipo, dim, ctx.escala, p.imagen.width, p.imagen.height, p.nucleo)?.let { return it }
    }
    val caja = if (p.datos != null) cajaAccesorio(p.subtipo, ctx.escala) else p.subtipo.caja
    return encajar(caja, p.imagen.width, p.imagen.height, slot.alineacion, p.nucleo)
}

/** Núcleo del recorte a partir de su canal alfa. */
private fun ImageBitmap.nucleo(): Nucleo? {
    if (width <= 0 || height <= 0) return null
    val pixeles = IntArray(width * height)
    readPixels(pixeles)
    val alfa = ByteArray(pixeles.size) { (pixeles[it] ushr 24).toByte() }
    return nucleoPrenda(alfa, width, height)
}

/** Dibuja el lienzo del avatar (400 x 1000 unidades) con origen en (0, 0) y [escala] píxeles por unidad. */
fun DrawScope.dibujarAvatarConPrendas(ctx: ContextoAvatar, prendas: List<PrendaAvatar>, escala: Float) {
    val porSlot: Map<Slot, PrendaAvatar> = prendas.associateBy { it.subtipo.slot }
    val capas = capasOrdenadas(porSlot.keys)
    scale(escala, escala, pivot = Offset.Zero) {
        capas.forEach { capa ->
            when (capa) {
                is Capa.Avatar -> dibujarCapaAvatar(capa.capa, ctx.escala)
                is Capa.Prenda -> {
                    val p = porSlot.getValue(capa.slot)
                    val r = ajustar(rectBase(ctx, p), p.ajuste)
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
