package app.luxion.fitarchiveapp.backup

import app.luxion.fitarchiveapp.model.CalendarDay
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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

private class FakeStore(var data: WardrobeSnapshot = WardrobeSnapshot(), var failOnReplace: Boolean = false) : WardrobeBackupStore {
    override suspend fun readAll() = data
    override suspend fun replaceAll(snapshot: WardrobeSnapshot) {
        if (failOnReplace) error("fallo de BD")
        data = snapshot
    }
}

private class FakePhotos(val files: MutableMap<String, ByteArray> = mutableMapOf(), val prefix: String = "files/") : PhotoStore {
    override suspend fun read(path: String) = files[path]
    override suspend fun save(garmentId: Long, bytes: ByteArray): String =
        "$prefix$garmentId.jpg".also { files[it] = bytes }
    override suspend fun delete(path: String) { files.remove(path) }
}

private fun garment(id: Long, path: String, brand: String? = null) = Garment(
    id, path, GarmentType.DRESS, GarmentColor.BURGUNDY, Season.WINTER, Usage.PARTY, brand, "M",
    favorite = true, dirty = true, wearsSinceWash = 2, totalWears = 9, washAfterWears = 3, createdAt = 1000L + id,
)

private fun sample() = WardrobeSnapshot(
    garments = listOf(garment(1, "old/1.jpg", "Mango ñ \"é\""), garment(2, "old/2.jpg"), garment(3, "old/gone.jpg")),
    outfits = listOf(Outfit(10, "Cena", Occasion.DATE, true, 5), Outfit(11, "Sin ocasión", null, false, 6)),
    outfitGarments = listOf(OutfitGarment(10, 1, 0.25f, -3.5f, 1.5f, 2), OutfitGarment(10, 2, 0f, 0f, 1f, 0)),
    measurements = Measurements(165f, 90f, null, 95.5f, 40f, 58f, 100f, 777),
    calendar = listOf(CalendarDay("2026-11-02", 10, "Boda"), CalendarDay("2026-11-03", null, null)),
    trips = listOf(Trip(20, "Roma", "Italia", "2026-12-01", "2026-12-05", 9), Trip(21, "Sin fechas", null, null, null, 10)),
    tripOutfits = listOf(TripOutfitLink(20, 10)),
    tripGarments = listOf(TripGarmentLink(20, 1, true), TripGarmentLink(20, 2, false)),
)

class BackupServiceTest {
    private val jpg1 = byteArrayOf(1, 2, 3, -1, 0)
    private val jpg2 = ByteArray(300) { it.toByte() }

    private fun source() = FakePhotos(mutableMapOf("old/1.jpg" to jpg1, "old/2.jpg" to jpg2)) // la 3 no existe

    @Test
    fun idaYVueltaConservaTodosLosDatos() = runTest {
        val data = sample()
        val file = BackupService(FakeStore(data), source()) { 42L }.export()

        val destStore = FakeStore()
        val destPhotos = FakePhotos(prefix = "new/")
        val result = BackupService(destStore, destPhotos) { 0L }.import(file)

        val restored = destStore.data
        assertEquals(data.garments.map { it.copy(photoPath = "") }, restored.garments.map { it.copy(photoPath = "") })
        assertEquals(data.outfits, restored.outfits)
        assertEquals(data.outfitGarments, restored.outfitGarments)
        assertEquals(data.measurements, restored.measurements)
        assertEquals(data.calendar, restored.calendar)
        assertEquals(data.trips, restored.trips)
        assertEquals(data.tripOutfits, restored.tripOutfits)
        assertEquals(data.tripGarments, restored.tripGarments)
        assertEquals(RestoreResult(3, 2, listOf(3L)), result)
    }

    @Test
    fun lasFotosVuelvenIgualesConRutaNueva() = runTest {
        val file = BackupService(FakeStore(sample()), source()) { 1L }.export()
        val store = FakeStore()
        val photos = FakePhotos(prefix = "new/")
        BackupService(store, photos) { 1L }.import(file)

        val byId = store.data.garments.associateBy { it.id }
        assertEquals("new/1.jpg", byId.getValue(1).photoPath)
        assertContentEquals(jpg1, photos.files.getValue("new/1.jpg"))
        assertContentEquals(jpg2, photos.files.getValue("new/2.jpg"))
        assertEquals("", byId.getValue(3).photoPath) // sin foto en la copia
    }

    @Test
    fun copiaVaciaHaceIdaYVuelta() = runTest {
        val file = BackupService(FakeStore(), FakePhotos()) { 1L }.export()
        val store = FakeStore(sample())
        BackupService(store, FakePhotos()) { 1L }.import(file)
        assertEquals(WardrobeSnapshot(), store.data)
    }

    @Test
    fun importarReemplazaYBorraFotosAntiguas() = runTest {
        val file = BackupService(FakeStore(sample()), source()) { 1L }.export()
        val photos = FakePhotos(mutableMapOf("viejo/9.jpg" to byteArrayOf(9)), prefix = "new/")
        val store = FakeStore(WardrobeSnapshot(garments = listOf(garment(9, "viejo/9.jpg"))))
        BackupService(store, photos) { 1L }.import(file)

        assertEquals(listOf(1L, 2L, 3L), store.data.garments.map { it.id })
        assertFalse("viejo/9.jpg" in photos.files)
    }

    @Test
    fun siFallaLaBDNoSePierdeNada() = runTest {
        val file = BackupService(FakeStore(sample()), source()) { 1L }.export()
        val before = WardrobeSnapshot(garments = listOf(garment(9, "viejo/9.jpg")))
        val store = FakeStore(before, failOnReplace = true)
        val photos = FakePhotos(mutableMapOf("viejo/9.jpg" to byteArrayOf(9)), prefix = "new/")

        assertFailsWith<IllegalStateException> { BackupService(store, photos) { 1L }.import(file) }

        assertEquals(before, store.data)
        assertEquals(setOf("viejo/9.jpg"), photos.files.keys) // fotos nuevas retiradas, viejas intactas
    }

    @Test
    fun rechazaArchivoQueNoEsCopia() = runTest {
        val svc = BackupService(FakeStore(), FakePhotos()) { 1L }
        assertFailsWith<BackupException.NotABackup> { svc.import("hola".encodeToByteArray()) }
        assertFailsWith<BackupException.NotABackup> { svc.import(ByteArray(100)) }
    }

    @Test
    fun rechazaArchivoTruncado() = runTest {
        val file = BackupService(FakeStore(sample()), source()) { 1L }.export()
        val store = FakeStore(sample())
        val svc = BackupService(store, FakePhotos()) { 1L }
        for (cut in listOf(file.size - 1, file.size - 100, 20)) {
            assertFailsWith<BackupException.Corrupt> { svc.import(file.copyOf(cut)) }
        }
        assertEquals(sample(), store.data) // nada tocado
    }

    @Test
    fun rechazaVersionMasNueva() = runTest {
        val manifest = BackupManifest(formatVersion = BackupManifest.FORMAT_VERSION, createdAt = 1)
        val file = BackupCodec.encode(manifest, emptyList())
        file[11] = 99 // versión (u32 BE en bytes 8..11)
        val e = assertFailsWith<BackupException.UnsupportedVersion> {
            BackupService(FakeStore(), FakePhotos()) { 1L }.import(file)
        }
        assertEquals(99, e.found)
    }

    @Test
    fun toleraCamposDesconocidosDeVersionesMenores() = runTest {
        val json = """{"formatVersion":1,"createdAt":5,"campoNuevo":{"a":1},"outfits":[{"id":1,"name":"X","createdAt":2,"extra":true}]}"""
        val raw = json.encodeToByteArray()
        val header = BackupCodec.encode(BackupManifest(formatVersion = 1, createdAt = 0), emptyList()).copyOf(16)
        for (i in 0..3) header[12 + i] = (raw.size ushr (24 - 8 * i)).toByte()
        val store = FakeStore()
        BackupService(store, FakePhotos()) { 1L }.import(header + raw)
        assertEquals("X", store.data.outfits.single().name)
    }

    @Test
    fun rechazaReferenciasRotasYEnumsDesconocidos() = runTest {
        val svc = BackupService(FakeStore(), FakePhotos()) { 1L }
        val rotas = BackupManifest(
            formatVersion = 1, createdAt = 1,
            outfitGarments = listOf(OutfitGarmentDto(1, 2)),
        )
        assertFailsWith<BackupException.Corrupt> { svc.import(BackupCodec.encode(rotas, emptyList())) }

        val g = GarmentDto(1, "NOPE", "BLACK", "WINTER", "WORK", createdAt = 1)
        val enumMal = BackupManifest(formatVersion = 1, createdAt = 1, garments = listOf(g))
        assertFailsWith<BackupException.Corrupt> { svc.import(BackupCodec.encode(enumMal, emptyList())) }
    }

    @Test
    fun medidasAusentesSeConservanNulas() = runTest {
        val file = BackupService(FakeStore(WardrobeSnapshot(measurements = null)), FakePhotos()) { 1L }.export()
        val store = FakeStore(sample())
        BackupService(store, FakePhotos()) { 1L }.import(file)
        assertNull(store.data.measurements)
    }
}
