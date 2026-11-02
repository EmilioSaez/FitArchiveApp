package app.luxion.fitarchiveapp.suggestions

import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import app.luxion.fitarchiveapp.model.WearRecord

/** Outfit guardado como señal de gusto (T42): ids de sus prendas y si es favorito. */
data class OutfitGuardado(val prendaIds: List<Long>, val favorito: Boolean) {
    companion object {
        fun de(outfit: Outfit, piezas: List<OutfitGarment>) = OutfitGuardado(piezas.map { it.garmentId }, outfit.favorite)
    }
}

/** Pesos de cada rasgo en la afinidad; se normalizan entre los rasgos disponibles. */
data class PesosAfinidad(
    val tipo: Double = 0.20,
    val color: Double = 0.35,
    val uso: Double = 0.25,
    val marca: Double = 0.20,
)

/**
 * Afinidad aprendida (T42). Cada prenda aporta evidencia de gusto: like (prenda favorita), aparecer en
 * outfits guardados (más si el outfit es favorito) y uso registrado. Para cada rasgo (tipo, color, uso, marca)
 * se compara cuánto pesa en la evidencia frente a cuánto pesa en el armario ("lift") y se suaviza hacia lo
 * neutro con [SUAVIZADO], de modo que con pocos datos todo queda cerca de 0.5. Sin evidencia, siempre 0.5.
 *
 * Uso: `AfinidadAprendida.desde(armario, outfits, usos)` y pasarla en [PedidoSugerencia.afinidad].
 */
class AfinidadAprendida private constructor(
    private val perfiles: Map<Rasgo, Map<String, Double>>,
    private val pesos: PesosAfinidad,
) : AfinidadEstilo {

    private enum class Rasgo { TIPO, COLOR, USO, MARCA }

    override fun puntuar(prendas: List<Garment>): Double {
        if (prendas.isEmpty()) return NEUTRO
        return prendas.map { puntuarPrenda(it) }.average().coerceIn(0.0, 1.0)
    }

    private fun puntuarPrenda(g: Garment): Double {
        var suma = 0.0
        var total = 0.0
        for (r in Rasgo.entries) {
            val valor = valor(g, r) ?: continue
            val peso = peso(r)
            suma += (perfiles[r]?.get(valor) ?: NEUTRO) * peso
            total += peso
        }
        return if (total <= 0.0) NEUTRO else suma / total
    }

    private fun peso(r: Rasgo) = when (r) {
        Rasgo.TIPO -> pesos.tipo
        Rasgo.COLOR -> pesos.color
        Rasgo.USO -> pesos.uso
        Rasgo.MARCA -> pesos.marca
    }

    companion object {
        const val NEUTRO = 0.5

        /** Evidencia de una prenda con like, de una prenda en un outfit guardado / favorito, y por uso (con tope). */
        const val EVIDENCIA_LIKE = 2.0
        const val EVIDENCIA_OUTFIT = 1.0
        const val EVIDENCIA_OUTFIT_FAVORITO = 2.0
        const val EVIDENCIA_POR_USO = 0.25
        const val USOS_MAXIMOS = 8

        /** "Observaciones virtuales" neutras: cuanto mayor, más datos hacen falta para salirse de 0.5. */
        const val SUAVIZADO = 3.0

        private fun valor(g: Garment, r: Rasgo): String? = when (r) {
            Rasgo.TIPO -> g.type.name
            Rasgo.COLOR -> g.color.name
            Rasgo.USO -> g.usage.name
            Rasgo.MARCA -> g.brand?.trim()?.lowercase()?.ifEmpty { null }
        }

        /** Sin datos: puntúa siempre 0.5. */
        val neutra: AfinidadEstilo = AfinidadEstilo { NEUTRO }

        /**
         * Aprende del [armario] (prendas con like), de [outfits] guardados y del historial de [usos].
         * Los ids que no estén en el armario se ignoran.
         */
        fun desde(
            armario: List<Garment>,
            outfits: List<OutfitGuardado> = emptyList(),
            usos: List<WearRecord> = emptyList(),
            pesos: PesosAfinidad = PesosAfinidad(),
            suavizado: Double = SUAVIZADO,
        ): AfinidadEstilo {
            val ids = armario.map { it.id }.toSet()
            val evidencia = mutableMapOf<Long, Double>()
            fun sumar(id: Long, v: Double) { if (id in ids) evidencia[id] = (evidencia[id] ?: 0.0) + v }

            armario.filter { it.favorite }.forEach { sumar(it.id, EVIDENCIA_LIKE) }
            outfits.forEach { o ->
                val v = if (o.favorito) EVIDENCIA_OUTFIT_FAVORITO else EVIDENCIA_OUTFIT
                o.prendaIds.distinct().forEach { sumar(it, v) }
            }
            usos.groupingBy { it.garmentId }.eachCount().forEach { (id, n) ->
                sumar(id, minOf(n, USOS_MAXIMOS) * EVIDENCIA_POR_USO)
            }
            if (evidencia.values.sum() <= 0.0) return neutra

            val perfiles = Rasgo.entries.associateWith { r -> perfil(r, armario, evidencia, suavizado) }
            return AfinidadAprendida(perfiles, pesos)
        }

        private fun perfil(r: Rasgo, armario: List<Garment>, evidencia: Map<Long, Double>, k: Double): Map<String, Double> {
            val conValor = armario.mapNotNull { g -> valor(g, r)?.let { g to it } }
            val totalArmario = conValor.size.toDouble()
            val totalEv = conValor.sumOf { (g, _) -> evidencia[g.id] ?: 0.0 }
            if (totalArmario == 0.0 || totalEv <= 0.0) return emptyMap()
            val enArmario = conValor.groupingBy { it.second }.eachCount()
            val enEvidencia = conValor.groupBy({ it.second }, { evidencia[it.first.id] ?: 0.0 }).mapValues { it.value.sum() }
            return enArmario.mapValues { (v, n) ->
                val cuotaArmario = n / totalArmario
                // Cuota de evidencia suavizada hacia la del armario: sin datos, lift = 1 -> neutro.
                val cuotaEv = ((enEvidencia[v] ?: 0.0) + k * cuotaArmario) / (totalEv + k)
                val lift = cuotaEv / cuotaArmario
                lift / (1.0 + lift)
            }
        }
    }
}
