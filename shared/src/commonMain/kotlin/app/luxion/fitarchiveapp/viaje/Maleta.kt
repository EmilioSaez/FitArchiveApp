package app.luxion.fitarchiveapp.viaje

import app.luxion.fitarchiveapp.data.OutfitWithGarments
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.TripGarment
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** Datos de un viaje ya validados y normalizados, listos para guardar. */
data class DatosViaje(
    val nombre: String,
    val destino: String?,
    val inicio: String?,
    val fin: String?,
)

sealed interface ResultadoViaje {
    data class Valido(val datos: DatosViaje) : ResultadoViaje
    data class Invalido(val mensaje: String) : ResultadoViaje
}

/**
 * Valida el formulario de un viaje. Sin nombre se usa el destino; las fechas (AAAA-MM-DD) son opcionales,
 * pero la de fin exige la de inicio y no puede ser anterior a ella.
 */
fun validarViaje(nombre: String, destino: String, inicio: String, fin: String): ResultadoViaje {
    val destinoLimpio = destino.trim().ifEmpty { null }
    val nombreFinal = nombre.trim().ifEmpty { destinoLimpio }
        ?: return ResultadoViaje.Invalido("Ponle un nombre o un destino al viaje")
    val desde = inicio.trim().ifEmpty { null }
    val hasta = fin.trim().ifEmpty { null }
    val fechaInicio = desde?.let { parsearFecha(it) ?: return ResultadoViaje.Invalido("Fecha de inicio no válida (usa AAAA-MM-DD)") }
    val fechaFin = hasta?.let { parsearFecha(it) ?: return ResultadoViaje.Invalido("Fecha de fin no válida (usa AAAA-MM-DD)") }
    if (fechaFin != null && fechaInicio == null) return ResultadoViaje.Invalido("Indica también la fecha de inicio")
    if (fechaInicio != null && fechaFin != null && fechaFin < fechaInicio) {
        return ResultadoViaje.Invalido("La vuelta no puede ser antes de la ida")
    }
    return ResultadoViaje.Valido(DatosViaje(nombreFinal, destinoLimpio, fechaInicio?.toString(), fechaFin?.toString()))
}

private fun parsearFecha(texto: String): LocalDate? = try {
    LocalDate.parse(texto)
} catch (e: IllegalArgumentException) {
    null
}

/** Número de días del viaje contando ida y vuelta; null si falta alguna fecha o no son válidas. */
fun diasDeViaje(inicio: String?, fin: String?): Int? {
    val desde = inicio?.let { parsearFecha(it) } ?: return null
    val hasta = fin?.let { parsearFecha(it) } ?: return null
    if (hasta < desde) return null
    return desde.daysUntil(hasta) + 1
}

private val MESES_CORTOS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** "2026-12-10" → "10 dic 2026" (o "10 dic" si [conAnio] es false); el texto original si no es una fecha válida. */
fun formatearFechaCorta(iso: String, conAnio: Boolean = true): String = parsearFecha(iso)?.let {
    "${it.day} ${MESES_CORTOS[it.month.ordinal]}" + if (conAnio) " ${it.year}" else ""
} ?: iso

/**
 * Fechas de un viaje para mostrar: "10 dic → 14 dic 2026 · 5 días". El año va solo al final si ida y vuelta
 * coinciden en él; si no, en cada fecha. Null si no hay fecha de ida.
 */
fun formatearFechasViaje(inicio: String?, fin: String?): String? {
    if (inicio == null) return null
    if (fin == null) return formatearFechaCorta(inicio)
    val mismoAnio = parsearFecha(inicio)?.year.let { it != null && it == parsearFecha(fin)?.year }
    val dias = diasDeViaje(inicio, fin)?.let { " · $it ${if (it == 1) "día" else "días"}" }.orEmpty()
    return "${formatearFechaCorta(inicio, conAnio = !mismoAnio)} → ${formatearFechaCorta(fin)}$dias"
}

/** Prenda de la lista de la maleta. */
data class ItemMaleta(
    val prenda: Garment,
    val guardada: Boolean,
    /** Nombres de los outfits del viaje que la incluyen; vacío si se añadió a mano. */
    val outfits: List<String>,
) {
    /** Añadida a mano: solo estas se pueden quitar de la maleta (las de un outfit salen quitando el outfit). */
    val manual get() = outfits.isEmpty()
}

data class Maleta(val items: List<ItemMaleta>) {
    val total get() = items.size
    val guardadas get() = items.count { it.guardada }
    val completa get() = items.isNotEmpty() && guardadas == total

    /** Prendas que van sucias: conviene lavarlas antes de salir. */
    val sucias get() = items.filter { it.prenda.dirty }
}

/**
 * Lista de la maleta: prendas de los [outfits] del viaje más las de [lista], sin repetir.
 * El estado "guardada" sale de [lista]. Se ignoran las prendas que ya no existen en [armario].
 * Orden: por tipo de prenda y, dentro de cada tipo, la más antigua primero.
 */
fun construirMaleta(outfits: List<OutfitWithGarments>, lista: List<TripGarment>, armario: List<Garment>): Maleta {
    val porId = armario.associateBy { it.id }
    val enLista = lista.associateBy { it.garmentId }
    val deOutfits = linkedMapOf<Long, MutableList<String>>()
    outfits.forEach { o ->
        o.garments.forEach { og -> deOutfits.getOrPut(og.garmentId) { mutableListOf() }.add(o.outfit.name) }
    }
    val ids = (deOutfits.keys + enLista.keys).distinct()
    val items = ids.mapNotNull { id ->
        porId[id]?.let { ItemMaleta(it, enLista[id]?.packed == true, deOutfits[id].orEmpty().distinct()) }
    }.sortedWith(compareBy({ it.prenda.type.ordinal }, { it.prenda.id }))
    return Maleta(items)
}
