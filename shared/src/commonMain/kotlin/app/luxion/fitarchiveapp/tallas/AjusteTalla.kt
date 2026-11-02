package app.luxion.fitarchiveapp.tallas

import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Measurements
import kotlin.math.abs

/** Cómo queda una talla en una zona del cuerpo (o en conjunto). */
enum class EtiquetaAjuste { AJUSTADA, BIEN, HOLGADA }

/** Por qué un resultado puede no traer etiquetas. */
enum class EstadoAjuste {
    /** Hay etiquetas por zona y global. */
    OK,

    /** Tipo de prenda sin talla corporal (bolso, accesorio). */
    NO_APLICA,

    /** Ninguna tabla (ni la de la marca ni la genérica) cubre este tipo de prenda. */
    SIN_TABLA,

    /** Hay tabla, pero la usuaria no tiene ninguna de las medidas que la tabla trae para este tipo. */
    SIN_MEDIDAS,

    /** La talla indicada no existe en la tabla usada (o no se indicó); solo se calcula la talla ideal. */
    TALLA_NO_ENCONTRADA,
}

/**
 * Umbrales en cm. Para tallas de rango [min, max], dentro del rango es "bien". Fuera hay un margen
 * antes de cambiar la etiqueta:
 * - cuerpo > `max` + [margenAjustada] → AJUSTADA (la prenda aprieta);
 * - cuerpo < `min` − [margenHolgada] → HOLGADA (sobra prenda).
 * Se tolera más holgura que estrechez: una prenda algo grande se lleva, una pequeña no.
 * Las tallas de valor único (Brownie, Scalpers) se tratan como `v ± semiAnchoPunto`
 * (las tallas avanzan 3–4 cm, así que quedan contiguas).
 * T29 (etiqueta de corte) ampliará esta clase con sus parámetros.
 */
data class OpcionesAjuste(
    val margenAjustada: Double = 1.0,
    val margenHolgada: Double = 3.0,
    val semiAnchoPunto: Double = 2.0,
    /** Calzado: las tallas avanzan ~0,65 cm, así que los márgenes son más finos. */
    val margenAjustadaPie: Double = 0.3,
    val margenHolgadaPie: Double = 1.0,
    val semiAnchoPuntoPie: Double = 0.4,
    /** Al elegir la talla ideal, quedarse corta pesa más que quedarse grande. */
    val pesoAjustada: Double = 2.0,
)

/** Medidas de la usuaria en cm; las ausentes son null. */
data class MedidasUsuaria(
    val pecho: Double? = null,
    val cintura: Double? = null,
    val cadera: Double? = null,
    val pie: Double? = null,
) {
    operator fun get(medida: Medida): Double? = when (medida) {
        Medida.PECHO -> pecho
        Medida.CINTURA -> cintura
        Medida.CADERA -> cadera
        Medida.PIE -> pie
    }

    companion object {
        /** `Measurements` aún no guarda el pie: se pasa aparte si se conoce. */
        fun de(m: Measurements?, pieCm: Float? = null) = MedidasUsuaria(
            pecho = m?.bustCm?.toDouble(),
            cintura = m?.waistCm?.toDouble(),
            cadera = m?.hipCm?.toDouble(),
            pie = pieCm?.toDouble(),
        )
    }
}

data class ResultadoAjuste(
    val estado: EstadoAjuste,
    /** Solo las zonas con medida de la usuaria y dato en la talla. */
    val porZona: Map<Medida, EtiquetaAjuste> = emptyMap(),
    val global: EtiquetaAjuste? = null,
    /** Talla de la tabla usada que mejor encaja con las medidas; null si no se puede calcular. */
    val tallaIdeal: String? = null,
    /** Marca de la tabla usada (la de la marca o la genérica). */
    val tablaUsada: String? = null,
    /** true si se usó la tabla genérica EU (marca sin datos o sin ese tipo de prenda). */
    val esGenerica: Boolean = false,
    /** Clave de prenda de la tabla usada (`camiseta`, `parte_superior`…). */
    val clavePrenda: String? = null,
)

object AjusteTalla {
    const val ID_GENERICA = "generica-eu"

    /** Claves de prenda (de todas las tablas) por tipo de prenda de la app, de más a menos específica. */
    fun clavesPara(tipo: GarmentType): List<String> = when (tipo) {
        GarmentType.TSHIRT, GarmentType.SHIRT, GarmentType.SWEATER -> listOf("camiseta", "parte_superior")
        GarmentType.JACKET, GarmentType.COAT -> listOf("chaqueta", "abrigos_chaquetas")
        GarmentType.TROUSERS, GarmentType.JEANS, GarmentType.SHORTS ->
            listOf("pantalon", "parte_inferior", "parte_inferior_numerica_us")
        GarmentType.SKIRT -> listOf("falda", "parte_inferior")
        GarmentType.DRESS, GarmentType.JUMPSUIT -> listOf("vestido", "vestidos")
        GarmentType.SHOES -> listOf("calzado")
        GarmentType.BAG, GarmentType.ACCESSORY -> emptyList()
    }

    /** "Pull&Bear" → "pullbear"; "Massimo Dutti" y "massimo_dutti" → "massimodutti". */
    fun normalizarMarca(marca: String): String = marca.lowercase()
        .map { c ->
            when (c) {
                'á', 'à', 'ä', 'â' -> 'a'
                'é', 'è', 'ë', 'ê' -> 'e'
                'í', 'ì', 'ï', 'î' -> 'i'
                'ó', 'ò', 'ö', 'ô' -> 'o'
                'ú', 'ù', 'ü', 'û' -> 'u'
                else -> c
            }
        }
        .filter { it in 'a'..'z' || it in '0'..'9' }
        .joinToString("")

    /**
     * @param tablas tablas por id (`CargadorTallas.cargarTodas()`); debe incluir [ID_GENERICA] para el respaldo.
     * @param marca texto libre de la prenda; null o desconocida → genérica.
     */
    fun calcular(
        medidas: MedidasUsuaria,
        marca: String?,
        tipo: GarmentType,
        talla: String?,
        tablas: Map<String, TablaTallas>,
        opciones: OpcionesAjuste = OpcionesAjuste(),
    ): ResultadoAjuste {
        val claves = clavesPara(tipo)
        if (claves.isEmpty()) return ResultadoAjuste(EstadoAjuste.NO_APLICA)

        val buscada = marca?.let(::normalizarMarca).orEmpty()
        val deMarca = if (buscada.isEmpty()) {
            null
        } else {
            tablas.entries.firstOrNull { it.key != ID_GENERICA && normalizarMarca(it.key) == buscada }?.value
        }
        val generica = tablas[ID_GENERICA]

        val delaMarca = elegir(deMarca, claves, talla)
        val (tabla, prenda) = delaMarca ?: elegir(generica, claves, talla)
            ?: return ResultadoAjuste(EstadoAjuste.SIN_TABLA)
        val base = ResultadoAjuste(
            estado = EstadoAjuste.SIN_MEDIDAS,
            tablaUsada = tabla.marca,
            esGenerica = delaMarca == null,
            clavePrenda = prenda.clave,
        )

        val hayMedidas = prenda.tallas.any { t -> t.medidas.keys.any { medidas[it] != null } }
        if (!hayMedidas) return base

        val ideal = prenda.tallas
            .map { it to puntuacion(it, medidas, opciones) }
            .minWithOrNull(compareBy({ it.second.first }, { it.second.second }))
            ?.first?.etiqueta

        val actual = talla?.let { prenda.talla(it.trim()) }
            ?: return base.copy(estado = EstadoAjuste.TALLA_NO_ENCONTRADA, tallaIdeal = ideal)

        val zonas = actual.medidas.mapNotNull { (m, r) ->
            medidas[m]?.let { m to etiquetar(it, r, m, opciones) }
        }.toMap()
        if (zonas.isEmpty()) return base.copy(tallaIdeal = ideal)
        return base.copy(estado = EstadoAjuste.OK, porZona = zonas, global = global(zonas.values), tallaIdeal = ideal)
    }

    /** Global: si algo aprieta, ajustada; si no, y algo sobra, holgada; si no, bien. */
    fun global(zonas: Collection<EtiquetaAjuste>): EtiquetaAjuste? = when {
        zonas.isEmpty() -> null
        EtiquetaAjuste.AJUSTADA in zonas -> EtiquetaAjuste.AJUSTADA
        EtiquetaAjuste.HOLGADA in zonas -> EtiquetaAjuste.HOLGADA
        else -> EtiquetaAjuste.BIEN
    }

    private fun limites(r: Rango, m: Medida, o: OpcionesAjuste): Rango {
        if (!r.esValorUnico) return r
        val s = if (m == Medida.PIE) o.semiAnchoPuntoPie else o.semiAnchoPunto
        return Rango(r.min - s, r.max + s)
    }

    internal fun etiquetar(cuerpo: Double, r: Rango, m: Medida, o: OpcionesAjuste): EtiquetaAjuste {
        val l = limites(r, m, o)
        val ajustada = if (m == Medida.PIE) o.margenAjustadaPie else o.margenAjustada
        val holgada = if (m == Medida.PIE) o.margenHolgadaPie else o.margenHolgada
        return when {
            cuerpo > l.max + ajustada -> EtiquetaAjuste.AJUSTADA
            cuerpo < l.min - holgada -> EtiquetaAjuste.HOLGADA
            else -> EtiquetaAjuste.BIEN
        }
    }

    /** (penalización fuera de rango, distancia al centro): menor es mejor; el empate lo gana la primera talla. */
    private fun puntuacion(t: TallaMarca, medidas: MedidasUsuaria, o: OpcionesAjuste): Pair<Double, Double> {
        var fuera = 0.0
        var centro = 0.0
        for ((m, r) in t.medidas) {
            val c = medidas[m] ?: continue
            val l = limites(r, m, o)
            fuera += when {
                c > l.max -> (c - l.max) * o.pesoAjustada
                c < l.min -> l.min - c
                else -> 0.0
            }
            centro += abs(c - r.centro)
        }
        return fuera to centro
    }

    /** Primera clave candidata que contiene la talla; si ninguna, la primera con datos. */
    private fun elegir(tabla: TablaTallas?, claves: List<String>, talla: String?): Pair<TablaTallas, TablaPrenda>? {
        if (tabla == null) return null
        val existentes = claves.mapNotNull { tabla.prenda(it) }.filter { it.tallas.isNotEmpty() }
        val p = existentes.firstOrNull { talla != null && it.talla(talla.trim()) != null } ?: existentes.firstOrNull()
        return p?.let { tabla to it }
    }
}
