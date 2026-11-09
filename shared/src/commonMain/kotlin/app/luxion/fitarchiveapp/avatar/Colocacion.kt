package app.luxion.fitarchiveapp.avatar

import app.luxion.fitarchiveapp.model.OutfitGarment
import app.luxion.fitarchiveapp.model.GarmentType
import kotlin.math.min

/** Lienzo del avatar (docs/avatar.md): 400 x 1000 unidades. */
const val LIENZO_ANCHO = 400f
const val LIENZO_ALTO = 1000f

data class Caja(val x: Float, val y: Float, val w: Float, val h: Float)

enum class Alineacion { ARRIBA_CENTRO, ABAJO_CENTRO, CENTRO }

/** Capas del avatar (no prendas). */
enum class CapaAvatar(val z: Int) { HAIR_BACK(0), BODY(10), FACE(12), ARMS(14), HAIR_FRONT(70) }

/** Slots donde se coloca una prenda, con su z y su alineación en la caja. */
enum class Slot(val z: Int, val alineacion: Alineacion) {
    SHOES(20, Alineacion.ABAJO_CENTRO),
    BOTTOM(30, Alineacion.ARRIBA_CENTRO),
    TOP(40, Alineacion.ARRIBA_CENTRO),
    DRESS(45, Alineacion.ARRIBA_CENTRO),
    OUTER(50, Alineacion.ARRIBA_CENTRO),
    ACC_WAIST(60, Alineacion.CENTRO),
    ACC_NECK(62, Alineacion.ARRIBA_CENTRO),
    ACC_FACE(72, Alineacion.CENTRO),
    ACC_HEAD(80, Alineacion.ABAJO_CENTRO),
    ACC_HAND(90, Alineacion.ARRIBA_CENTRO),
}

/**
 * Subtipo de prenda: elige slot y caja base (valores de `anchors.json`).
 * [reflejar] voltea la imagen horizontalmente (zapato izquierdo a partir del derecho).
 */
enum class Subtipo(val slot: Slot, val caja: Caja, val reflejar: Boolean = false) {
    TOP_SIN_MANGAS_CORTO(Slot.TOP, Caja(120f, 172f, 160f, 260f)),
    TOP_MANGA_CORTA(Slot.TOP, Caja(106f, 172f, 188f, 270f)),
    TOP_MANGA_LARGA(Slot.TOP, Caja(88f, 172f, 224f, 340f)),
    TOP_LARGO_CADERA(Slot.TOP, Caja(88f, 172f, 224f, 410f)),

    OUTER_CHAQUETA(Slot.OUTER, Caja(84f, 170f, 232f, 330f)),
    OUTER_ABRIGO(Slot.OUTER, Caja(80f, 170f, 240f, 560f)),

    BOTTOM_SHORTS(Slot.BOTTOM, Caja(140f, 350f, 120f, 130f)),
    BOTTOM_FALDA_MINI(Slot.BOTTOM, Caja(136f, 350f, 128f, 150f)),
    BOTTOM_FALDA_MIDI(Slot.BOTTOM, Caja(134f, 350f, 132f, 310f)),
    BOTTOM_FALDA_LARGA(Slot.BOTTOM, Caja(132f, 350f, 136f, 550f)),
    BOTTOM_PANTALON_CORTO(Slot.BOTTOM, Caja(140f, 350f, 120f, 300f)),
    BOTTOM_PANTALON_LARGO(Slot.BOTTOM, Caja(138f, 350f, 124f, 550f)),

    DRESS_CORTO(Slot.DRESS, Caja(106f, 172f, 188f, 330f)),
    DRESS_MIDI(Slot.DRESS, Caja(104f, 172f, 192f, 500f)),
    DRESS_LARGO(Slot.DRESS, Caja(102f, 172f, 196f, 730f)),

    SHOES_PAR(Slot.SHOES, Caja(148f, 900f, 104f, 72f)),
    SHOES_BOTA_ALTA(Slot.SHOES, Caja(148f, 760f, 104f, 212f)),
    SHOES_PIE_DER(Slot.SHOES, Caja(206f, 900f, 44f, 72f)),
    SHOES_PIE_IZQ(Slot.SHOES, Caja(150f, 900f, 44f, 72f), reflejar = true),

    ACC_SOMBRERO_GORRO(Slot.ACC_HEAD, Caja(138f, 8f, 124f, 76f)),
    ACC_DIADEMA(Slot.ACC_HEAD, Caja(158f, 36f, 84f, 30f)),
    ACC_GAFAS(Slot.ACC_FACE, Caja(160f, 76f, 80f, 28f)),
    ACC_PENDIENTES(Slot.ACC_FACE, Caja(150f, 100f, 100f, 50f)),
    ACC_COLLAR(Slot.ACC_NECK, Caja(168f, 150f, 64f, 70f)),
    ACC_PANUELO(Slot.ACC_NECK, Caja(156f, 150f, 88f, 90f)),
    ACC_CINTURON(Slot.ACC_WAIST, Caja(146f, 335f, 108f, 36f)),
    ACC_BOLSO_MANO_DER(Slot.ACC_HAND, Caja(268f, 470f, 110f, 150f)),
    ACC_BOLSO_HOMBRO_IZQ(Slot.ACC_HAND, Caja(40f, 230f, 90f, 170f)),
}

/** Subtipo por defecto según el tipo del armario: el que se usa mientras la prenda no tenga uno guardado. */
fun subtipoPorDefecto(tipo: GarmentType): Subtipo = when (tipo) {
    GarmentType.TSHIRT -> Subtipo.TOP_MANGA_CORTA
    GarmentType.SHIRT, GarmentType.SWEATER -> Subtipo.TOP_MANGA_LARGA
    GarmentType.JACKET -> Subtipo.OUTER_CHAQUETA
    GarmentType.COAT -> Subtipo.OUTER_ABRIGO
    GarmentType.TROUSERS, GarmentType.JEANS -> Subtipo.BOTTOM_PANTALON_LARGO
    GarmentType.SKIRT -> Subtipo.BOTTOM_FALDA_MIDI
    GarmentType.SHORTS -> Subtipo.BOTTOM_SHORTS
    GarmentType.DRESS -> Subtipo.DRESS_MIDI
    GarmentType.JUMPSUIT -> Subtipo.DRESS_LARGO
    GarmentType.SHOES -> Subtipo.SHOES_PAR
    GarmentType.BAG -> Subtipo.ACC_BOLSO_MANO_DER
    GarmentType.ACCESSORY -> Subtipo.ACC_COLLAR
}

fun slotDe(tipo: GarmentType): Slot = subtipoPorDefecto(tipo).slot

/** Rectángulo en unidades del lienzo. */
data class Rect(val x: Float, val y: Float, val w: Float, val h: Float)

/** Regla *contain*: la imagen cabe en [caja] manteniendo proporción y se alinea según [alineacion]. */
fun encajar(caja: Caja, anchoImg: Int, altoImg: Int, alineacion: Alineacion): Rect {
    if (anchoImg <= 0 || altoImg <= 0) return Rect(caja.x, caja.y, 0f, 0f)
    val s = min(caja.w / anchoImg, caja.h / altoImg)
    val w = anchoImg * s
    val h = altoImg * s
    val x = caja.x + (caja.w - w) / 2f
    val y = when (alineacion) {
        Alineacion.ARRIBA_CENTRO -> caja.y
        Alineacion.ABAJO_CENTRO -> caja.y + caja.h - h
        Alineacion.CENTRO -> caja.y + (caja.h - h) / 2f
    }
    return Rect(x, y, w, h)
}

/** Capa a dibujar, ya en orden de apilado. */
sealed interface Capa {
    val z: Int

    data class Avatar(val capa: CapaAvatar) : Capa {
        override val z get() = capa.z
    }

    data class Prenda(val slot: Slot) : Capa {
        override val z get() = slot.z
    }
}

/**
 * Capas a dibujar de abajo arriba: las del avatar más los [slotsOcupados].
 * Un vestido excluye `top` y `bottom`.
 */
fun capasOrdenadas(slotsOcupados: Set<Slot>): List<Capa> {
    val prendas = if (Slot.DRESS in slotsOcupados) slotsOcupados - Slot.TOP - Slot.BOTTOM else slotsOcupados
    return (CapaAvatar.entries.map { Capa.Avatar(it) } + prendas.map { Capa.Prenda(it) }).sortedBy { it.z }
}

/**
 * Ajuste manual de una prenda sobre su colocación base: desplazamiento ([dx], [dy]) en unidades del
 * lienzo y [escala] multiplicativa respecto al tamaño *contain* (se escala desde el centro).
 * Se persiste tal cual en `OutfitGarment.x/y/scale`.
 */
data class AjustePrenda(val dx: Float = 0f, val dy: Float = 0f, val escala: Float = 1f) {
    /** Lo deja dentro de los límites de [caja]: el centro de la prenda no sale de la caja y la escala va de 0,5x a 1,5x. */
    fun limitado(caja: Caja) = AjustePrenda(
        dx = dx.coerceIn(-caja.w / 2f, caja.w / 2f),
        dy = dy.coerceIn(-caja.h / 2f, caja.h / 2f),
        escala = escala.coerceIn(ESCALA_MIN, ESCALA_MAX),
    )

    companion object {
        const val ESCALA_MIN = 0.5f
        const val ESCALA_MAX = 1.5f

        /** Multiplicador fijo: el tamaño real sale de las dimensiones de la prenda y las medidas del cuerpo. */
        const val ESCALA_AUTOMATICA = 1f
        val Ninguno = AjustePrenda()
    }
}

/** Aplica [ajuste] a la colocación base [base]: escala desde su centro y luego desplaza. */
fun ajustar(base: Rect, ajuste: AjustePrenda): Rect {
    val w = base.w * ajuste.escala
    val h = base.h * ajuste.escala
    val cx = base.x + base.w / 2f + ajuste.dx
    val cy = base.y + base.h / 2f + ajuste.dy
    return Rect(cx - w / 2f, cy - h / 2f, w, h)
}

/** Como [ajustar], pero limitando antes el [ajuste] a la [caja] del subtipo (la prenda no sale de su zona). */
fun ajustarEnCaja(base: Rect, ajuste: AjustePrenda, caja: Caja): Rect = ajustar(base, ajuste.limitado(caja))

/**
 * Ajuste de una prenda guardada. La escala guardada se ignora (las primeras versiones dejaban elegirla a
 * mano): el tamaño es siempre el automático.
 */
fun OutfitGarment.ajusteGuardado() = AjustePrenda(x, y, AjustePrenda.ESCALA_AUTOMATICA)
