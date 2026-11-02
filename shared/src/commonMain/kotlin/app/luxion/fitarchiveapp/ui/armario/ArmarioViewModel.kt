package app.luxion.fitarchiveapp.ui.armario

import app.luxion.fitarchiveapp.data.GarmentFilter
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.ui.anadir.etiqueta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

/** Cómo se agrupa el armario en pestañas. */
sealed interface Agrupacion {
    /** Nombre en el selector de agrupación. */
    val titulo: String

    /** Pestañas de la agrupación, en orden. */
    val pestanas: List<Pestana>

    data object PorTipo : Agrupacion {
        override val titulo = "Tipo"
        override val pestanas: List<Pestana> =
            PestanaTipo.entries.map { p -> Pestana(p.name, p.titulo) { it.type in p.tipos } } +
                Pestana("FAVORITAS", "Favoritas") { it.favorite }
    }

    data object PorColor : Agrupacion {
        override val titulo = "Color"
        override val pestanas: List<Pestana> =
            GarmentColor.entries.map { c -> Pestana(c.name, c.etiqueta()) { it.color == c } }
    }

    data object PorEstacion : Agrupacion {
        override val titulo = "Estación"
        override val pestanas: List<Pestana> =
            Season.entries.map { s -> Pestana(s.name, s.etiqueta()) { it.season == s } }
    }

    data object PorUso : Agrupacion {
        override val titulo = "Uso"
        override val pestanas: List<Pestana> =
            Usage.entries.map { u -> Pestana(u.name, u.etiqueta()) { it.usage == u } }
    }

    companion object {
        val todas: List<Agrupacion> = listOf(PorTipo, PorColor, PorEstacion, PorUso)
    }
}

/** Una pestaña: [admite] es su criterio de pertenencia. */
class Pestana(val id: String, val titulo: String, val admite: (Garment) -> Boolean) {
    fun admite(prenda: Garment) = admite.invoke(prenda)
    override fun equals(other: Any?) = other is Pestana && other.id == id
    override fun hashCode() = id.hashCode()
}

data class ArmarioUiState(
    val cargando: Boolean = true,
    val agrupacion: Agrupacion = Agrupacion.PorTipo,
    val pestanas: List<Pestana> = emptyList(),
    val pestanaActual: Int = 0,
    /** Prendas de la pestaña actual (ya filtradas). */
    val prendas: List<Garment> = emptyList(),
    /** Nº de prendas por pestaña (misma posición que [pestanas]), con el filtro aplicado. */
    val conteos: List<Int> = emptyList(),
    /** Prendas que pasan el filtro, sin importar la pestaña. */
    val total: Int = 0,
    /** Prendas del armario sin filtrar. */
    val totalArmario: Int = 0,
    val filtro: GarmentFilter = GarmentFilter(),
) {
    /** Armario sin ninguna prenda (distinto de un filtro sin resultados o una pestaña vacía). */
    val armarioVacio get() = !cargando && totalArmario == 0

    /** Hay algún criterio de filtro activo. */
    val hayFiltro get() = filtro.numeroCriterios() > 0

    /** El filtro deja el armario sin ninguna prenda. */
    val filtroSinResultados get() = !cargando && totalArmario > 0 && total == 0
}

/** Nº de criterios del filtro que el usuario puede activar (type, color, season, usage, favorite). */
fun GarmentFilter.numeroCriterios() =
    listOf(type, color, season, usage).count { it != null } + if (favorite == true) 1 else 0

/**
 * Estado de la pantalla del armario. Observa el Flow del repositorio y lo reparte en pestañas.
 * T11: la agrupación (tipo/color/estación) es seleccionable. T12: [aplicarFiltro] combina criterios.
 * T14 (favoritas) puede añadir su propia agrupación/criterio sin tocar la pantalla.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ArmarioViewModel(
    private val repositorio: GarmentRepository,
    private val scope: CoroutineScope,
    agrupacionInicial: Agrupacion = Agrupacion.PorTipo,
) {
    private val agrupacion = MutableStateFlow(agrupacionInicial)
    private val pestanaActual = MutableStateFlow(0)
    private val filtro = MutableStateFlow(GarmentFilter())

    val estado: StateFlow<ArmarioUiState> = combine(
        filtro.flatMapLatest { repositorio.observeFiltered(it) },
        repositorio.observeAll(),
        agrupacion,
        pestanaActual,
        filtro,
    ) { prendas, todas, grupo, indice, criterio ->
        val pestanas = grupo.pestanas
        val actual = indice.coerceIn(0, pestanas.lastIndex)
        ArmarioUiState(
            cargando = false,
            agrupacion = grupo,
            pestanas = pestanas,
            pestanaActual = actual,
            prendas = prendas.filter { pestanas[actual].admite(it) },
            conteos = pestanas.map { p -> prendas.count { p.admite(it) } },
            total = prendas.size,
            totalArmario = todas.size,
            filtro = criterio,
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ArmarioUiState(pestanas = agrupacionInicial.pestanas))

    fun seleccionarPestana(indice: Int) {
        pestanaActual.value = indice
    }

    /** Cambia la agrupación y vuelve a la primera pestaña. */
    fun seleccionarAgrupacion(nueva: Agrupacion) {
        agrupacion.value = nueva
        pestanaActual.value = 0
    }

    /** Alterna el like de la prenda; el Flow del repositorio refresca la cuadrícula al instante. */
    fun alternarFavorita(prenda: Garment) {
        scope.launch { repositorio.setFavorite(prenda.id, !prenda.favorite) }
    }

    fun aplicarFiltro(nuevo: GarmentFilter) {
        filtro.value = nuevo
    }

    fun limpiarFiltro() = aplicarFiltro(GarmentFilter())
}
