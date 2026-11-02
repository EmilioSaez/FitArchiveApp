package app.luxion.fitarchiveapp.ui.calendario

import app.luxion.fitarchiveapp.data.CalendarRepository
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.model.CalendarDay
import app.luxion.fitarchiveapp.model.Outfit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarioUiState(
    val mes: MesCalendario,
    /** Fecha ISO de hoy, para resaltarla. */
    val hoy: String,
    /** Días con registro del mes visible, por fecha. */
    val dias: Map<String, CalendarDay> = emptyMap(),
    /** Outfits guardados, para elegir uno al asignar y mostrar su nombre. */
    val outfits: List<Outfit> = emptyList(),
    /** Fecha ISO del día abierto (hoja de detalle), o null. */
    val seleccionado: String? = null,
    val error: String? = null,
) {
    val celdas: List<String?> get() = mes.celdas()
    val diaSeleccionado: CalendarDay? get() = seleccionado?.let { dias[it] }
    fun outfitDe(fecha: String): Outfit? = dias[fecha]?.outfitId?.let { id -> outfits.firstOrNull { it.id == id } }
}

/** Estado del calendario (T32): navegar por meses y asignar un outfit (y nota) a cada día. */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarioViewModel(
    private val calendario: CalendarRepository,
    outfits: OutfitRepository,
    private val scope: CoroutineScope,
    hoy: String,
) {
    private val _estado = MutableStateFlow(CalendarioUiState(MesCalendario.de(hoy), hoy))
    val estado: StateFlow<CalendarioUiState> = _estado.asStateFlow()

    init {
        scope.launch {
            _estado
                .map { it.mes }
                .distinctUntilChanged()
                .flatMapLatest { mes -> calendario.observeBetween(mes.primerDia.toString(), mes.ultimoDia.toString()) }
                .collect { lista -> _estado.update { it.copy(dias = lista.associateBy { d -> d.date }) } }
        }
        scope.launch {
            outfits.observeAll().collect { lista -> _estado.update { it.copy(outfits = lista) } }
        }
    }

    fun mesSiguiente() = _estado.update { it.copy(mes = it.mes.siguiente(), seleccionado = null) }

    fun mesAnterior() = _estado.update { it.copy(mes = it.mes.anterior(), seleccionado = null) }

    fun irAHoy() = _estado.update { it.copy(mes = MesCalendario.de(it.hoy), seleccionado = null) }

    fun seleccionar(fecha: String) = _estado.update { it.copy(seleccionado = fecha) }

    fun cerrarDia() = _estado.update { it.copy(seleccionado = null) }

    /** Asigna [outfit] al día abierto, conservando su nota. */
    fun asignarOutfit(outfit: Outfit) {
        val fecha = _estado.value.seleccionado ?: return
        val nota = _estado.value.dias[fecha]?.note
        accion("No se pudo asignar el outfit") { calendario.set(CalendarDay(fecha, outfit.id, nota)) }
    }

    /** Quita el outfit del día abierto; si tiene nota, la conserva. */
    fun quitarOutfit() {
        val fecha = _estado.value.seleccionado ?: return
        val dia = _estado.value.dias[fecha] ?: return
        accion("No se pudo quitar el outfit") {
            if (dia.note.isNullOrBlank()) calendario.clear(fecha) else calendario.set(dia.copy(outfitId = null))
        }
    }

    /** Guarda la nota del día abierto (vacía = sin nota); si el día queda sin outfit ni nota, se borra. */
    fun guardarNota(texto: String) {
        val fecha = _estado.value.seleccionado ?: return
        val nota = texto.trim().ifEmpty { null }
        val outfitId = _estado.value.dias[fecha]?.outfitId
        accion("No se pudo guardar la nota") {
            if (outfitId == null && nota == null) calendario.clear(fecha) else calendario.set(CalendarDay(fecha, outfitId, nota))
        }
    }

    fun descartarError() = _estado.update { it.copy(error = null) }

    private fun accion(mensajeError: String, bloque: suspend () -> Unit) {
        scope.launch {
            try {
                bloque()
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                _estado.update { it.copy(error = mensajeError) }
            }
        }
    }
}
