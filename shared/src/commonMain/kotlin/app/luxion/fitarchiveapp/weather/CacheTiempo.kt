package app.luxion.fitarchiveapp.weather

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Caché corta del tiempo (O18). Guarda la última respuesta válida para unas coordenadas ya aproximadas y la sirve
 * durante [vidaMs] (30 min por defecto); caduca también al cambiar el día, porque la máxima y la mínima son las de hoy.
 * Vive en memoria y debe crearse una sola vez para toda la app, no por pantalla.
 */
class CacheTiempo(
    private val vidaMs: Long = VIDA_POR_DEFECTO_MS,
    private val ahora: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val zona: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private class Entrada(val coordenadas: Coordenadas, val tiempo: TiempoDia, val guardadoEn: Long, val dia: LocalDate)

    private var entrada: Entrada? = null

    /** El tiempo guardado para [coordenadas], o null si no hay, caducó o es de otro día. */
    fun obtener(coordenadas: Coordenadas): TiempoDia? {
        val e = entrada ?: return null
        val t = ahora()
        val vigente = e.coordenadas == coordenadas && t - e.guardadoEn in 0 until vidaMs && e.dia == diaDe(t)
        return if (vigente) e.tiempo else null
    }

    fun guardar(coordenadas: Coordenadas, tiempo: TiempoDia) {
        val t = ahora()
        entrada = Entrada(coordenadas, tiempo, t, diaDe(t))
    }

    fun vaciar() {
        entrada = null
    }

    private fun diaDe(t: Long): LocalDate = Instant.fromEpochMilliseconds(t).toLocalDateTime(zona()).date

    companion object {
        const val VIDA_POR_DEFECTO_MS = 30 * 60 * 1000L
    }
}
