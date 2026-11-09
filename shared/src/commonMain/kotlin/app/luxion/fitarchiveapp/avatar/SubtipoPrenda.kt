package app.luxion.fitarchiveapp.avatar

import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentType

/** Subtipo de la prenda: el guardado o, si no tiene, el por defecto de su tipo. */
val Garment.subtipoEfectivo: Subtipo get() = subtipo ?: subtipoPorDefecto(type)

/** Ajuste manual guardado de la prenda ([AjustePrenda.Ninguno] si no tiene). */
val Garment.ajusteManual: AjustePrenda get() = ajuste ?: AjustePrenda.Ninguno

/**
 * Subtipos entre los que se puede elegir para un [tipo] (los que tienen sentido: manga y longitud en
 * tops, longitud en faldas y vestidos…). El primero es el por defecto del tipo.
 */
fun subtiposValidos(tipo: GarmentType): List<Subtipo> = when (tipo) {
    GarmentType.TSHIRT, GarmentType.SHIRT, GarmentType.SWEATER -> listOf(
        Subtipo.TOP_MANGA_CORTA, Subtipo.TOP_MANGA_LARGA, Subtipo.TOP_SIN_MANGAS_CORTO, Subtipo.TOP_LARGO_CADERA,
    )
    GarmentType.JACKET, GarmentType.COAT -> listOf(Subtipo.OUTER_CHAQUETA, Subtipo.OUTER_ABRIGO)
    GarmentType.TROUSERS, GarmentType.JEANS -> listOf(Subtipo.BOTTOM_PANTALON_LARGO, Subtipo.BOTTOM_PANTALON_CORTO)
    GarmentType.SKIRT -> listOf(Subtipo.BOTTOM_FALDA_MIDI, Subtipo.BOTTOM_FALDA_MINI, Subtipo.BOTTOM_FALDA_LARGA)
    GarmentType.SHORTS -> listOf(Subtipo.BOTTOM_SHORTS, Subtipo.BOTTOM_PANTALON_CORTO)
    GarmentType.DRESS, GarmentType.JUMPSUIT -> listOf(Subtipo.DRESS_MIDI, Subtipo.DRESS_CORTO, Subtipo.DRESS_LARGO)
    GarmentType.SHOES -> listOf(Subtipo.SHOES_PAR, Subtipo.SHOES_BOTA_ALTA, Subtipo.SHOES_PIE_DER, Subtipo.SHOES_PIE_IZQ)
    GarmentType.BAG -> listOf(Subtipo.ACC_BOLSO_MANO_DER, Subtipo.ACC_BOLSO_HOMBRO_IZQ)
    GarmentType.ACCESSORY -> listOf(
        Subtipo.ACC_COLLAR, Subtipo.ACC_PANUELO, Subtipo.ACC_CINTURON, Subtipo.ACC_SOMBRERO_GORRO,
        Subtipo.ACC_DIADEMA, Subtipo.ACC_GAFAS, Subtipo.ACC_PENDIENTES,
    )
}.let { validos -> listOf(subtipoPorDefecto(tipo)) + (validos - subtipoPorDefecto(tipo)) }

/**
 * Subtipo que se sugiere al añadir o editar una prenda.
 *
 * HUECO: de momento es el por defecto del tipo. Cuando llegue la proporción del núcleo de la prenda
 * (fix/colocacion-nucleo-prenda), se usará [proporcionNucleo] para elegir manga corta/larga, longitud…
 */
@Suppress("UNUSED_PARAMETER")
fun sugerirSubtipo(tipo: GarmentType, proporcionNucleo: Float? = null): Subtipo = subtipoPorDefecto(tipo)

/** [subtipo] si es válido para [tipo]; si no, null (vuelve al por defecto, p. ej. al cambiar de tipo). */
fun subtipoValidoPara(tipo: GarmentType?, subtipo: Subtipo?): Subtipo? =
    subtipo?.takeIf { tipo != null && it in subtiposValidos(tipo) }

/** Suma dos ajustes: desplazamientos sumados y escalas multiplicadas (el de la prenda y el del outfit). */
operator fun AjustePrenda.plus(otro: AjustePrenda) = AjustePrenda(dx + otro.dx, dy + otro.dy, escala * otro.escala)
