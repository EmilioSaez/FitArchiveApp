package com.emilio.fitarchive.suggestions

import com.emilio.fitarchive.model.GarmentColor
import com.emilio.fitarchive.model.GarmentColor.*
import kotlin.math.abs
import kotlin.math.min

/** Resultado de evaluar un conjunto de colores. [score] va de 0.0 a 1.0. */
data class ColorMatch(
    val combina: Boolean,
    val score: Double,
    val choques: List<Pair<GarmentColor, GarmentColor>> = emptyList(),
)

/** Reglas de combinación de colores para las sugerencias de outfits. */
object ColorRules {
    const val UMBRAL_COMBINA = 0.6

    /** Colores neutros: combinan con todo. */
    val neutros: Set<GarmentColor> = setOf(BLACK, WHITE, GREY, BEIGE, BROWN, NAVY)

    /** Parejas que se consideran un choque aunque sus tonos sean cercanos. */
    private val choquesConocidos: Set<Set<GarmentColor>> = setOf(
        setOf(RED, PINK),
        setOf(RED, ORANGE),
        setOf(PINK, ORANGE),
        setOf(RED, GREEN),
        setOf(ORANGE, PURPLE),
        setOf(PINK, YELLOW),
    )

    /** Tono en la rueda cromática (grados) de los colores no neutros. */
    private val tono: Map<GarmentColor, Int> = mapOf(
        RED to 0, ORANGE to 30, YELLOW to 60, GREEN to 120,
        BLUE to 220, PURPLE to 280, PINK to 330, BURGUNDY to 350,
    )

    private const val PENALIZACION_POR_COLOR_EXTRA = 0.15
    private const val MAX_CROMATICOS_SIN_PENALIZAR = 2

    fun esNeutro(color: GarmentColor): Boolean = color in neutros

    /** Puntuación (0.0–1.0) de una pareja de colores. */
    fun puntuacionPareja(a: GarmentColor, b: GarmentColor): Double {
        if (a == b) return 1.0
        if (esNeutro(a) || esNeutro(b)) return 1.0
        if (a == MULTI || b == MULTI) return 0.5
        if (setOf(a, b) in choquesConocidos) return 0.2
        val ta = tono.getValue(a)
        val tb = tono.getValue(b)
        val d = abs(ta - tb).let { min(it, 360 - it) }
        return when {
            d <= 60 -> 0.9 // análogos
            d >= 150 -> 0.85 // complementarios
            d in 100..149 -> 0.6 // triádicos / casi complementarios
            else -> 0.35 // tonos intermedios sin relación clara
        }
    }

    /** ¿Es la pareja un choque? */
    fun esChoque(a: GarmentColor, b: GarmentColor): Boolean = puntuacionPareja(a, b) < UMBRAL_COMBINA

    /** Evalúa los colores de las prendas de un outfit. */
    fun evaluar(colores: Collection<GarmentColor>): ColorMatch {
        val distintos = colores.distinct()
        if (distintos.size <= 1) return ColorMatch(combina = true, score = 1.0)

        val choques = mutableListOf<Pair<GarmentColor, GarmentColor>>()
        var suma = 0.0
        var parejas = 0
        for (i in distintos.indices) {
            for (j in i + 1 until distintos.size) {
                val p = puntuacionPareja(distintos[i], distintos[j])
                suma += p
                parejas++
                if (p < UMBRAL_COMBINA) choques += distintos[i] to distintos[j]
            }
        }
        val cromaticos = distintos.count { !esNeutro(it) }
        val extra = (cromaticos - MAX_CROMATICOS_SIN_PENALIZAR).coerceAtLeast(0)
        val score = (suma / parejas - extra * PENALIZACION_POR_COLOR_EXTRA).coerceIn(0.0, 1.0)
        return ColorMatch(combina = choques.isEmpty() && score >= UMBRAL_COMBINA, score = score, choques = choques)
    }

    fun combinan(colores: Collection<GarmentColor>): Boolean = evaluar(colores).combina
}
