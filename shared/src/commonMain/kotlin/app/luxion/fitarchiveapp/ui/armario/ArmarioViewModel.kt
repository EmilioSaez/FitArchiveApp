package app.luxion.fitarchiveapp.ui.armario

import app.luxion.fitarchiveapp.data.GarmentFilter
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** Familias de prendas que se muestran como pestañas; cada una agrupa varios [GarmentType]. */
enum class PestanaTipo(val titulo: String, val tipos: Set<GarmentType>) {
    Camisetas("Camisetas", setOf(GarmentType.TSHIRT, GarmentType.SHIRT)),
    Punto("Jerséis", setOf(GarmentType.SWEATER)),
    Abrigos("Abrigos", setOf(GarmentType.JACKET, GarmentType.COAT)),
    Pantalones("Pantalones", setOf(GarmentType.TROUSERS, GarmentType.JEANS, GarmentType.SHORTS)),
    Faldas("Faldas", setOf(GarmentType.SKIRT)),
    Vestidos("Vestidos", setOf(GarmentType.DRESS, GarmentType.JUMPSUIT)),
    Calzado("Calzado", setOf(GarmentType.SHOES)),
    Accesorios("Accesorios", setOf(GarmentType.BAG, GarmentType.ACCESSORY)),
}

/** Cómo se agrupa el armario en pestañas. T11 añadirá `PorColor` y `PorEstacion` aquí. */
sealed interface Agrupacion {
    /** Pestañas de la agrupación, en orden. */
    val pestanas: List<Pestana>

    data object PorTipo : Agrupacion {
        override val pestanas: List<Pestana> = PestanaTipo.entries.map { Pestana(it.name, it.titulo, it.tipos) }
    }
}

/** Una pestaña: [tipos] es el criterio de pertenencia (T11 añadirá colores/estaciones). */
data class Pestana(val id: String, val titulo: String, val tipos: Set<GarmentType>) {
    fun admite(prenda: Garment) = prenda.type in tipos
}

data class ArmarioUiState(
    val cargando: Boolean = true,
    val pestanas: List<Pestana> = emptyList(),
    val pestanaActual: Int = 0,
    /** Prendas de la pestaña actual. */
    val prendas: List<Garment> = emptyList(),
    /** Nº de prendas por pestaña (misma posición que [pestanas]). */
    val conteos: List<Int> = emptyList(),
    /** Total de prendas del armario, sin importar la pestaña. */
    val total: Int = 0,
) {
    /** Armario sin ninguna prenda (distinto de una pestaña vacía). */
    val armarioVacio get() = !cargando && total == 0
}

/**
 * Estado de la pantalla del armario. Observa el Flow del repositorio y lo reparte en pestañas.
 * T12 (filtro) cambia [filtro]; T14 (favoritas) añade su propia agrupación/criterio sin tocar la pantalla.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ArmarioViewModel(
    repositorio: GarmentRepository,
    scope: CoroutineScope,
    agrupacion: Agrupacion = Agrupacion.PorTipo,
) {
    private val pestanaActual = MutableStateFlow(0)
    private val filtro = MutableStateFlow(GarmentFilter())
    private val pestanas = agrupacion.pestanas

    val estado: StateFlow<ArmarioUiState> = filtro
        .flatMapLatest { repositorio.observeFiltered(it) }
        .combine(pestanaActual) { prendas, indice ->
            val actual = indice.coerceIn(0, pestanas.lastIndex)
            ArmarioUiState(
                cargando = false,
                pestanas = pestanas,
                pestanaActual = actual,
                prendas = prendas.filter { pestanas[actual].admite(it) },
                conteos = pestanas.map { p -> prendas.count { p.admite(it) } },
                total = prendas.size,
            )
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), ArmarioUiState(pestanas = pestanas))

    fun seleccionarPestana(indice: Int) {
        pestanaActual.value = indice
    }

    fun aplicarFiltro(nuevo: GarmentFilter) {
        filtro.value = nuevo
    }
}
