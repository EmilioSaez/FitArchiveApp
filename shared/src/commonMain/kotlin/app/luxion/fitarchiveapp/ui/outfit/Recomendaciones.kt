package app.luxion.fitarchiveapp.ui.outfit

import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.suggestions.ColorRules

/**
 * Prendas de [candidatas] que mejor combinan con lo que ya lleva el maniquí ([puestas]): reutiliza las reglas de
 * color del motor de sugerencias y suma un extra por misma estación y uso parecido. No repite lo que ya está puesto.
 * Sin nada puesto recomienda por temporada actual, favoritas y más usadas.
 */
fun recomendadas(candidatas: List<Garment>, puestas: List<Garment>, cuantas: Int = 2, estacion: Season? = null): List<Garment> {
    val idsPuestas = puestas.map { it.id }.toSet()
    val libres = candidatas.filter { it.id !in idsPuestas }
    if (puestas.isEmpty()) {
        // Sin nada puesto no hay con qué combinar: temporada actual, favoritas y más usadas.
        return libres.sortedByDescending { (if (it.season == estacion) 4 else 0) + (if (it.favorite) 2 else 0) + it.totalWears * 0.01 }
            .take(cuantas)
    }
    return libres
        .map { it to puntuar(it, puestas) }
        .filter { (_, puntos) -> puntos >= UMBRAL }
        .sortedByDescending { it.second }
        .take(cuantas)
        .map { it.first }
}

/** Temporada de un mes (1-12) en el hemisferio norte. */
fun estacionDelMes(mes: Int): Season = when (mes) {
    12, 1, 2 -> Season.WINTER
    6, 7, 8 -> Season.SUMMER
    else -> Season.MID_SEASON
}

private const val UMBRAL = 0.5

private fun puntuar(prenda: Garment, puestas: List<Garment>): Double {
    val color = puestas.map { ColorRules.puntuacionPareja(prenda.color, it.color) }.average()
    val estacion = puestas.count { it.season == prenda.season }.toDouble() / puestas.size
    val uso = puestas.count { it.usage == prenda.usage }.toDouble() / puestas.size
    return color * 0.6 + estacion * 0.25 + uso * 0.15
}
