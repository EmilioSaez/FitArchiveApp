package app.luxion.fitarchiveapp.ui.calendario

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.number

/** Mes del calendario. [mes] va de 1 a 12. */
data class MesCalendario(val anio: Int, val mes: Int) {
    init {
        require(mes in 1..12) { "Mes fuera de rango: $mes" }
    }

    val primerDia: LocalDate get() = LocalDate(anio, mes, 1)
    val ultimoDia: LocalDate get() = siguiente().primerDia.plus(-1, DateTimeUnit.DAY)

    fun siguiente() = if (mes == 12) MesCalendario(anio + 1, 1) else MesCalendario(anio, mes + 1)
    fun anterior() = if (mes == 1) MesCalendario(anio - 1, 12) else MesCalendario(anio, mes - 1)

    /** Título tipo "Noviembre 2026". */
    val titulo: String get() = "${NOMBRES_MESES[mes - 1]} $anio"

    /**
     * Celdas de la cuadrícula con semanas completas de lunes a domingo: fechas ISO del mes
     * y `null` en los huecos antes del día 1 y después del último día. Su tamaño es múltiplo de 7.
     */
    fun celdas(): List<String?> {
        val huecosInicio = primerDia.dayOfWeek.ordinal // lunes = 0
        val dias = ultimoDia.day
        val total = ((huecosInicio + dias + 6) / 7) * 7
        return List(total) { i ->
            val dia = i - huecosInicio + 1
            if (dia in 1..dias) LocalDate(anio, mes, dia).toString() else null
        }
    }

    companion object {
        val NOMBRES_MESES = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
        )
        val DIAS_SEMANA = listOf("L", "M", "X", "J", "V", "S", "D")

        /** Mes al que pertenece una fecha ISO (yyyy-MM-dd). */
        fun de(fechaIso: String): MesCalendario = LocalDate.parse(fechaIso).let { MesCalendario(it.year, it.month.number) }
    }
}
