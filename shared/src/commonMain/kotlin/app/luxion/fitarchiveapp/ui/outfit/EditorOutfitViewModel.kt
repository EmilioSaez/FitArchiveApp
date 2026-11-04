package app.luxion.fitarchiveapp.ui.outfit

import app.luxion.fitarchiveapp.avatar.AjustePrenda
import app.luxion.fitarchiveapp.avatar.Slot
import app.luxion.fitarchiveapp.avatar.slotDe
import app.luxion.fitarchiveapp.avatar.subtipoPorDefecto
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Slots que el editor ofrece (los que alcanzan las prendas del armario, en orden de pestañas). */
val SLOTS_EDITOR: List<Slot> =
    listOf(Slot.TOP, Slot.OUTER, Slot.BOTTOM, Slot.DRESS, Slot.SHOES, Slot.ACC_NECK, Slot.ACC_HAND)

/** Prenda elegida para un slot, con su ajuste manual. */
data class PrendaElegida(val prenda: Garment, val ajuste: AjustePrenda = AjustePrenda.Ninguno)

data class EditorOutfitUiState(
    val nombre: String = "",
    val ocasion: Occasion? = null,
    val elegidas: Map<Slot, PrendaElegida> = emptyMap(),
    val slotActivo: Slot = SLOTS_EDITOR.first(),
    val armario: List<Garment> = emptyList(),
    val guardando: Boolean = false,
    /** Id del outfit guardado; el editor se cierra al recibirlo. */
    val idGuardado: Long? = null,
    val error: String? = null,
    /** Editando un outfit guardado y aún cargándolo: no se puede guardar hasta que termine. */
    val cargando: Boolean = false,
) {
    /** Prendas del armario que caben en el slot activo. */
    val candidatas: List<Garment> get() = armario.filter { slotDe(it.type) == slotActivo }
    val elegidaActiva: PrendaElegida? get() = elegidas[slotActivo]
    val puedeGuardar get() = elegidas.isNotEmpty() && nombre.isNotBlank() && !guardando && !cargando && idGuardado == null
}

/**
 * Estado del editor de outfit (T18): prendas por slot, ajuste, nombre/ocasión y guardado.
 * Con [outfitId] edita ese outfit guardado (conserva su favorito y fecha) en vez de crear uno nuevo.
 */
class EditorOutfitViewModel(
    private val outfits: OutfitRepository,
    armario: GarmentRepository,
    private val scope: CoroutineScope,
    private val outfitId: Long? = null,
    /** Prendas con las que arranca un outfit nuevo (las elegidas en el armario). */
    private val prendasIniciales: List<Long> = emptyList(),
    private val ahora: () -> Long,
) {
    private val _estado = MutableStateFlow(EditorOutfitUiState(cargando = outfitId != null))
    val estado: StateFlow<EditorOutfitUiState> = _estado.asStateFlow()

    /** Outfit que se edita (para conservar favorito y fecha al guardar); null si es nuevo o aún no cargó. */
    private var original: Outfit? = null

    init {
        scope.launch {
            if (outfitId != null) cargarOutfit(outfitId, armario)
            var inicialesPuestas = outfitId != null || prendasIniciales.isEmpty()
            armario.observeAll().collect { prendas ->
                if (!inicialesPuestas) {
                    inicialesPuestas = true
                    val porId = prendas.associateBy { it.id }
                    prendasIniciales.mapNotNull { porId[it] }.forEach { alternarPrenda(it) }
                }
                // Si una prenda elegida se elimina del armario, sale también del outfit.
                val ids = prendas.map { it.id }.toSet()
                _estado.update { e ->
                    e.copy(armario = prendas, elegidas = e.elegidas.filterValues { it.prenda.id in ids })
                }
            }
        }
    }

    private suspend fun cargarOutfit(id: Long, armario: GarmentRepository) {
        try {
            val guardado = outfits.get(id)
            if (guardado == null) {
                _estado.update { it.copy(cargando = false, error = "No se encontró el outfit") }
                return
            }
            val prendas = armario.observeAll().first().associateBy { it.id }
            val elegidas = guardado.garments.mapNotNull { g ->
                val prenda = prendas[g.garmentId] ?: return@mapNotNull null
                val slot = slotDe(prenda.type)
                slot to PrendaElegida(prenda, AjustePrenda(g.x, g.y, g.scale))
            }.toMap()
            original = guardado.outfit
            _estado.update {
                it.copy(
                    nombre = guardado.outfit.name,
                    ocasion = guardado.outfit.occasion,
                    elegidas = elegidas,
                    slotActivo = elegidas.keys.minByOrNull { s -> SLOTS_EDITOR.indexOf(s) } ?: it.slotActivo,
                    cargando = false,
                )
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            _estado.update { it.copy(cargando = false, error = "No se pudo cargar el outfit") }
        }
    }

    fun cambiarNombre(nombre: String) = _estado.update { it.copy(nombre = nombre) }

    fun cambiarOcasion(ocasion: Occasion?) = _estado.update { it.copy(ocasion = ocasion) }

    fun seleccionarSlot(slot: Slot) = _estado.update { it.copy(slotActivo = slot) }

    /** Elige [prenda] para su slot (y lo activa); volver a tocar la elegida la quita. Vestido y top/bottom se excluyen. */
    fun alternarPrenda(prenda: Garment) = _estado.update { e ->
        val slot = slotDe(prenda.type)
        if (e.elegidas[slot]?.prenda?.id == prenda.id) return@update e.copy(elegidas = e.elegidas - slot)
        val incompatibles = when (slot) {
            Slot.DRESS -> setOf(Slot.TOP, Slot.BOTTOM)
            Slot.TOP, Slot.BOTTOM -> setOf(Slot.DRESS)
            else -> emptySet()
        }
        e.copy(elegidas = e.elegidas - incompatibles + (slot to PrendaElegida(prenda)), slotActivo = slot)
    }

    /** Quita todas las prendas del maniquí. */
    fun limpiar() = _estado.update { it.copy(elegidas = emptyMap()) }

    /** Quita la prenda de [slot]. */
    fun quitar(slot: Slot) = _estado.update { it.copy(elegidas = it.elegidas - slot) }

    fun quitarActiva() = _estado.update { it.copy(elegidas = it.elegidas - it.slotActivo) }

    /** Desplaza la prenda activa [dx], [dy] unidades del lienzo (respetando los límites de su caja). */
    fun mover(dx: Float, dy: Float) = ajustarActiva { it.copy(dx = it.dx + dx, dy = it.dy + dy) }

    /** Multiplica la escala de la prenda activa por [factor]. */
    fun escalarPor(factor: Float) = ajustarActiva { it.copy(escala = it.escala * factor) }

    fun fijarEscala(escala: Float) = ajustarActiva { it.copy(escala = escala) }

    fun restablecerAjuste() = ajustarActiva { AjustePrenda.Ninguno }

    private fun ajustarActiva(cambio: (AjustePrenda) -> AjustePrenda) = _estado.update { e ->
        val actual = e.elegidaActiva ?: return@update e
        val caja = subtipoPorDefecto(actual.prenda.type).caja
        e.copy(elegidas = e.elegidas + (e.slotActivo to actual.copy(ajuste = cambio(actual.ajuste).limitado(caja))))
    }

    fun mostrarError(mensaje: String) = _estado.update { it.copy(error = mensaje) }

    fun descartarError() = _estado.update { it.copy(error = null) }

    /** Guarda el outfit si [EditorOutfitUiState.puedeGuardar]; al terminar rellena `idGuardado`. */
    fun guardar() {
        val e = _estado.value
        if (!e.puedeGuardar) return
        _estado.update { it.copy(guardando = true, error = null) }
        scope.launch {
            try {
                val previo = original
                val id = if (previo != null) {
                    outfits.update(
                        previo.copy(name = e.nombre.trim(), occasion = e.ocasion),
                        e.elegidas.aOutfitGarments(previo.id),
                    )
                    previo.id
                } else {
                    outfits.create(
                        Outfit(0, e.nombre.trim(), e.ocasion, favorite = false, createdAt = ahora()),
                        e.elegidas.aOutfitGarments(),
                    )
                }
                _estado.update { it.copy(guardando = false, idGuardado = id) }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                _estado.update { it.copy(guardando = false, error = "No se pudo guardar el outfit") }
            }
        }
    }
}

/** Prendas del outfit listas para guardar: posición y escala del ajuste, y `zIndex` = z del slot. */
fun Map<Slot, PrendaElegida>.aOutfitGarments(outfitId: Long = 0): List<OutfitGarment> =
    entries.sortedBy { it.key.z }.map { (slot, e) ->
        OutfitGarment(outfitId, e.prenda.id, e.ajuste.dx, e.ajuste.dy, e.ajuste.escala, slot.z)
    }
