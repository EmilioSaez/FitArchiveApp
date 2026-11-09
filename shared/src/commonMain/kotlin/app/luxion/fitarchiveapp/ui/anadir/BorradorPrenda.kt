package app.luxion.fitarchiveapp.ui.anadir

import app.luxion.fitarchiveapp.avatar.Subtipo
import app.luxion.fitarchiveapp.avatar.subtipoEfectivo
import app.luxion.fitarchiveapp.avatar.subtipoPorDefecto
import app.luxion.fitarchiveapp.avatar.subtipoValidoPara
import app.luxion.fitarchiveapp.avatar.sugerirSubtipo
import app.luxion.fitarchiveapp.foto.FotoCapturada
import app.luxion.fitarchiveapp.model.Fit
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage

/**
 * Enganche de T07: recibe la foto y devuelve la foto con el fondo quitado.
 * Mientras T07 no esté integrada, la foto se guarda tal cual.
 */
typealias QuitarFondo = suspend (FotoCapturada) -> FotoCapturada

/**
 * Enganche de T09: recibe la foto (ya sin fondo) y devuelve el color principal
 * detectado, o null si no se sabe. Se usa solo para preseleccionar el color.
 */
typealias DetectarColor = suspend (FotoCapturada) -> GarmentColor?

val SinQuitarFondo: QuitarFondo = { it }
val SinDetectarColor: DetectarColor = { null }

/** Tallas en letra y en numeración EU de ropa (las dos aparecen en las tablas de marca). */
private val TALLAS_LETRA = listOf("XXS", "XS", "S", "M", "L", "XL", "XXL")
private val TALLAS_ROPA_NUMERICAS = (32..48 step 2).map { it.toString() }
private val TALLAS_CALZADO = (35..46).map { it.toString() }

/**
 * Tallas del formulario según el tipo de prenda: calzado, numeración de pie; ropa, letras y
 * numeración EU (Zara, H&M o Brownie dan pantalones como 38); bolsos y accesorios no tienen talla corporal.
 */
fun tallasPara(tipo: GarmentType?): List<String> = when (tipo) {
    GarmentType.SHOES -> TALLAS_CALZADO
    GarmentType.BAG, GarmentType.ACCESSORY -> TALLAS_LETRA
    else -> TALLAS_LETRA + TALLAS_ROPA_NUMERICAS
}

/** Marcas con tabla de tallas (docs/requisitos.md); se sugieren al escribir, pero la marca es texto libre. */
val MARCAS_CON_TABLA = listOf(
    "Zara", "Pull&Bear", "Bershka", "Stradivarius", "Massimo Dutti",
    "Mango", "H&M", "Brownie", "Scalpers", "Hollister", "Brandy Melville",
)

/** Marcas que empiezan por [texto] (sin distinguir mayúsculas); vacío = ninguna. Excluye la coincidencia exacta. */
fun sugerenciasDeMarca(texto: String): List<String> {
    val t = texto.trim()
    if (t.isEmpty()) return emptyList()
    return MARCAS_CON_TABLA.filter { it.startsWith(t, ignoreCase = true) && !it.equals(t, ignoreCase = true) }
}

/** Estado editable del formulario. Tipo, color, estación y uso son obligatorios; marca y talla, no. */
data class BorradorPrenda(
    val type: GarmentType? = null,
    val color: GarmentColor? = null,
    val season: Season? = null,
    val usage: Usage? = null,
    val brand: String = "",
    val size: String? = null,
    val fit: Fit = Fit.NORMAL,
    /** Subtipo elegido a mano; null = el sugerido para el tipo. */
    val subtipo: Subtipo? = null,
) {
    val completo: Boolean get() = type != null && color != null && season != null && usage != null

    companion object {
        /** Borrador con los datos de una prenda existente, para editarla. */
        fun desde(g: Garment) = BorradorPrenda(
            type = g.type, color = g.color, season = g.season, usage = g.usage,
            brand = g.brand.orEmpty(), size = g.size, fit = g.fit, subtipo = g.subtipo,
        )
    }

    /** Subtipo con el que se guardará: el elegido (si vale para el tipo) o, si no, el sugerido; null sin tipo. */
    val subtipoEfectivo: Subtipo? get() = type?.let { subtipoValidoPara(it, subtipo) ?: sugerirSubtipo(it) }

    /** Cambia el tipo y descarta el subtipo elegido si no vale para el nuevo. */
    fun conTipo(nuevo: GarmentType) = copy(
        type = nuevo,
        size = size?.takeIf { it in tallasPara(nuevo) },
        subtipo = subtipoValidoPara(nuevo, subtipo),
    )

    /**
     * Prenda editada: copia de [original] con los campos del formulario; id, foto, favorito y
     * contadores no cambian. Null si falta algún campo obligatorio.
     */
    fun aplicarA(original: Garment): Garment? {
        if (!completo) return null
        return original.copy(
            type = type!!,
            color = color!!,
            season = season!!,
            usage = usage!!,
            brand = brand.trim().ifEmpty { null },
            size = size,
            fit = fit,
            subtipo = subtipoGuardado(),
            // El ajuste manual es relativo a la caja del subtipo: si cambia de subtipo deja de valer.
            ajuste = original.ajuste.takeIf { subtipoEfectivo == original.subtipoEfectivo },
        )
    }

    /** Lo que se guarda en la prenda: null si es el por defecto del tipo (así sigue a `subtipoPorDefecto`). */
    private fun subtipoGuardado(): Subtipo? = subtipoValidoPara(type, subtipo)?.takeIf { it != subtipoPorDefecto(type!!) }

    /** Crea la prenda nueva; null si falta algún campo obligatorio. El id lo asigna el repositorio. */
    fun aPrenda(photoPath: String, createdAt: Long): Garment? {
        if (!completo) return null
        return Garment(
            id = 0,
            photoPath = photoPath,
            type = type!!,
            color = color!!,
            season = season!!,
            usage = usage!!,
            brand = brand.trim().ifEmpty { null },
            size = size,
            fit = fit,
            favorite = false,
            dirty = false,
            wearsSinceWash = 0,
            totalWears = 0,
            washAfterWears = null,
            createdAt = createdAt,
            subtipo = subtipoGuardado(),
        )
    }
}

fun GarmentType.etiqueta() = when (this) {
    GarmentType.TSHIRT -> "Camiseta"
    GarmentType.SHIRT -> "Camisa"
    GarmentType.SWEATER -> "Jersey"
    GarmentType.JACKET -> "Chaqueta"
    GarmentType.COAT -> "Abrigo"
    GarmentType.TROUSERS -> "Pantalón"
    GarmentType.JEANS -> "Vaqueros"
    GarmentType.SKIRT -> "Falda"
    GarmentType.SHORTS -> "Shorts"
    GarmentType.DRESS -> "Vestido"
    GarmentType.JUMPSUIT -> "Mono"
    GarmentType.SHOES -> "Calzado"
    GarmentType.BAG -> "Bolso"
    GarmentType.ACCESSORY -> "Accesorio"
}

fun Subtipo.etiqueta() = when (this) {
    Subtipo.TOP_SIN_MANGAS_CORTO -> "Sin mangas"
    Subtipo.TOP_MANGA_CORTA -> "Manga corta"
    Subtipo.TOP_MANGA_LARGA -> "Manga larga"
    Subtipo.TOP_LARGO_CADERA -> "Largo hasta la cadera"
    Subtipo.OUTER_CHAQUETA -> "Chaqueta corta"
    Subtipo.OUTER_ABRIGO -> "Abrigo largo"
    Subtipo.BOTTOM_SHORTS -> "Shorts"
    Subtipo.BOTTOM_FALDA_MINI -> "Falda mini"
    Subtipo.BOTTOM_FALDA_MIDI -> "Falda midi"
    Subtipo.BOTTOM_FALDA_LARGA -> "Falda larga"
    Subtipo.BOTTOM_PANTALON_CORTO -> "Pantalón corto o pirata"
    Subtipo.BOTTOM_PANTALON_LARGO -> "Pantalón largo"
    Subtipo.DRESS_CORTO -> "Corto"
    Subtipo.DRESS_MIDI -> "Midi"
    Subtipo.DRESS_LARGO -> "Largo"
    Subtipo.SHOES_PAR -> "Par de zapatos"
    Subtipo.SHOES_BOTA_ALTA -> "Bota alta"
    Subtipo.SHOES_PIE_DER -> "Solo pie derecho"
    Subtipo.SHOES_PIE_IZQ -> "Solo pie izquierdo"
    Subtipo.ACC_SOMBRERO_GORRO -> "Sombrero o gorro"
    Subtipo.ACC_DIADEMA -> "Diadema"
    Subtipo.ACC_GAFAS -> "Gafas"
    Subtipo.ACC_PENDIENTES -> "Pendientes"
    Subtipo.ACC_COLLAR -> "Collar"
    Subtipo.ACC_PANUELO -> "Pañuelo"
    Subtipo.ACC_CINTURON -> "Cinturón"
    Subtipo.ACC_BOLSO_MANO_DER -> "Bolso de mano"
    Subtipo.ACC_BOLSO_HOMBRO_IZQ -> "Bolso de hombro"
}

fun Fit.etiqueta() = when (this) {
    Fit.SLIM -> "Slim"
    Fit.NORMAL -> "Normal"
    Fit.OVERSIZE -> "Oversize"
}

fun Season.etiqueta() = when (this) {
    Season.WINTER -> "Invierno"
    Season.SUMMER -> "Verano"
    Season.MID_SEASON -> "Entretiempo"
}

fun Usage.etiqueta() = when (this) {
    Usage.WORK -> "Trabajo"
    Usage.GOING_OUT -> "Salir"
    Usage.CASUAL -> "Casual"
    Usage.SPORT -> "Deporte"
    Usage.PARTY -> "Fiesta"
}

fun GarmentColor.etiqueta() = when (this) {
    GarmentColor.BLACK -> "Negro"
    GarmentColor.WHITE -> "Blanco"
    GarmentColor.GREY -> "Gris"
    GarmentColor.BEIGE -> "Beige"
    GarmentColor.BROWN -> "Marrón"
    GarmentColor.RED -> "Rojo"
    GarmentColor.BURGUNDY -> "Burdeos"
    GarmentColor.PINK -> "Rosa"
    GarmentColor.ORANGE -> "Naranja"
    GarmentColor.YELLOW -> "Amarillo"
    GarmentColor.GREEN -> "Verde"
    GarmentColor.BLUE -> "Azul"
    GarmentColor.NAVY -> "Marino"
    GarmentColor.PURPLE -> "Morado"
    GarmentColor.MULTI -> "Multicolor"
}
