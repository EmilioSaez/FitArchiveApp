package app.luxion.fitarchiveapp.tallas

/** Medida (corporal) que puede traer una talla en una tabla de marca. */
enum class Medida(val clave: String) {
    PECHO("pecho"),
    CINTURA("cintura"),
    CADERA("cadera"),
    PIE("pie"),
    ;

    companion object {
        fun deClave(clave: String): Medida? = entries.firstOrNull { it.clave == clave }
    }
}

/**
 * Valor de una medida en cm: un rango [min, max] o un valor único (min == max).
 * Los JSON guardan ambos casos como `[min, max]`; un valor único es `[v, v]`.
 */
data class Rango(val min: Double, val max: Double) {
    init {
        require(min <= max) { "Rango inválido: $min > $max" }
    }

    val esValorUnico: Boolean get() = min == max
    val centro: Double get() = (min + max) / 2.0

    operator fun contains(valor: Double): Boolean = valor in min..max
}

/** Una talla de una prenda: su etiqueta ("M", "38"…) y las medidas que la marca publica para ella. */
data class TallaMarca(
    val etiqueta: String,
    /** Solo las medidas presentes en la tabla; las ausentes no se rellenan. */
    val medidas: Map<Medida, Rango>,
    /** Equivalencia EU informativa si la tabla la trae (`eur` en H&M, `talla_eu` en Scalpers). */
    val equivalenciaEu: String? = null,
) {
    operator fun get(medida: Medida): Rango? = medidas[medida]
}

/** Tallas de un tipo de prenda de una marca, en el orden del JSON (de menor a mayor). */
data class TablaPrenda(
    val clave: String,
    val tallas: List<TallaMarca>,
) {
    fun talla(etiqueta: String): TallaMarca? =
        tallas.firstOrNull { it.etiqueta == etiqueta } ?: tallas.firstOrNull { it.etiqueta.equals(etiqueta, ignoreCase = true) }
}

/** Tabla de tallas de una marca (o la genérica europea). Puede estar vacía (`prendas` vacío). */
data class TablaTallas(
    val marca: String,
    val fuentes: List<String> = emptyList(),
    val fecha: String? = null,
    /** Unidad de las medidas; siempre "cm" en las tablas actuales. */
    val unidad: String = "cm",
    /** "rango" o "punto" si la tabla lo declara; informativo, cada [Rango] ya lo expresa. */
    val tipoValor: String? = null,
    /** Marca con prendas de talla única (p. ej. Brandy Melville). */
    val tallaUnica: Boolean = false,
    val notas: List<String> = emptyList(),
    /** Qué falta por recoger. */
    val pendiente: List<String> = emptyList(),
    /** Letra → numeración EU (tabla genérica). */
    val equivalencias: Map<String, List<Int>> = emptyMap(),
    val prendas: Map<String, TablaPrenda> = emptyMap(),
    /** Entradas del JSON que se ignoraron por no tener el formato esperado. */
    val avisos: List<String> = emptyList(),
) {
    val estaVacia: Boolean get() = prendas.values.all { it.tallas.isEmpty() }

    fun prenda(clave: String): TablaPrenda? = prendas[clave]
}
