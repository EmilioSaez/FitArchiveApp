package app.luxion.fitarchiveapp.ui.outfit

import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Prenda de un outfit guardado junto con su colocación en el lienzo. */
data class PrendaEnOutfit(val prenda: Garment, val colocacion: OutfitGarment)

/** Outfit guardado listo para mostrar en la lista, con las prendas que aún existen en el armario. */
data class OutfitEnLista(val outfit: Outfit, val prendas: List<PrendaEnOutfit>)

data class ListaOutfitsUiState(
    val cargando: Boolean = true,
    val outfits: List<OutfitEnLista> = emptyList(),
    /** Outfit pendiente de confirmar su eliminación. */
    val porEliminar: Outfit? = null,
    val error: String? = null,
) {
    val vacio get() = !cargando && outfits.isEmpty()
}

/** Estado de "Mis outfits" (T19): lista observada, favorito y eliminación con confirmación. */
class ListaOutfitsViewModel(
    private val outfits: OutfitRepository,
    armario: GarmentRepository,
    private val scope: CoroutineScope,
) {
    private val _estado = MutableStateFlow(ListaOutfitsUiState())
    val estado: StateFlow<ListaOutfitsUiState> = _estado.asStateFlow()

    init {
        scope.launch {
            combine(outfits.observeAllConPrendas(), armario.observeAll()) { guardados, prendas ->
                val porId = prendas.associateBy { it.id }
                guardados.map { g ->
                    OutfitEnLista(
                        g.outfit,
                        g.garments.mapNotNull { og -> porId[og.garmentId]?.let { PrendaEnOutfit(it, og) } },
                    )
                }
            }.collect { lista -> _estado.update { it.copy(cargando = false, outfits = lista) } }
        }
    }

    fun alternarFavorito(outfit: Outfit) = accion("No se pudo actualizar el favorito") {
        outfits.setFavorite(outfit.id, !outfit.favorite)
    }

    fun pedirEliminar(outfit: Outfit) = _estado.update { it.copy(porEliminar = outfit) }

    fun cancelarEliminar() = _estado.update { it.copy(porEliminar = null) }

    fun confirmarEliminar() {
        val outfit = _estado.value.porEliminar ?: return
        _estado.update { it.copy(porEliminar = null) }
        accion("No se pudo eliminar el outfit") { outfits.delete(outfit.id) }
    }

    fun mostrarError(mensaje: String) = _estado.update { it.copy(error = mensaje) }

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
