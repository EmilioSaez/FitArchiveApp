package app.luxion.fitarchiveapp.ui.anadir

import app.luxion.fitarchiveapp.foto.FotoCapturada
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

/** Tallas disponibles en el formulario. */
val TALLAS = listOf("XS", "S", "M", "L", "XL")

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
) {
    val completo: Boolean get() = type != null && color != null && season != null && usage != null

    companion object {
        /** Borrador con los datos de una prenda existente, para editarla. */
        fun desde(g: Garment) = BorradorPrenda(
            type = g.type, color = g.color, season = g.season, usage = g.usage,
            brand = g.brand.orEmpty(), size = g.size,
        )
    }

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
        )
    }

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
            favorite = false,
            dirty = false,
            wearsSinceWash = 0,
            totalWears = 0,
            washAfterWears = null,
            createdAt = createdAt,
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
