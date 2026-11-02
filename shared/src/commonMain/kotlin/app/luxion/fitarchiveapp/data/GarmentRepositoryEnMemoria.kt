package app.luxion.fitarchiveapp.data

import app.luxion.fitarchiveapp.model.Garment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Implementación provisional en memoria hasta que la app cree la BD real (T04 ya la tiene en SQL). */
class GarmentRepositoryEnMemoria(inicial: List<Garment> = emptyList()) : GarmentRepository {
    private val prendas = MutableStateFlow(inicial)

    private fun List<Garment>.recientes() = sortedByDescending { it.createdAt }

    override fun observeAll(): Flow<List<Garment>> = prendas.map { it.recientes() }

    override fun observeFavorites(): Flow<List<Garment>> =
        prendas.map { l -> l.filter { it.favorite }.recientes() }

    override fun observeFiltered(filter: GarmentFilter): Flow<List<Garment>> = prendas.map { l ->
        l.filter {
            (filter.type == null || it.type == filter.type) &&
                (filter.color == null || it.color == filter.color) &&
                (filter.season == null || it.season == filter.season) &&
                (filter.usage == null || it.usage == filter.usage) &&
                (filter.favorite == null || it.favorite == filter.favorite) &&
                (filter.dirty == null || it.dirty == filter.dirty)
        }.recientes()
    }

    override fun observe(id: Long): Flow<Garment?> = prendas.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun get(id: Long) = prendas.value.firstOrNull { it.id == id }

    override suspend fun add(garment: Garment): Long {
        val id = (prendas.value.maxOfOrNull { it.id } ?: 0L) + 1
        prendas.update { it + garment.copy(id = id) }
        return id
    }

    private fun modificar(id: Long, cambio: (Garment) -> Garment) =
        prendas.update { l -> l.map { if (it.id == id) cambio(it) else it } }

    override suspend fun update(garment: Garment) = modificar(garment.id) { garment }
    override suspend fun setFavorite(id: Long, favorite: Boolean) = modificar(id) { it.copy(favorite = favorite) }

    override suspend fun setDirty(id: Long, dirty: Boolean) =
        modificar(id) { it.copy(dirty = dirty, wearsSinceWash = if (dirty) it.wearsSinceWash else 0) }

    override suspend fun registerWear(id: Long) = modificar(id) {
        val usos = it.wearsSinceWash + 1
        val limite = it.washAfterWears
        it.copy(
            wearsSinceWash = usos,
            totalWears = it.totalWears + 1,
            dirty = it.dirty || (limite != null && usos >= limite),
        )
    }

    override suspend fun delete(id: Long) = prendas.update { l -> l.filterNot { it.id == id } }
}
