package app.luxion.fitarchiveapp.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.luxion.fitarchiveapp.db.FitArchiveDatabase
import app.luxion.fitarchiveapp.model.Trip
import app.luxion.fitarchiveapp.model.TripGarment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

interface TripRepository {
    /** Viajes con fecha de inicio más reciente primero; los que no tienen fecha, al final. */
    fun observeAll(): Flow<List<Trip>>
    fun observe(id: Long): Flow<Trip?>
    suspend fun get(id: Long): Trip?

    /** Inserta el viaje (se ignora `trip.id`) y devuelve el id nuevo. */
    suspend fun create(trip: Trip): Long
    suspend fun update(trip: Trip)

    /** Elimina el viaje junto con sus outfits y lista de ropa. */
    suspend fun delete(id: Long)

    fun observeOutfitIds(tripId: Long): Flow<List<Long>>
    suspend fun addOutfit(tripId: Long, outfitId: Long)
    suspend fun removeOutfit(tripId: Long, outfitId: Long)

    /** Lista de ropa de la maleta. */
    fun observePacking(tripId: Long): Flow<List<TripGarment>>
    suspend fun addGarment(tripId: Long, garmentId: Long)
    suspend fun setPacked(tripId: Long, garmentId: Long, packed: Boolean)
    suspend fun removeGarment(tripId: Long, garmentId: Long)
}

internal class SqlTripRepository(
    private val db: FitArchiveDatabase,
    private val context: CoroutineContext = Dispatchers.Default,
) : TripRepository {
    private val queries = db.tripQueries

    override fun observeAll() = queries.selectAll().asFlow().mapToList(context).map { it.map { t -> t.toModel() } }

    override fun observe(id: Long) = queries.selectById(id).asFlow().mapToOneOrNull(context).map { it?.toModel() }

    override suspend fun get(id: Long) = withContext(context) { queries.selectById(id).executeAsOneOrNull()?.toModel() }

    override suspend fun create(trip: Trip): Long = withContext(context) {
        db.transactionWithResult {
            queries.insert(trip.name, trip.destination, trip.startDate, trip.endDate, trip.createdAt)
            queries.lastInsertId().executeAsOne()
        }
    }

    override suspend fun update(trip: Trip) = withContext(context) {
        queries.update(trip.name, trip.destination, trip.startDate, trip.endDate, trip.id)
        Unit
    }

    override suspend fun delete(id: Long) = withContext(context) { queries.deleteById(id); Unit }

    override fun observeOutfitIds(tripId: Long) = queries.selectOutfitIds(tripId).asFlow().mapToList(context)

    override suspend fun addOutfit(tripId: Long, outfitId: Long) =
        withContext(context) { queries.insertOutfit(tripId, outfitId); Unit }

    override suspend fun removeOutfit(tripId: Long, outfitId: Long) =
        withContext(context) { queries.deleteOutfit(tripId, outfitId); Unit }

    override fun observePacking(tripId: Long) =
        queries.selectPacking(tripId).asFlow().mapToList(context).map { it.map { g -> g.toModel() } }

    override suspend fun addGarment(tripId: Long, garmentId: Long) =
        withContext(context) { queries.insertGarment(tripId, garmentId); Unit }

    override suspend fun setPacked(tripId: Long, garmentId: Long, packed: Boolean) =
        withContext(context) { queries.setPacked(packed.toDb(), tripId, garmentId); Unit }

    override suspend fun removeGarment(tripId: Long, garmentId: Long) =
        withContext(context) { queries.deleteGarment(tripId, garmentId); Unit }
}
