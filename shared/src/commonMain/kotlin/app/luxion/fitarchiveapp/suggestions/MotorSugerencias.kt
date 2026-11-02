package app.luxion.fitarchiveapp.suggestions

import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.model.WearRecord
import app.luxion.fitarchiveapp.weather.CondicionTiempo
import app.luxion.fitarchiveapp.weather.TiempoDia
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Motor de sugerencias (T41): lógica pura, sin acceso a datos. T42 (aprender de likes) se engancha con
 * [AfinidadEstilo] y T43 (outfit del día) solo tiene que construir un [PedidoSugerencia] y pintar la lista.
 */
interface MotorSugerencias {
    /** Outfits sugeridos, el mejor primero. Lista vacía si el armario no da para ninguno. */
    fun sugerir(pedido: PedidoSugerencia): List<OutfitSugerido>
}

/** Qué se pide al motor: el armario y el contexto del día. Todo salvo [armario] y [hoy] es opcional. */
data class PedidoSugerencia(
    val armario: List<Garment>,
    /** Fecha de hoy, ISO-8601 (yyyy-MM-dd). */
    val hoy: String,
    /** Sin tiempo no se filtra ni se puntúa por temperatura. */
    val tiempo: TiempoDia? = null,
    /** Sin ocasión no se filtra ni se puntúa por uso. */
    val ocasion: Occasion? = null,
    /** Historial de usos (T33), para no repetir lo que se puso hace poco. */
    val usos: List<WearRecord> = emptyList(),
    val maximo: Int = 5,
    /** Gusto aprendido (T42); null = sin personalización. */
    val afinidad: AfinidadEstilo? = null,
    val pesos: PesosSugerencia = PesosSugerencia(),
)

/** Puntúa de 0.0 a 1.0 cuánto encaja un conjunto de prendas con el estilo de la persona. */
fun interface AfinidadEstilo {
    fun puntuar(prendas: List<Garment>): Double
}

/** Peso relativo de cada criterio en la puntuación final (se normalizan; no hace falta que sumen 1). */
data class PesosSugerencia(
    val clima: Double = 0.30,
    val ocasion: Double = 0.25,
    val colores: Double = 0.25,
    val frescura: Double = 0.20,
    val afinidad: Double = 0.30,
)

/** Puntuación de cada criterio, de 0.0 a 1.0; [afinidad] es null si no se pidió personalización. */
data class DesgloseSugerencia(
    val clima: Double,
    val ocasion: Double,
    val colores: Double,
    val frescura: Double,
    val afinidad: Double? = null,
)

data class OutfitSugerido(
    /** Ordenadas de arriba abajo: capa o vestido, superior, inferior, calzado. */
    val prendas: List<Garment>,
    /** De 0.0 a 1.0; es la media ponderada de [desglose]. */
    val puntuacion: Double,
    val desglose: DesgloseSugerencia,
)

/** Motor basado en reglas: clima, ocasión, color y rotación de prendas. */
class MotorSugerenciasPorReglas : MotorSugerencias {

    override fun sugerir(pedido: PedidoSugerencia): List<OutfitSugerido> {
        if (pedido.maximo <= 0) return emptyList()
        val temp = pedido.tiempo?.let { (it.maximaC + it.minimaC) / 2 }
        val ultimoUso = ultimosUsos(pedido.usos)
        val hoy = parsearFecha(pedido.hoy)

        val candidatas = pedido.armario
            .filter { !it.dirty && ranura(it.type) != null }
            .map { Candidata(it, temp?.let { t -> encajeClima(it.season, t) } ?: 1.0, encajeOcasion(it.usage, pedido.ocasion), frescura(ultimoUso[it.id], hoy)) }
            .filter { it.clima > 0.0 && it.ocasion > 0.0 }
            .filter { !(temp != null && temp >= CALOR_SIN_CAPA && ranura(it.prenda.type) == Ranura.CAPA) }

        fun mejores(r: Ranura) = candidatas.filter { ranura(it.prenda.type) == r }
            .sortedWith(compareByDescending<Candidata> { it.base }.thenBy { it.prenda.id })
            .take(MAX_POR_RANURA)

        val enteras = mejores(Ranura.ENTERA)
        val superiores = mejores(Ranura.SUPERIOR)
        val inferiores = mejores(Ranura.INFERIOR)
        val capas = mejores(Ranura.CAPA)
        val calzados = mejores(Ranura.CALZADO)

        val bases: List<List<Candidata>> =
            enteras.map { listOf(it) } + superiores.flatMap { s -> inferiores.map { listOf(s, it) } }
        val necesitaCapa = temp != null && (temp <= FRIO_CON_CAPA || pedido.tiempo.llueve())
        val opcionesCapa: List<Candidata?> = if (capas.isEmpty()) listOf(null) else if (necesitaCapa) capas else listOf(null) + capas
        val opcionesCalzado: List<Candidata?> = if (calzados.isEmpty()) listOf(null) else calzados

        val todos = buildList {
            for (base in bases) for (capa in opcionesCapa) for (calzado in opcionesCalzado) {
                add(evaluar(listOfNotNull(capa) + base + listOfNotNull(calzado), pedido, temp, necesitaCapa && capas.isNotEmpty()))
            }
        }
        val ordenados = todos
            .sortedWith(compareByDescending<Evaluado> { it.sugerido.puntuacion }.thenBy { it.sugerido.prendas.map { g -> g.id }.joinToString(",") })
        // Si hay combinaciones que casan de color, las que chocan no se sugieren.
        val buenos = ordenados.filter { it.combina }.ifEmpty { ordenados }
        return variar(buenos.map { it.sugerido }, pedido.maximo)
    }

    private class Candidata(val prenda: Garment, val clima: Double, val ocasion: Double, val frescura: Double) {
        /** Puntuación individual, solo para quedarse con las mejores de cada ranura. */
        val base get() = clima + ocasion + frescura
    }

    private class Evaluado(val sugerido: OutfitSugerido, val combina: Boolean)

    private fun evaluar(piezas: List<Candidata>, pedido: PedidoSugerencia, temp: Double?, faltaCapaSiNoHay: Boolean): Evaluado {
        val prendas = piezas.map { it.prenda }
        var clima = piezas.map { it.clima }.average()
        if (faltaCapaSiNoHay && piezas.none { ranura(it.prenda.type) == Ranura.CAPA }) {
            clima *= if (temp != null && temp <= FRIO_CON_CAPA) PENALIZA_SIN_CAPA else PENALIZA_SIN_CAPA_LLUVIA
        }
        val color = ColorRules.evaluar(prendas.map { it.color })
        val afinidad = pedido.afinidad?.puntuar(prendas)?.coerceIn(0.0, 1.0)
        val desglose = DesgloseSugerencia(
            clima = clima,
            ocasion = piezas.map { it.ocasion }.average(),
            colores = color.score,
            frescura = piezas.map { it.frescura }.average(),
            afinidad = afinidad,
        )
        val p = pedido.pesos
        val partes = listOf(desglose.clima to p.clima, desglose.ocasion to p.ocasion, desglose.colores to p.colores, desglose.frescura to p.frescura) +
            listOfNotNull(afinidad?.let { it to p.afinidad })
        val pesoTotal = partes.sumOf { it.second }
        val puntuacion = if (pesoTotal <= 0.0) 0.0 else partes.sumOf { it.first * it.second } / pesoTotal
        return Evaluado(OutfitSugerido(prendas, puntuacion.coerceIn(0.0, 1.0), desglose), color.combina)
    }

    /** Recorta a [maximo] evitando que una misma prenda protagonice casi todas las sugerencias. */
    private fun variar(ordenados: List<OutfitSugerido>, maximo: Int): List<OutfitSugerido> {
        val usadas = mutableMapOf<Long, Int>()
        val elegidos = mutableListOf<OutfitSugerido>()
        for (o in ordenados) {
            if (elegidos.size >= maximo) break
            if (o.prendas.any { (usadas[it.id] ?: 0) >= MAX_REPETICIONES_PRENDA }) continue
            elegidos += o
            o.prendas.forEach { usadas[it.id] = (usadas[it.id] ?: 0) + 1 }
        }
        return elegidos
    }

    private fun ultimosUsos(usos: List<WearRecord>): Map<Long, LocalDate> {
        val mapa = mutableMapOf<Long, LocalDate>()
        usos.forEach { u ->
            val f = parsearFecha(u.date) ?: return@forEach
            val actual = mapa[u.garmentId]
            if (actual == null || f > actual) mapa[u.garmentId] = f
        }
        return mapa
    }

    private fun frescura(ultimo: LocalDate?, hoy: LocalDate?): Double {
        if (ultimo == null || hoy == null) return 1.0
        val dias = ultimo.daysUntil(hoy).coerceAtLeast(0)
        return (dias.toDouble() / VENTANA_FRESCURA_DIAS).coerceIn(0.0, 1.0)
    }

    private enum class Ranura { ENTERA, SUPERIOR, INFERIOR, CAPA, CALZADO }

    companion object {
        /** Con esta temperatura media o menos (o con lluvia) se sugiere llevar capa. */
        const val FRIO_CON_CAPA = 15.0

        /** Con esta temperatura media o más no se sugieren chaquetas ni abrigos. */
        const val CALOR_SIN_CAPA = 24.0

        /** Temperatura media por debajo de la cual es invierno y por encima de [LIMITE_VERANO] verano. */
        const val LIMITE_INVIERNO = 13.0
        const val LIMITE_VERANO = 21.0

        /** Días sin ponerse una prenda para que cuente como "descansada" (frescura 1.0). */
        const val VENTANA_FRESCURA_DIAS = 7

        private const val MAX_POR_RANURA = 8
        private const val MAX_REPETICIONES_PRENDA = 2
        private const val PENALIZA_SIN_CAPA = 0.7
        private const val PENALIZA_SIN_CAPA_LLUVIA = 0.85

        private fun ranura(tipo: GarmentType): Ranura? = when (tipo) {
            GarmentType.DRESS, GarmentType.JUMPSUIT -> Ranura.ENTERA
            GarmentType.TSHIRT, GarmentType.SHIRT, GarmentType.SWEATER -> Ranura.SUPERIOR
            GarmentType.TROUSERS, GarmentType.JEANS, GarmentType.SKIRT, GarmentType.SHORTS -> Ranura.INFERIOR
            GarmentType.JACKET, GarmentType.COAT -> Ranura.CAPA
            GarmentType.SHOES -> Ranura.CALZADO
            GarmentType.BAG, GarmentType.ACCESSORY -> null
        }

        /** Estación que pide la temperatura media del día. */
        fun estacionPara(temperaturaC: Double): Season = when {
            temperaturaC < LIMITE_INVIERNO -> Season.WINTER
            temperaturaC > LIMITE_VERANO -> Season.SUMMER
            else -> Season.MID_SEASON
        }

        /** 0.0 (no vale con este tiempo) a 1.0 (ideal). */
        internal fun encajeClima(estacion: Season, temperaturaC: Double): Double {
            val ideal = estacionPara(temperaturaC)
            return when {
                estacion == ideal -> 1.0
                ideal == Season.MID_SEASON -> 0.4
                estacion == Season.MID_SEASON -> 0.6
                else -> 0.0
            }
        }

        /** 0.0 (no sirve para la ocasión) a 1.0 (ideal); sin ocasión, 1.0. */
        internal fun encajeOcasion(uso: Usage, ocasion: Occasion?): Double = when (ocasion) {
            null -> 1.0
            Occasion.WORK -> when (uso) { Usage.WORK -> 1.0; Usage.CASUAL, Usage.GOING_OUT -> 0.4; else -> 0.0 }
            Occasion.DATE -> when (uso) { Usage.GOING_OUT -> 1.0; Usage.PARTY -> 0.6; Usage.CASUAL -> 0.5; Usage.WORK -> 0.4; Usage.SPORT -> 0.0 }
            Occasion.PARTY -> when (uso) { Usage.PARTY -> 1.0; Usage.GOING_OUT -> 0.7; Usage.CASUAL, Usage.WORK -> 0.2; Usage.SPORT -> 0.0 }
            Occasion.CASUAL -> when (uso) { Usage.CASUAL -> 1.0; Usage.GOING_OUT -> 0.6; Usage.SPORT -> 0.5; Usage.WORK -> 0.4; Usage.PARTY -> 0.0 }
            Occasion.SPORT -> when (uso) { Usage.SPORT -> 1.0; Usage.CASUAL -> 0.4; else -> 0.0 }
        }

        private fun TiempoDia?.llueve(): Boolean = this != null && condicion in setOf(
            CondicionTiempo.LLOVIZNA, CondicionTiempo.LLUVIA, CondicionTiempo.NIEVE, CondicionTiempo.TORMENTA,
        )

        private fun parsearFecha(texto: String): LocalDate? = try {
            LocalDate.parse(texto)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
