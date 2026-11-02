package app.luxion.fitarchiveapp.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.luxion.fitarchiveapp.db.FitArchiveDatabase
import app.luxion.fitarchiveapp.db.GarmentQueries
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.model.sucioTrasUso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Criterios opcionales del filtro del armario; null = no filtrar por ese criterio. */
data class GarmentFilter(
    val type: GarmentType? = null,
    val color: GarmentColor? = null,
    val season: Season? = null,
    val usage: Usage? = null,
    val favorite: Boolean? = null,
    val dirty: Boolean? = null,
)

interface GarmentRepository {
    /** Todas las prendas, las más recientes primero. */
    fun observeAll(): Flow<List<Garment>>
    fun observeFavorites(): Flow<List<Garment>>
    fun observeFiltered(filter: GarmentFilter): Flow<List<Garment>>
    fun observe(id: Long): Flow<Garment?>
    suspend fun get(id: Long): Garment?

    /** Inserta la prenda (se ignora `garment.id`) y devuelve el id nuevo. */
    suspend fun add(garment: Garment): Long
    suspend fun update(garment: Garment)
    suspend fun setFavorite(id: Long, favorite: Boolean)

    /** Al marcar limpia se reinicia el contador de usos desde el lavado. */
    suspend fun setDirty(id: Long, dirty: Boolean)

    /** Suma un uso; si alcanza `usosParaLavar` (propio o del tipo) la prenda pasa a sucia. */
    suspend fun registerWear(id: Long)

    /** Elimina la prenda y, en cascada, sus apariciones en outfits y maletas. */
    suspend fun delete(id: Long)
}

internal class SqlGarmentRepository(
    private val db: FitArchiveDatabase,
    private val context: CoroutineContext = Dispatchers.Default,
) : GarmentRepository {
    private val queries = db.garmentQueries

    override fun observeAll() = queries.selectAll().asFlow().mapToList(context).map { it.map { g -> g.toModel() } }

    override fun observeFavorites() =
        queries.selectFavorites().asFlow().mapToList(context).map { it.map { g -> g.toModel() } }

    override fun observeFiltered(filter: GarmentFilter) = queries.selectFiltered(
        type = filter.type?.name,
        color = filter.color?.name,
        season = filter.season?.name,
        usage = filter.usage?.name,
        favorite = filter.favorite?.toDb(),
        dirty = filter.dirty?.toDb(),
    ).asFlow().mapToList(context).map { it.map { g -> g.toModel() } }

    override fun observe(id: Long) =
        queries.selectById(id).asFlow().mapToOneOrNull(context).map { it?.toModel() }

    override suspend fun get(id: Long) = withContext(context) { queries.selectById(id).executeAsOneOrNull()?.toModel() }

    override suspend fun add(garment: Garment): Long = withContext(context) {
        db.transactionWithResult {
            queries.insert(
                photo_path = garment.photoPath,
                type = garment.type.name,
                color = garment.color.name,
                season = garment.season.name,
                usage = garment.usage.name,
                brand = garment.brand,
                size = garment.size,
                fit = garment.fit.name,
                favorite = garment.favorite.toDb(),
                dirty = garment.dirty.toDb(),
                wears_since_wash = garment.wearsSinceWash.toLong(),
                total_wears = garment.totalWears.toLong(),
                wash_after_wears = garment.washAfterWears?.toLong(),
                created_at = garment.createdAt,
            )
            queries.lastInsertId().executeAsOne()
        }
    }

    override suspend fun update(garment: Garment) = withContext(context) {
        queries.update(
            photo_path = garment.photoPath,
            type = garment.type.name,
            color = garment.color.name,
            season = garment.season.name,
            usage = garment.usage.name,
            brand = garment.brand,
            size = garment.size,
            fit = garment.fit.name,
            favorite = garment.favorite.toDb(),
            dirty = garment.dirty.toDb(),
            wears_since_wash = garment.wearsSinceWash.toLong(),
            total_wears = garment.totalWears.toLong(),
            wash_after_wears = garment.washAfterWears?.toLong(),
            id = garment.id,
        )
        Unit
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) =
        withContext(context) { queries.setFavorite(favorite.toDb(), id); Unit }

    override suspend fun setDirty(id: Long, dirty: Boolean) =
        withContext(context) { queries.setDirty(dirty = dirty.toDb(), id = id); Unit }

    override suspend fun registerWear(id: Long) = withContext(context) {
        db.transaction { queries.registrarUso(id) }
    }

    override suspend fun delete(id: Long) = withContext(context) { queries.deleteById(id); Unit }
}

/** Suma un uso a la prenda y la marca sucia si toca (T35). Debe llamarse dentro de una transacción. */
internal fun GarmentQueries.registrarUso(id: Long) {
    val prenda = selectById(id).executeAsOneOrNull()?.toModel() ?: return
    registerWear(dirty = prenda.sucioTrasUso().toDb(), id = id)
}
