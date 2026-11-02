package app.luxion.fitarchiveapp.data

import app.luxion.fitarchiveapp.backup.TripGarmentLink
import app.luxion.fitarchiveapp.backup.TripOutfitLink
import app.luxion.fitarchiveapp.backup.WardrobeBackupStore
import app.luxion.fitarchiveapp.backup.WardrobeSnapshot
import app.luxion.fitarchiveapp.db.FitArchiveDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** [WardrobeBackupStore] sobre la BD SQLDelight: lectura completa y reemplazo transaccional con ids. */
class SqlWardrobeBackupStore(
    private val db: FitArchiveDatabase,
    private val context: CoroutineContext = Dispatchers.Default,
) : WardrobeBackupStore {

    override suspend fun readAll(): WardrobeSnapshot = withContext(context) {
        // Una sola transacción para que la lectura sea coherente.
        db.transactionWithResult {
            WardrobeSnapshot(
                garments = db.garmentQueries.selectAll().executeAsList().map { it.toModel() },
                outfits = db.outfitQueries.selectAll().executeAsList().map { it.toModel() },
                outfitGarments = db.outfitQueries.selectAllGarments().executeAsList().map { it.toModel() },
                measurements = db.measurementsQueries.select().executeAsOneOrNull()?.toModel(),
                calendar = db.calendarDayQueries.selectAll().executeAsList().map { it.toModel() },
                trips = db.tripQueries.selectAll().executeAsList().map { it.toModel() },
                tripOutfits = db.tripQueries.selectAllOutfits().executeAsList()
                    .map { TripOutfitLink(it.trip_id, it.outfit_id) },
                tripGarments = db.tripQueries.selectAllPacking().executeAsList()
                    .map { TripGarmentLink(it.trip_id, it.garment_id, it.packed == 1L) },
            )
        }
    }

    override suspend fun replaceAll(snapshot: WardrobeSnapshot) = withContext(context) {
        db.transaction {
            // Hijos antes que padres, para no depender de ON DELETE CASCADE.
            db.tripQueries.deleteAllGarments()
            db.tripQueries.deleteAllOutfits()
            db.tripQueries.deleteAll()
            db.calendarDayQueries.deleteAll()
            db.outfitQueries.deleteAllGarments()
            db.outfitQueries.deleteAll()
            db.garmentQueries.deleteAll()
            db.measurementsQueries.delete()

            snapshot.garments.forEach {
                db.garmentQueries.insertWithId(
                    id = it.id, photo_path = it.photoPath, type = it.type.name, color = it.color.name,
                    season = it.season.name, usage = it.usage.name, brand = it.brand, size = it.size,
                    fit = it.fit.name,
                    favorite = it.favorite.toDb(), dirty = it.dirty.toDb(),
                    wears_since_wash = it.wearsSinceWash.toLong(), total_wears = it.totalWears.toLong(),
                    wash_after_wears = it.washAfterWears?.toLong(), created_at = it.createdAt,
                )
            }
            snapshot.outfits.forEach {
                db.outfitQueries.insertWithId(it.id, it.name, it.occasion?.name, it.favorite.toDb(), it.createdAt)
            }
            snapshot.outfitGarments.forEach {
                db.outfitQueries.insertGarment(
                    it.outfitId, it.garmentId, it.x.toDouble(), it.y.toDouble(), it.scale.toDouble(), it.zIndex.toLong(),
                )
            }
            snapshot.measurements?.let {
                db.measurementsQueries.upsert(
                    it.heightCm?.toDouble(), it.bustCm?.toDouble(), it.waistCm?.toDouble(), it.hipCm?.toDouble(),
                    it.shouldersCm?.toDouble(), it.armCm?.toDouble(), it.legCm?.toDouble(), it.updatedAt,
                )
            }
            snapshot.calendar.forEach { db.calendarDayQueries.upsert(it.date, it.outfitId, it.note) }
            snapshot.trips.forEach {
                db.tripQueries.insertWithId(it.id, it.name, it.destination, it.startDate, it.endDate, it.createdAt)
            }
            snapshot.tripOutfits.forEach { db.tripQueries.insertOutfit(it.tripId, it.outfitId) }
            snapshot.tripGarments.forEach {
                db.tripQueries.insertGarmentWithState(it.tripId, it.garmentId, it.packed.toDb())
            }
        }
    }
}
