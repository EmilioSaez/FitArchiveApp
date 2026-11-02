package app.luxion.fitarchiveapp.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.luxion.fitarchiveapp.db.FitArchiveDatabase
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Outfit con sus prendas colocadas en el lienzo, ordenadas por capa (`zIndex`). */
data class OutfitWithGarments(
    val outfit: Outfit,
    val garments: List<OutfitGarment>,
)

interface OutfitRepository {
    fun observeAll(): Flow<List<Outfit>>
    fun observeFavorites(): Flow<List<Outfit>>

    /** Emite null si el outfit no existe (o se elimina). */
    fun observe(id: Long): Flow<OutfitWithGarments?>
    suspend fun get(id: Long): OutfitWithGarments?

    /** Crea el outfit (se ignora `outfit.id`) con sus prendas y devuelve el id nuevo. */
    suspend fun create(outfit: Outfit, garments: List<OutfitGarment>): Long

    /** Actualiza el outfit y reemplaza todas sus prendas por [garments]. */
    suspend fun update(outfit: Outfit, garments: List<OutfitGarment>)
    suspend fun setFavorite(id: Long, favorite: Boolean)

    /** Elimina el outfit; sus días de calendario quedan sin outfit. */
    suspend fun delete(id: Long)
}

internal class SqlOutfitRepository(
    private val db: FitArchiveDatabase,
    private val context: CoroutineContext = Dispatchers.Default,
) : OutfitRepository {
    private val queries = db.outfitQueries

    override fun observeAll() = queries.selectAll().asFlow().mapToList(context).map { it.map { o -> o.toModel() } }

    override fun observeFavorites() =
        queries.selectFavorites().asFlow().mapToList(context).map { it.map { o -> o.toModel() } }

    override fun observe(id: Long): Flow<OutfitWithGarments?> = combine(
        queries.selectById(id).asFlow().mapToOneOrNull(context),
        queries.selectGarments(id).asFlow().mapToList(context),
    ) { outfit, garments -> outfit?.let { OutfitWithGarments(it.toModel(), garments.map { g -> g.toModel() }) } }

    override suspend fun get(id: Long): OutfitWithGarments? = withContext(context) {
        db.transactionWithResult {
            queries.selectById(id).executeAsOneOrNull()?.let {
                OutfitWithGarments(it.toModel(), queries.selectGarments(id).executeAsList().map { g -> g.toModel() })
            }
        }
    }

    override suspend fun create(outfit: Outfit, garments: List<OutfitGarment>): Long = withContext(context) {
        db.transactionWithResult {
            queries.insert(outfit.name, outfit.occasion?.name, outfit.favorite.toDb(), outfit.createdAt)
            val id = queries.lastInsertId().executeAsOne()
            insertGarments(id, garments)
            id
        }
    }

    override suspend fun update(outfit: Outfit, garments: List<OutfitGarment>) = withContext(context) {
        db.transaction {
            queries.update(outfit.name, outfit.occasion?.name, outfit.favorite.toDb(), outfit.id)
            queries.deleteGarments(outfit.id)
            insertGarments(outfit.id, garments)
        }
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) =
        withContext(context) { queries.setFavorite(favorite.toDb(), id); Unit }

    override suspend fun delete(id: Long) = withContext(context) { queries.deleteById(id); Unit }

    private fun insertGarments(outfitId: Long, garments: List<OutfitGarment>) {
        garments.forEach {
            queries.insertGarment(outfitId, it.garmentId, it.x.toDouble(), it.y.toDouble(), it.scale.toDouble(), it.zIndex.toLong())
        }
    }
}
