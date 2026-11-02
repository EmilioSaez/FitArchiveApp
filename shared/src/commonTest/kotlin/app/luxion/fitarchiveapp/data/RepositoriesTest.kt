package app.luxion.fitarchiveapp.data

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
import app.luxion.fitarchiveapp.model.TripGarment
import app.luxion.fitarchiveapp.model.Usage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoriesTest {
    private var clock = 1_000L
    private val repos = Repositories(createTestDatabase(), now = { clock })

    private fun garment(
        type: GarmentType = GarmentType.TSHIRT,
        color: GarmentColor = GarmentColor.BLACK,
        season: Season = Season.SUMMER,
        usage: Usage = Usage.CASUAL,
        createdAt: Long = 1L,
        washAfterWears: Int? = null,
    ) = Garment(
        id = 0, photoPath = "p.png", type = type, color = color, season = season, usage = usage,
        brand = "Zara", size = "M", favorite = false, dirty = false, wearsSinceWash = 0, totalWears = 0,
        washAfterWears = washAfterWears, createdAt = createdAt,
    )

    private fun outfit(name: String = "Look", createdAt: Long = 1L) =
        Outfit(id = 0, name = name, occasion = Occasion.DATE, favorite = false, createdAt = createdAt)

    // --- Prendas ---

    @Test
    fun garmentRoundTripKeepsEnumsAndNulls() = runTest {
        val original = garment(GarmentType.JUMPSUIT, GarmentColor.BURGUNDY, Season.MID_SEASON, Usage.GOING_OUT, washAfterWears = 3)
            .copy(brand = null, size = null)
        val id = repos.garments.add(original)
        assertEquals(original.copy(id = id), repos.garments.get(id))
        assertNull(repos.garments.get(id + 1))
    }

    @Test
    fun garmentsAreOrderedNewestFirst() = runTest {
        val old = repos.garments.add(garment(createdAt = 1))
        val new = repos.garments.add(garment(createdAt = 2))
        assertEquals(listOf(new, old), repos.garments.observeAll().first().map { it.id })
    }

    @Test
    fun garmentFilterCombinesCriteria() = runTest {
        val a = repos.garments.add(garment(GarmentType.TSHIRT, GarmentColor.BLACK, Season.SUMMER))
        val b = repos.garments.add(garment(GarmentType.TSHIRT, GarmentColor.WHITE, Season.SUMMER))
        repos.garments.add(garment(GarmentType.JEANS, GarmentColor.BLACK, Season.WINTER))
        suspend fun ids(f: GarmentFilter) = repos.garments.observeFiltered(f).first().map { it.id }.sorted()
        assertEquals(3, ids(GarmentFilter()).size)
        assertEquals(listOf(a, b), ids(GarmentFilter(type = GarmentType.TSHIRT)))
        assertEquals(listOf(a), ids(GarmentFilter(type = GarmentType.TSHIRT, color = GarmentColor.BLACK)))
        assertTrue(ids(GarmentFilter(season = Season.MID_SEASON)).isEmpty())
    }

    @Test
    fun favoritesAndUpdate() = runTest {
        val id = repos.garments.add(garment())
        repos.garments.setFavorite(id, true)
        assertEquals(listOf(id), repos.garments.observeFavorites().first().map { it.id })
        assertEquals(listOf(id), repos.garments.observeFiltered(GarmentFilter(favorite = true)).first().map { it.id })
        val edited = repos.garments.get(id)!!.copy(brand = "Mango", color = GarmentColor.NAVY)
        repos.garments.update(edited)
        assertEquals(edited, repos.garments.observe(id).first())
        repos.garments.setFavorite(id, false)
        assertTrue(repos.garments.observeFavorites().first().isEmpty())
    }

    @Test
    fun wearsMakeGarmentDirtyAfterThreshold() = runTest {
        val id = repos.garments.add(garment(washAfterWears = 2))
        repos.garments.registerWear(id)
        assertFalse(repos.garments.get(id)!!.dirty)
        repos.garments.registerWear(id)
        val dirty = repos.garments.get(id)!!
        assertTrue(dirty.dirty)
        assertEquals(2, dirty.wearsSinceWash)
        assertEquals(2, dirty.totalWears)
        repos.garments.setDirty(id, false)
        val clean = repos.garments.get(id)!!
        assertFalse(clean.dirty)
        assertEquals(0, clean.wearsSinceWash)
        assertEquals(2, clean.totalWears)
    }

    @Test
    fun garmentWithoutThresholdOnlyDirtyManually() = runTest {
        val id = repos.garments.add(garment())
        repeat(10) { repos.garments.registerWear(id) }
        assertFalse(repos.garments.get(id)!!.dirty)
        repos.garments.setDirty(id, true)
        assertTrue(repos.garments.get(id)!!.dirty)
    }

    // --- Outfits ---

    @Test
    fun outfitRoundTripWithGarmentsOrderedByLayer() = runTest {
        val g1 = repos.garments.add(garment())
        val g2 = repos.garments.add(garment(GarmentType.JEANS))
        val id = repos.outfits.create(
            outfit(),
            listOf(OutfitGarment(0, g1, 1f, 2f, 1.5f, 5), OutfitGarment(0, g2, 3f, 4f, 1f, 1)),
        )
        val loaded = repos.outfits.get(id)!!
        assertEquals(outfit().copy(id = id), loaded.outfit)
        assertEquals(
            listOf(OutfitGarment(id, g2, 3f, 4f, 1f, 1), OutfitGarment(id, g1, 1f, 2f, 1.5f, 5)),
            loaded.garments,
        )
        assertEquals(loaded, repos.outfits.observe(id).first())
        assertNull(repos.outfits.get(id + 1))
    }

    @Test
    fun outfitUpdateReplacesGarments() = runTest {
        val g1 = repos.garments.add(garment())
        val g2 = repos.garments.add(garment())
        val id = repos.outfits.create(outfit(), listOf(OutfitGarment(0, g1, 0f, 0f, 1f, 0)))
        repos.outfits.update(outfit("Nuevo").copy(id = id, occasion = null), listOf(OutfitGarment(0, g2, 0f, 0f, 1f, 0)))
        val loaded = repos.outfits.get(id)!!
        assertEquals("Nuevo", loaded.outfit.name)
        assertNull(loaded.outfit.occasion)
        assertEquals(listOf(g2), loaded.garments.map { it.garmentId })
    }

    @Test
    fun outfitFavoritesAndOrder() = runTest {
        val a = repos.outfits.create(outfit("A", 1), emptyList())
        val b = repos.outfits.create(outfit("B", 2), emptyList())
        assertEquals(listOf(b, a), repos.outfits.observeAll().first().map { it.id })
        repos.outfits.setFavorite(a, true)
        assertEquals(listOf(a), repos.outfits.observeFavorites().first().map { it.id })
    }

    @Test
    fun deletingGarmentRemovesItFromOutfits() = runTest {
        val g = repos.garments.add(garment())
        val id = repos.outfits.create(outfit(), listOf(OutfitGarment(0, g, 0f, 0f, 1f, 0)))
        repos.garments.delete(g)
        assertTrue(repos.outfits.get(id)!!.garments.isEmpty())
    }

    @Test
    fun deletingGarmentRemovesItFromTripPackingAndKeepsTheTrip() = runTest {
        val trip = repos.trips.create(Trip(0, "Viaje", null, null, null, 1))
        val g = repos.garments.add(garment())
        val otra = repos.garments.add(garment())
        repos.trips.addGarment(trip, g)
        repos.trips.addGarment(trip, otra)
        repos.garments.delete(g)
        assertEquals(listOf(otra), repos.trips.observePacking(trip).first().map { it.garmentId })
        assertNull(repos.garments.get(g))
        assertEquals(trip, repos.trips.get(trip)?.id)
    }

    // --- Medidas ---

    @Test
    fun measurementsSaveStampsUpdatedAtAndReplaces() = runTest {
        assertNull(repos.measurements.get())
        assertNull(repos.measurements.observe().first())
        val m = Measurements(165f, 90f, 70f, 95f, null, null, 80f, updatedAt = 0)
        clock = 5_000
        repos.measurements.save(m)
        assertEquals(m.copy(updatedAt = 5_000), repos.measurements.get())
        clock = 6_000
        repos.measurements.save(m.copy(waistCm = 72f, heightCm = null))
        val saved = repos.measurements.observe().first()!!
        assertEquals(72f, saved.waistCm)
        assertNull(saved.heightCm)
        assertEquals(6_000, saved.updatedAt)
        repos.measurements.clear()
        assertNull(repos.measurements.get())
    }

    // --- Calendario ---

    @Test
    fun calendarSetReplaceRangeAndClear() = runTest {
        val o = repos.outfits.create(outfit(), emptyList())
        repos.calendar.set(CalendarDay("2026-11-02", o, "cena"))
        repos.calendar.set(CalendarDay("2026-11-10", null, "descanso"))
        repos.calendar.set(CalendarDay("2026-12-01", o, null))
        assertEquals(CalendarDay("2026-11-02", o, "cena"), repos.calendar.observe("2026-11-02").first())
        repos.calendar.set(CalendarDay("2026-11-02", o, "comida"))
        assertEquals(
            listOf("2026-11-02", "2026-11-10"),
            repos.calendar.observeBetween("2026-11-01", "2026-11-30").first().map { it.date },
        )
        assertEquals("comida", repos.calendar.observe("2026-11-02").first()!!.note)
        repos.calendar.clear("2026-11-02")
        assertNull(repos.calendar.observe("2026-11-02").first())
    }

    @Test
    fun deletingOutfitKeepsCalendarDayWithoutOutfit() = runTest {
        val o = repos.outfits.create(outfit(), emptyList())
        repos.calendar.set(CalendarDay("2026-11-02", o, "nota"))
        repos.outfits.delete(o)
        assertEquals(CalendarDay("2026-11-02", null, "nota"), repos.calendar.observe("2026-11-02").first())
    }

    // --- Viajes ---

    @Test
    fun tripRoundTripAndOrder() = runTest {
        val noDate = repos.trips.create(Trip(0, "Sin fecha", null, null, null, 1))
        val early = repos.trips.create(Trip(0, "Enero", "Roma", "2027-01-05", "2027-01-10", 2))
        val late = repos.trips.create(Trip(0, "Marzo", null, "2027-03-01", null, 3))
        assertEquals(listOf(late, early, noDate), repos.trips.observeAll().first().map { it.id })
        assertEquals(Trip(early, "Enero", "Roma", "2027-01-05", "2027-01-10", 2), repos.trips.get(early))
        repos.trips.update(repos.trips.get(early)!!.copy(destination = "París"))
        assertEquals("París", repos.trips.observe(early).first()!!.destination)
    }

    @Test
    fun tripOutfitsAndPacking() = runTest {
        val trip = repos.trips.create(Trip(0, "Viaje", null, null, null, 1))
        val o = repos.outfits.create(outfit(), emptyList())
        val g = repos.garments.add(garment())
        repos.trips.addOutfit(trip, o)
        repos.trips.addOutfit(trip, o)
        assertEquals(listOf(o), repos.trips.observeOutfitIds(trip).first())
        repos.trips.addGarment(trip, g)
        assertEquals(listOf(TripGarment(trip, g, false)), repos.trips.observePacking(trip).first())
        repos.trips.setPacked(trip, g, true)
        assertEquals(listOf(TripGarment(trip, g, true)), repos.trips.observePacking(trip).first())
        repos.trips.removeGarment(trip, g)
        repos.trips.removeOutfit(trip, o)
        assertTrue(repos.trips.observePacking(trip).first().isEmpty())
        assertTrue(repos.trips.observeOutfitIds(trip).first().isEmpty())
    }

    @Test
    fun deletingTripCascades() = runTest {
        val trip = repos.trips.create(Trip(0, "Viaje", null, null, null, 1))
        val g = repos.garments.add(garment())
        repos.trips.addGarment(trip, g)
        repos.trips.delete(trip)
        assertNull(repos.trips.get(trip))
        assertTrue(repos.trips.observePacking(trip).first().isEmpty())
        assertEquals(1, repos.garments.observeAll().first().size)
    }
}
