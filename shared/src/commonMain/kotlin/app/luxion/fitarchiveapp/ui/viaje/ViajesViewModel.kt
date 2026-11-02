package app.luxion.fitarchiveapp.ui.viaje

import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.data.OutfitWithGarments
import app.luxion.fitarchiveapp.data.TripRepository
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.Trip
import app.luxion.fitarchiveapp.viaje.Maleta
import app.luxion.fitarchiveapp.viaje.ResultadoViaje
import app.luxion.fitarchiveapp.viaje.construirMaleta
import app.luxion.fitarchiveapp.viaje.validarViaje
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ViajesUiState(
    val cargando: Boolean = true,
    val viajes: List<Trip> = emptyList(),
    /** Outfits guardados, para elegir los del viaje. */
    val outfits: List<Outfit> = emptyList(),
    /** Armario completo, para añadir prendas sueltas a la maleta. */
    val prendas: List<Garment> = emptyList(),
    /** Viaje abierto (detalle y lista de la maleta), o null. */
    val abierto: Trip? = null,
    val outfitsDelViaje: Set<Long> = emptySet(),
    val maleta: Maleta = Maleta(emptyList()),
    /** Viaje pendiente de confirmar su eliminación. */
    val porEliminar: Trip? = null,
    val error: String? = null,
) {
    val vacio get() = !cargando && viajes.isEmpty()

    /** Prendas del armario que aún no están en la maleta. */
    val prendasDisponibles: List<Garment> get() = prendas.filter { p -> maleta.items.none { it.prenda.id == p.id } }
}

/** Estado de la maleta de viaje (T37): crear viajes, elegir outfits y prendas y marcar lo ya guardado. */
@OptIn(ExperimentalCoroutinesApi::class)
class ViajesViewModel(
    private val viajes: TripRepository,
    outfits: OutfitRepository,
    armario: GarmentRepository,
    private val scope: CoroutineScope,
    private val ahora: () -> Long,
) {
    private val _estado = MutableStateFlow(ViajesUiState())
    val estado: StateFlow<ViajesUiState> = _estado.asStateFlow()

    init {
        scope.launch {
            viajes.observeAll().collect { lista ->
                _estado.update { s ->
                    // Si el viaje abierto se elimina o cambia, la vista se mantiene coherente.
                    s.copy(cargando = false, viajes = lista, abierto = s.abierto?.let { a -> lista.firstOrNull { it.id == a.id } })
                }
            }
        }
        scope.launch { outfits.observeAll().collect { lista -> _estado.update { it.copy(outfits = lista) } } }
        scope.launch { armario.observeAll().collect { lista -> _estado.update { it.copy(prendas = lista) } } }
        scope.launch {
            _estado
                .map { it.abierto?.id }
                .distinctUntilChanged()
                .flatMapLatest { id -> if (id == null) flowOf(null) else observarMaleta(id, outfits, armario) }
                .collect { datos ->
                    _estado.update {
                        it.copy(outfitsDelViaje = datos?.first.orEmpty(), maleta = datos?.second ?: Maleta(emptyList()))
                    }
                }
        }
    }

    private fun observarMaleta(
        id: Long,
        outfits: OutfitRepository,
        armario: GarmentRepository,
    ): Flow<Pair<Set<Long>, Maleta>> {
        val deLosOutfits: Flow<Pair<Set<Long>, List<OutfitWithGarments>>> = viajes.observeOutfitIds(id).flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(ids.toSet() to emptyList())
            } else {
                combine(ids.map { oid -> outfits.observe(oid) }) { lista -> ids.toSet() to lista.filterNotNull() }
            }
        }
        return combine(deLosOutfits, viajes.observePacking(id), armario.observeAll()) { (ids, conPrendas), lista, prendas ->
            ids to construirMaleta(conPrendas, lista, prendas)
        }
    }

    fun abrir(viaje: Trip) = _estado.update { it.copy(abierto = viaje) }

    fun cerrar() = _estado.update { it.copy(abierto = null) }

    /**
     * Crea el viaje (o edita el de [id]) con los datos del formulario. Devuelve el mensaje de validación,
     * o null si es válido y se guarda.
     */
    fun guardarViaje(id: Long?, nombre: String, destino: String, inicio: String, fin: String): String? {
        val datos = when (val r = validarViaje(nombre, destino, inicio, fin)) {
            is ResultadoViaje.Invalido -> return r.mensaje
            is ResultadoViaje.Valido -> r.datos
        }
        accion("No se pudo guardar el viaje") {
            if (id == null) {
                val nuevo = Trip(0, datos.nombre, datos.destino, datos.inicio, datos.fin, ahora())
                val creado = viajes.create(nuevo)
                // Tras crearlo se abre para elegir outfits y prendas.
                viajes.get(creado)?.let { t -> _estado.update { it.copy(abierto = t) } }
            } else {
                viajes.get(id)?.let { viajes.update(it.copy(name = datos.nombre, destination = datos.destino, startDate = datos.inicio, endDate = datos.fin)) }
            }
        }
        return null
    }

    fun pedirEliminar(viaje: Trip) = _estado.update { it.copy(porEliminar = viaje) }

    fun cancelarEliminar() = _estado.update { it.copy(porEliminar = null) }

    fun confirmarEliminar() {
        val viaje = _estado.value.porEliminar ?: return
        _estado.update { it.copy(porEliminar = null, abierto = it.abierto?.takeUnless { a -> a.id == viaje.id }) }
        accion("No se pudo eliminar el viaje") { viajes.delete(viaje.id) }
    }

    /** Añade el outfit al viaje o, si ya estaba, lo quita (con las prendas que solo venían de él). */
    fun alternarOutfit(outfit: Outfit) {
        val estado = _estado.value
        val viaje = estado.abierto ?: return
        accion("No se pudo cambiar el outfit del viaje") {
            if (outfit.id !in estado.outfitsDelViaje) {
                viajes.addOutfit(viaje.id, outfit.id)
            } else {
                viajes.removeOutfit(viaje.id, outfit.id)
                // Sus prendas dejan de estar en la lista, salvo las que otro outfit del viaje sigue necesitando.
                val propias = estado.maleta.items.filter { it.outfits == listOf(outfit.name) }
                propias.forEach { viajes.removeGarment(viaje.id, it.prenda.id) }
            }
        }
    }

    fun anadirPrenda(prenda: Garment) {
        val viaje = _estado.value.abierto ?: return
        accion("No se pudo añadir la prenda") { viajes.addGarment(viaje.id, prenda.id) }
    }

    fun quitarPrenda(prenda: Garment) {
        val viaje = _estado.value.abierto ?: return
        accion("No se pudo quitar la prenda") { viajes.removeGarment(viaje.id, prenda.id) }
    }

    /** Marca o desmarca una prenda como ya guardada en la maleta. */
    fun marcarGuardada(prenda: Garment, guardada: Boolean) {
        val viaje = _estado.value.abierto ?: return
        accion("No se pudo actualizar la maleta") {
            // Las prendas que vienen de un outfit aún no tienen fila en la lista: se crea antes de marcarla.
            viajes.addGarment(viaje.id, prenda.id)
            viajes.setPacked(viaje.id, prenda.id, guardada)
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
