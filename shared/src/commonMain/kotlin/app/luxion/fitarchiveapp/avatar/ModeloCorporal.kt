package app.luxion.fitarchiveapp.avatar

import app.luxion.fitarchiveapp.model.Fit
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Measurements
import app.luxion.fitarchiveapp.tallas.AjusteTalla
import app.luxion.fitarchiveapp.tallas.Medida
import app.luxion.fitarchiveapp.tallas.TablaTallas
import kotlin.math.max

/**
 * Modelo común de medidas (avatar 2D, colocación a escala real y recomendación de talla).
 * Todo en cm. Ver docs/avatar-escala.md.
 */

/**
 * Cuerpo de la usuaria con todos los valores rellenos: lo que falta en [Measurements] se completa con
 * [REFERENCIA], de modo que el avatar siempre se puede dibujar.
 */
data class PerfilCorporal(
    val alturaCm: Double = REFERENCIA_ALTURA,
    val pechoCm: Double = REFERENCIA_PECHO,
    val cinturaCm: Double = REFERENCIA_CINTURA,
    val caderaCm: Double = REFERENCIA_CADERA,
    /** Ancho de hombros (de hombro a hombro, en línea recta). */
    val hombrosCm: Double = REFERENCIA_HOMBROS,
    /** Largo del brazo, del hombro a la muñeca. */
    val brazoCm: Double = REFERENCIA_BRAZO,
    /** Largo de pierna por dentro (entrepierna a suelo). */
    val piernaCm: Double = REFERENCIA_PIERNA,
    val pieCm: Double = REFERENCIA_PIE,
    /** Qué medidas venían realmente de la usuaria (el resto son de referencia). */
    val reales: Set<MedidaCuerpo> = emptySet(),
) {
    operator fun get(m: MedidaCuerpo): Double = when (m) {
        MedidaCuerpo.ALTURA -> alturaCm
        MedidaCuerpo.PECHO -> pechoCm
        MedidaCuerpo.CINTURA -> cinturaCm
        MedidaCuerpo.CADERA -> caderaCm
        MedidaCuerpo.HOMBROS -> hombrosCm
        MedidaCuerpo.BRAZO -> brazoCm
        MedidaCuerpo.PIERNA -> piernaCm
        MedidaCuerpo.PIE -> pieCm
    }

    companion object {
        const val REFERENCIA_ALTURA = 165.0
        const val REFERENCIA_PECHO = 90.0
        const val REFERENCIA_CINTURA = 72.0
        const val REFERENCIA_CADERA = 98.0
        const val REFERENCIA_HOMBROS = 38.0
        const val REFERENCIA_BRAZO = 58.0
        const val REFERENCIA_PIERNA = 76.0
        const val REFERENCIA_PIE = 24.0

        /** Rangos aceptados: lo que queda fuera (error de tecleo, 0, negativo) cuenta como ausente. */
        private val RANGO = mapOf(
            MedidaCuerpo.ALTURA to 120.0..220.0,
            MedidaCuerpo.PECHO to 60.0..160.0,
            MedidaCuerpo.CINTURA to 45.0..150.0,
            MedidaCuerpo.CADERA to 70.0..170.0,
            MedidaCuerpo.HOMBROS to 28.0..60.0,
            MedidaCuerpo.BRAZO to 40.0..85.0,
            MedidaCuerpo.PIERNA to 55.0..105.0,
            MedidaCuerpo.PIE to 18.0..34.0,
        )

        fun de(m: Measurements?): PerfilCorporal {
            val reales = mutableSetOf<MedidaCuerpo>()
            fun v(medida: MedidaCuerpo, valor: Float?, ref: Double): Double {
                val d = valor?.toDouble()
                return if (d != null && d in RANGO.getValue(medida)) {
                    reales += medida
                    d
                } else {
                    ref
                }
            }
            return PerfilCorporal(
                alturaCm = v(MedidaCuerpo.ALTURA, m?.heightCm, REFERENCIA_ALTURA),
                pechoCm = v(MedidaCuerpo.PECHO, m?.bustCm, REFERENCIA_PECHO),
                cinturaCm = v(MedidaCuerpo.CINTURA, m?.waistCm, REFERENCIA_CINTURA),
                caderaCm = v(MedidaCuerpo.CADERA, m?.hipCm, REFERENCIA_CADERA),
                hombrosCm = v(MedidaCuerpo.HOMBROS, m?.shouldersCm, REFERENCIA_HOMBROS),
                brazoCm = v(MedidaCuerpo.BRAZO, m?.armCm, REFERENCIA_BRAZO),
                piernaCm = v(MedidaCuerpo.PIERNA, m?.legCm, REFERENCIA_PIERNA),
                pieCm = v(MedidaCuerpo.PIE, m?.footCm, REFERENCIA_PIE),
                reales = reales,
            )
        }
    }
}

enum class MedidaCuerpo { ALTURA, PECHO, CINTURA, CADERA, HOMBROS, BRAZO, PIERNA, PIE }

/** Qué mide cada eje de la foto de la prenda al ponerla a escala real. */
enum class EjeEscala { ALTO, ANCHO }

/** De dónde salen las dimensiones de una prenda. */
enum class FuenteDimensiones { TABLA_TALLAS, MEDIDAS_USUARIA, REFERENCIA }

/**
 * Dimensiones de una prenda **puesta** (cm). Los contornos son de la prenda, no del cuerpo:
 * cuerpo + holgura del corte. Las dimensiones que no aplican al tipo (manga en un pantalón) son null.
 */
data class DimensionesPrenda(
    val pechoCm: Double? = null,
    val cinturaCm: Double? = null,
    val caderaCm: Double? = null,
    /** Alto total de la prenda (hombro/cintura → bajo; en calzado, altura de caña). */
    val largoCm: Double,
    /** Largo de manga desde el hombro; 0 = sin mangas. */
    val mangaCm: Double? = null,
    /** Qué eje de la foto se iguala a [medidaEjeCm]. */
    val eje: EjeEscala = EjeEscala.ALTO,
    val fuente: FuenteDimensiones = FuenteDimensiones.REFERENCIA,
) {
    /** Medida real (cm) que debe tener en pantalla el [eje] de la foto. */
    val medidaEjeCm: Double
        get() = when (eje) {
            EjeEscala.ALTO -> largoCm
            EjeEscala.ANCHO -> pechoCm ?: caderaCm ?: largoCm
        }
}

/** Cálculo de dimensiones de prenda a partir de la talla, el corte y las tablas de tallas. */
object DimensionesPrendaCalc {
    /** Holgura (cm de contorno) que añade el corte sobre el cuerpo, en pecho / cintura / cadera. */
    private data class Holgura(val pecho: Double, val cintura: Double, val cadera: Double)

    private fun holgura(corte: Fit) = when (corte) {
        Fit.SLIM -> Holgura(2.0, 1.0, 2.0)
        Fit.NORMAL -> Holgura(8.0, 4.0, 6.0)
        Fit.OVERSIZE -> Holgura(22.0, 12.0, 16.0)
    }

    /** Largo típico (cm) por subtipo para una persona de [PerfilCorporal.REFERENCIA_ALTURA]. */
    fun largoBase(subtipo: Subtipo): Double = when (subtipo) {
        Subtipo.TOP_SIN_MANGAS_CORTO -> 48.0
        Subtipo.TOP_MANGA_CORTA -> 62.0
        Subtipo.TOP_MANGA_LARGA -> 64.0
        Subtipo.TOP_LARGO_CADERA -> 76.0
        Subtipo.OUTER_CHAQUETA -> 60.0
        Subtipo.OUTER_ABRIGO -> 100.0
        Subtipo.BOTTOM_SHORTS -> 36.0
        Subtipo.BOTTOM_FALDA_MINI -> 40.0
        Subtipo.BOTTOM_FALDA_MIDI -> 72.0
        Subtipo.BOTTOM_FALDA_LARGA -> 98.0
        Subtipo.BOTTOM_PANTALON_CORTO -> 62.0
        Subtipo.BOTTOM_PANTALON_LARGO -> 104.0
        Subtipo.DRESS_CORTO -> 92.0
        Subtipo.DRESS_MIDI -> 118.0
        Subtipo.DRESS_LARGO -> 148.0
        Subtipo.SHOES_PAR, Subtipo.SHOES_PIE_DER, Subtipo.SHOES_PIE_IZQ -> 11.0
        Subtipo.SHOES_BOTA_ALTA -> 40.0
        Subtipo.ACC_SOMBRERO_GORRO -> 14.0
        Subtipo.ACC_DIADEMA -> 4.0
        Subtipo.ACC_GAFAS -> 5.0
        Subtipo.ACC_PENDIENTES -> 5.0
        Subtipo.ACC_COLLAR -> 40.0
        Subtipo.ACC_PANUELO -> 30.0
        Subtipo.ACC_CINTURON -> 4.0
        Subtipo.ACC_BOLSO_MANO_DER -> 30.0
        Subtipo.ACC_BOLSO_HOMBRO_IZQ -> 28.0
    }

    private fun mangaBase(subtipo: Subtipo): Double? = when (subtipo) {
        Subtipo.TOP_SIN_MANGAS_CORTO -> 0.0
        Subtipo.TOP_MANGA_CORTA -> 22.0
        Subtipo.TOP_MANGA_LARGA, Subtipo.TOP_LARGO_CADERA -> 58.0
        Subtipo.OUTER_CHAQUETA, Subtipo.OUTER_ABRIGO -> 60.0
        Subtipo.DRESS_CORTO, Subtipo.DRESS_MIDI, Subtipo.DRESS_LARGO -> 22.0
        else -> null
    }

    /** Centro del rango de [medida] para [talla] en la tabla de la marca (o la genérica); null si no hay. */
    internal fun medidaDeTalla(
        medida: Medida,
        marca: String?,
        tipo: GarmentType,
        talla: String?,
        tablas: Map<String, TablaTallas>,
    ): Double? {
        if (talla == null) return null
        val buscada = marca?.let(AjusteTalla::normalizarMarca).orEmpty()
        val candidatas = buildList {
            if (buscada.isNotEmpty()) {
                tablas.entries.firstOrNull { it.key != AjusteTalla.ID_GENERICA && AjusteTalla.normalizarMarca(it.key) == buscada }
                    ?.value?.let(::add)
            }
            tablas[AjusteTalla.ID_GENERICA]?.let(::add)
        }
        val claves = AjusteTalla.clavesPara(tipo)
        for (tabla in candidatas) {
            for (clave in claves) {
                val r = tabla.prenda(clave)?.talla(talla.trim())?.get(medida) ?: continue
                return r.centro
            }
        }
        return null
    }

    /**
     * Dimensiones de la prenda. Prioridad para cada contorno: la talla en la tabla (cuerpo al que va dirigida)
     * → las medidas de la usuaria (se supone que le queda bien) → la referencia. Luego se suma la holgura del [corte].
     */
    fun calcular(
        subtipo: Subtipo,
        tipo: GarmentType,
        talla: String?,
        marca: String?,
        corte: Fit,
        cuerpo: PerfilCorporal,
        tablas: Map<String, TablaTallas>,
    ): DimensionesPrenda {
        val h = holgura(corte)
        var fuente = FuenteDimensiones.REFERENCIA
        fun contorno(m: Medida, usuaria: Double, usuariaReal: Boolean, ease: Double): Double {
            val t = medidaDeTalla(m, marca, tipo, talla, tablas)
            return when {
                t != null -> { fuente = FuenteDimensiones.TABLA_TALLAS; t + ease }
                usuariaReal -> { if (fuente == FuenteDimensiones.REFERENCIA) fuente = FuenteDimensiones.MEDIDAS_USUARIA; usuaria + ease }
                else -> usuaria + ease
            }
        }
        val pecho = contorno(Medida.PECHO, cuerpo.pechoCm, MedidaCuerpo.PECHO in cuerpo.reales, h.pecho)
        val cintura = contorno(Medida.CINTURA, cuerpo.cinturaCm, MedidaCuerpo.CINTURA in cuerpo.reales, h.cintura)
        val cadera = contorno(Medida.CADERA, cuerpo.caderaCm, MedidaCuerpo.CADERA in cuerpo.reales, h.cadera)
        val largo = largoBase(subtipo)

        return when (subtipo.slot) {
            Slot.TOP, Slot.OUTER, Slot.DRESS -> DimensionesPrenda(
                pechoCm = pecho,
                cinturaCm = cintura,
                caderaCm = cadera,
                largoCm = largo,
                mangaCm = mangaBase(subtipo),
                fuente = fuente,
            )
            Slot.BOTTOM -> DimensionesPrenda(cinturaCm = cintura, caderaCm = cadera, largoCm = largo, fuente = fuente)
            Slot.SHOES -> {
                val pie = medidaDeTalla(Medida.PIE, marca, tipo, talla, tablas)
                val par = subtipo == Subtipo.SHOES_PAR
                if (subtipo == Subtipo.SHOES_BOTA_ALTA) {
                    DimensionesPrenda(largoCm = largo, fuente = if (pie != null) FuenteDimensiones.TABLA_TALLAS else FuenteDimensiones.REFERENCIA)
                } else {
                    // Un zapato mide el largo del pie + ~1,5 cm de horma; un par visto de frente, dos zapatos y el hueco.
                    val unZapato = (pie ?: cuerpo.pieCm) + 1.5
                    DimensionesPrenda(
                        caderaCm = if (par) unZapato * 2 + 2 else unZapato,
                        largoCm = largo,
                        eje = EjeEscala.ANCHO,
                        fuente = if (pie != null) FuenteDimensiones.TABLA_TALLAS else FuenteDimensiones.MEDIDAS_USUARIA,
                    )
                }
            }
            else -> DimensionesPrenda(largoCm = largo)
        }
    }
}

/**
 * Geometría del avatar para un [PerfilCorporal]: pasa cm a unidades del lienzo (docs/avatar.md) y da las
 * alturas de los puntos de referencia. Es la única fuente de la escala real.
 */
class EscalaAvatar(val cuerpo: PerfilCorporal) {
    /** Unidades de lienzo por cm (vertical y horizontal: la figura es isótropa). */
    val unidadesPorCm: Double = (SUELO - CORONILLA) / cuerpo.alturaCm

    fun cm(unidades: Double) = unidades / unidadesPorCm
    fun u(cm: Double): Float = (cm * unidadesPorCm).toFloat()

    /** y (lienzo) de un punto a [cmDesdeSuelo] centímetros del suelo. */
    fun yDesdeSuelo(cmDesdeSuelo: Double): Float = SUELO.toFloat() - u(cmDesdeSuelo)

    val yEntrepierna get() = yDesdeSuelo(cuerpo.piernaCm)
    val yCadera get() = yDesdeSuelo(cuerpo.piernaCm + 0.07 * cuerpo.alturaCm)
    val yCintura get() = yDesdeSuelo(cuerpo.alturaCm * 0.625)
    val yPecho get() = yDesdeSuelo(cuerpo.alturaCm * 0.73)
    val yHombros get() = yDesdeSuelo(cuerpo.alturaCm * 0.82)
    val yCuello get() = yDesdeSuelo(cuerpo.alturaCm * 0.845)
    val yBarbilla get() = yDesdeSuelo(cuerpo.alturaCm * 0.865)
    val yRodilla get() = yDesdeSuelo(cuerpo.alturaCm * 0.285)
    val yTobillo get() = yDesdeSuelo(cuerpo.alturaCm * 0.04)

    companion object {
        const val CORONILLA = 44.0
        const val SUELO = 968.0
        const val CENTRO_X = 200f

        /** Vista frontal: ancho del cuerpo ≈ esta fracción de su contorno. */
        const val ANCHO_FRONTAL_PECHO = 0.31
        const val ANCHO_FRONTAL_CINTURA = 0.30
        const val ANCHO_FRONTAL_CADERA = 0.31

        /**
         * Una prenda plana mide ≈ contorno/2 de ancho, pero puesta de frente se ve ≈ 0,3 × contorno. La foto se
         * comprime horizontalmente por este factor sobre su escala real.
         */
        const val ESCORZO_PRENDA = 0.72f
    }
}

/**
 * Rectángulo (unidades de lienzo) de una prenda a escala real sobre el avatar.
 * Solo las prendas de cuerpo y el calzado van a escala real; los accesorios usan su caja (escalada con la altura).
 */
fun colocacionReal(
    subtipo: Subtipo,
    dim: DimensionesPrenda,
    escala: EscalaAvatar,
    anchoImg: Int,
    altoImg: Int,
    nucleo: Nucleo? = null,
): Rect? {
    if (anchoImg <= 0 || altoImg <= 0) return null
    val slot = subtipo.slot
    if (slot == Slot.ACC_HEAD || slot == Slot.ACC_FACE || slot == Slot.ACC_NECK || slot == Slot.ACC_WAIST || slot == Slot.ACC_HAND) {
        return null
    }
    // Con núcleo, escala y posición salen de él (lo fino que sobresale no cuenta), pero se devuelve la imagen entera.
    val n = nucleo?.takeIf { slot.usaNucleo && it.ancho > 0 && it.alto > 0 } ?: Nucleo(0, 0, anchoImg, altoImg)
    val destinoCm = max(dim.medidaEjeCm, 1.0)
    val s = when (dim.eje) {
        EjeEscala.ALTO -> escala.u(destinoCm) / n.alto
        EjeEscala.ANCHO -> escala.u(destinoCm) / n.ancho
    }
    val h = altoImg * s
    // Compresión frontal solo en prendas de tela; el calzado ya viene de frente.
    val compresion = if (slot == Slot.SHOES) 1f else EscalaAvatar.ESCORZO_PRENDA
    val w = anchoImg * s * compresion
    val cx = EscalaAvatar.CENTRO_X + if (subtipo == Subtipo.SHOES_PIE_DER) escala.u(6.0) else if (subtipo == Subtipo.SHOES_PIE_IZQ) -escala.u(6.0) else 0f
    val yNucleo = when (slot) {
        Slot.TOP, Slot.OUTER, Slot.DRESS -> escala.yCuello - escala.u(2.0)
        Slot.BOTTOM -> escala.yCintura - escala.u(1.5)
        Slot.SHOES -> EscalaAvatar.SUELO.toFloat() + escala.u(1.0) - h
        else -> return null
    }
    // El centro del núcleo (no el de la imagen) cae en cx; su borde superior, en yNucleo.
    val cxNucleo = (n.colInicio + n.colFin) / 2f * s * compresion
    return Rect(cx - cxNucleo, yNucleo - n.filaInicio * s, w, h)
}
