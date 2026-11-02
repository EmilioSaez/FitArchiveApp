package app.luxion.fitarchiveapp.data

import app.luxion.fitarchiveapp.backup.BackupService
import app.luxion.fitarchiveapp.backup.PhotoStore
import app.luxion.fitarchiveapp.backup.TripGarmentLink
import app.luxion.fitarchiveapp.backup.TripOutfitLink
import app.luxion.fitarchiveapp.backup.WardrobeSnapshot
import app.luxion.fitarchiveapp.model.CalendarDay
import app.luxion.fitarchiveapp.model.Fit
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Measurements
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Trip
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.model.WearRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private fun garment(id: Long, path: String, brand: String? = null) = Garment(
    id, path, GarmentType.DRESS, GarmentColor.BURGUNDY, Season.WINTER, Usage.PARTY, brand, "M",
    favorite = true, dirty = true, wearsSinceWash = 2, totalWears = 9, washAfterWears = 3, createdAt = 1000L + id, fit = Fit.OVERSIZE,
)

private fun sample() = WardrobeSnapshot(
    garments = listOf(garment(1, "f/1.jpg", "Mango ñ \"é\""), garment(5, "f/5.jpg").copy(brand = null, size = null, washAfterWears = null, favorite = false, dirty = false)),
    outfits = listOf(Outfit(10, "Cena", Occasion.DATE, true, 5), Outfit(11, "Sin ocasión", null, false, 6)),
    outfitGarments = listOf(OutfitGarment(10, 1, 0.25f, -3.5f, 1.5f, 2), OutfitGarment(10, 5, 0f, 0f, 1f, 0)),
    measurements = Measurements(165f, 90f, null, 95.5f, 40f, 58f, 100f, 777, footCm = 24.5f),
    calendar = listOf(CalendarDay("2026-11-02", 10, "Boda"), CalendarDay("2026-11-03", null, null)),
    trips = listOf(Trip(20, "Roma", "Italia", "2026-12-01", "2026-12-05", 9), Trip(21, "Sin fechas", null, null, null, 10)),
    tripOutfits = listOf(TripOutfitLink(20, 10)),
    tripGarments = listOf(TripGarmentLink(20, 1, true), TripGarmentLink(20, 5, false)),
    wears = listOf(WearRecord("2026-11-02", 1), WearRecord("2026-11-02", 5)),
)

/** Orden estable para comparar sin depender del orden de las consultas. */
private fun WardrobeSnapshot.sorted() = copy(
    garments = garments.sortedBy { it.id },
    outfits = outfits.sortedBy { it.id },
    outfitGarments = outfitGarments.sortedWith(compareBy({ it.outfitId }, { it.garmentId })),
    calendar = calendar.sortedBy { it.date },
    trips = trips.sortedBy { it.id },
    tripOutfits = tripOutfits.sortedWith(compareBy({ it.tripId }, { it.outfitId })),
    tripGarments = tripGarments.sortedWith(compareBy({ it.tripId }, { it.garmentId })),
    wears = wears.sortedWith(compareBy({ it.date }, { it.garmentId })),
)

private class MemoryPhotos(val files: MutableMap<String, ByteArray> = mutableMapOf()) : PhotoStore {
    override suspend fun read(path: String) = files[path]
    override suspend fun save(garmentId: Long, bytes: ByteArray) = "new/$garmentId.jpg".also { files[it] = bytes }
    override suspend fun delete(path: String) { files.remove(path) }
}

class SqlWardrobeBackupStoreTest {
    private val db = createTestDatabase()
    private val store = SqlWardrobeBackupStore(db)

    @Test
    fun emptyDatabaseReadsEmptySnapshot() = runTest {
        assertEquals(WardrobeSnapshot(), store.readAll())
    }

    @Test
    fun replaceThenReadKeepsEverythingAndIds() = runTest {
        store.replaceAll(sample())
        assertEquals(sample().sorted(), store.readAll().sorted())
    }

    @Test
    fun replaceDiscardsPreviousContent() = runTest {
        store.replaceAll(sample())
        val other = WardrobeSnapshot(garments = listOf(garment(99, "x.jpg")))
        store.replaceAll(other)
        assertEquals(other, store.readAll())
        store.replaceAll(WardrobeSnapshot())
        assertEquals(WardrobeSnapshot(), store.readAll())
    }

    @Test
    fun failedReplaceRollsBackEverything() = runTest {
        store.replaceAll(sample())
        // Referencia a una prenda inexistente: viola la clave foránea al final de la inserción.
        val broken = sample().copy(outfitGarments = listOf(OutfitGarment(10, 404, 0f, 0f, 1f, 0)))
        assertFailsWith<Exception> { store.replaceAll(broken) }
        assertEquals(sample().sorted(), store.readAll().sorted())

        val duplicated = WardrobeSnapshot(garments = listOf(garment(1, "a"), garment(1, "b")))
        assertFailsWith<Exception> { store.replaceAll(duplicated) }
        assertEquals(sample().sorted(), store.readAll().sorted())
    }

    @Test
    fun repositoriesKeepWorkingAfterRestoreAndNewIdsDoNotCollide() = runTest {
        store.replaceAll(sample())
        val repos = Repositories(db)
        val newId = repos.garments.add(garment(0, "n.jpg"))
        assertEquals(6L, newId)
        assertEquals(sample().outfits.map { it.id }.toSet(), repos.outfits.observeAll().first().map { it.id }.toSet())
        assertNull(repos.garments.get(2))
    }

    @Test
    fun serviceExportImportRoundTripWithRealDatabase() = runTest {
        store.replaceAll(sample())
        val photos = MemoryPhotos(mutableMapOf("f/1.jpg" to byteArrayOf(1, 2, 3), "f/5.jpg" to ByteArray(200) { it.toByte() }))
        val file = BackupService(store, photos) { 1L }.export()

        val destDb = createTestDatabase()
        val destStore = SqlWardrobeBackupStore(destDb)
        val destPhotos = MemoryPhotos()
        val result = BackupService(destStore, destPhotos) { 1L }.import(file)

        assertEquals(emptyList(), result.missingPhotos)
        val expected = sample().let { s -> s.copy(garments = s.garments.map { it.copy(photoPath = "new/${it.id}.jpg") }) }
        assertEquals(expected.sorted(), destStore.readAll().sorted())
        assertContentEquals(byteArrayOf(1, 2, 3), destPhotos.files.getValue("new/1.jpg"))
        assertEquals(200, destPhotos.files.getValue("new/5.jpg").size)
    }
}
