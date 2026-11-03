package app.luxion.fitarchiveapp.data

import app.luxion.fitarchiveapp.model.CalendarDay
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import app.luxion.fitarchiveapp.model.SOLO_MANUAL
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UsoRepositoryTest {
    private val repos = Repositories(createTestDatabase())

    private suspend fun prenda(washAfterWears: Int? = SOLO_MANUAL, type: GarmentType = GarmentType.TSHIRT) = repos.garments.add(
        Garment(
            id = 0, photoPath = "p.png", type = type, color = GarmentColor.BLACK, season = Season.SUMMER,
            usage = Usage.CASUAL, brand = null, size = null, favorite = false, dirty = false, wearsSinceWash = 0,
            totalWears = 0, washAfterWears = washAfterWears, createdAt = 1,
        ),
    )

    private suspend fun outfitCon(vararg prendas: Long) = repos.outfits.create(
        Outfit(id = 0, name = "Look", occasion = null, favorite = false, createdAt = 1),
        prendas.mapIndexed { i, g -> OutfitGarment(0, g, 0f, 0f, 1f, i) },
    )

    @Test
    fun pastDayCountsOneWearPerGarmentAndIsLogged() = runTest {
        val a = prenda()
        val b = prenda()
        repos.calendar.set(CalendarDay("2026-11-01", outfitCon(a, b), null))

        assertEquals(2, repos.usos.registrarHasta("2026-11-02"))

        assertEquals(1, repos.garments.get(a)!!.totalWears)
        assertEquals(1, repos.garments.get(b)!!.wearsSinceWash)
        assertEquals(listOf("2026-11-01"), repos.usos.observeFechas(a).first())
    }

    @Test
    fun manyPendingDaysOfOneGarmentAreAppliedAtOnceWithTheWashRule() = runTest {
        val vaqueros = prenda(washAfterWears = null, type = GarmentType.JEANS) // se ensucian a los 3 usos
        val o = outfitCon(vaqueros)
        listOf("2026-10-28", "2026-10-29", "2026-10-30", "2026-10-31").forEach { repos.calendar.set(CalendarDay(it, o, null)) }

        assertEquals(4, repos.usos.registrarHasta("2026-11-02"))

        val g = repos.garments.get(vaqueros)!!
        assertEquals(4, g.totalWears)
        assertEquals(4, g.wearsSinceWash)
        assertTrue(g.dirty)
    }

    @Test
    fun todayAndFutureDaysDoNotCountYet() = runTest {
        val a = prenda()
        val o = outfitCon(a)
        repos.calendar.set(CalendarDay("2026-11-02", o, null))
        repos.calendar.set(CalendarDay("2026-11-03", o, null))

        assertEquals(0, repos.usos.registrarHasta("2026-11-02"))
        assertEquals(0, repos.garments.get(a)!!.totalWears)
        // Al día siguiente ya ha pasado el 2.
        assertEquals(1, repos.usos.registrarHasta("2026-11-03"))
        assertEquals(listOf("2026-11-02"), repos.usos.observeFechas(a).first())
    }

    @Test
    fun runningTwiceDoesNotCountTwice() = runTest {
        val a = prenda()
        repos.calendar.set(CalendarDay("2026-11-01", outfitCon(a), null))

        repos.usos.registrarHasta("2026-11-05")
        assertEquals(0, repos.usos.registrarHasta("2026-11-05"))
        assertEquals(0, repos.usos.registrarHasta("2026-11-09"))

        assertEquals(1, repos.garments.get(a)!!.totalWears)
    }

    @Test
    fun historyListsMostRecentFirstAndIgnoresOtherGarments() = runTest {
        val a = prenda()
        val b = prenda()
        repos.calendar.set(CalendarDay("2026-10-30", outfitCon(a), null))
        repos.calendar.set(CalendarDay("2026-11-01", outfitCon(a, b), null))

        repos.usos.registrarHasta("2026-11-02")

        assertEquals(listOf("2026-11-01", "2026-10-30"), repos.usos.observeFechas(a).first())
        assertEquals(listOf("2026-11-01"), repos.usos.observeFechas(b).first())
        assertEquals(2, repos.garments.get(a)!!.totalWears)
    }

    @Test
    fun daysWithoutOutfitOrWithEmptyOutfitCountNothing() = runTest {
        val a = prenda()
        repos.calendar.set(CalendarDay("2026-11-01", null, "descanso"))
        repos.calendar.set(CalendarDay("2026-11-02", outfitCon(), null))

        assertEquals(0, repos.usos.registrarHasta("2026-11-10"))
        assertEquals(0, repos.garments.get(a)!!.totalWears)
    }

    @Test
    fun reachingWashThresholdMarksGarmentDirty() = runTest {
        val a = prenda(washAfterWears = 2)
        val o = outfitCon(a)
        repos.calendar.set(CalendarDay("2026-11-01", o, null))
        repos.usos.registrarHasta("2026-11-02")
        assertFalse(repos.garments.get(a)!!.dirty)

        repos.calendar.set(CalendarDay("2026-11-02", o, null))
        repos.usos.registrarHasta("2026-11-03")
        assertTrue(repos.garments.get(a)!!.dirty)
    }

    @Test
    fun deletingGarmentRemovesItsHistory() = runTest {
        val a = prenda()
        repos.calendar.set(CalendarDay("2026-11-01", outfitCon(a), null))
        repos.usos.registrarHasta("2026-11-02")

        repos.garments.delete(a)

        assertEquals(emptyList(), repos.usos.observeFechas(a).first())
    }

    @Test
    fun garmentWithoutOwnThresholdUsesTypeDefault() = runTest {
        val camiseta = prenda(washAfterWears = null)
        val abrigo = prenda(washAfterWears = null, type = GarmentType.COAT)
        val o = outfitCon(camiseta, abrigo)
        repos.calendar.set(CalendarDay("2026-11-01", o, null))
        repos.usos.registrarHasta("2026-11-02")

        assertTrue(repos.garments.get(camiseta)!!.dirty)
        assertFalse(repos.garments.get(abrigo)!!.dirty)
    }

    @Test
    fun ownThresholdOverridesTypeDefault() = runTest {
        val a = prenda(washAfterWears = 2)
        val o = outfitCon(a)
        repos.calendar.set(CalendarDay("2026-11-01", o, null))
        repos.usos.registrarHasta("2026-11-02")
        assertFalse(repos.garments.get(a)!!.dirty)
    }

    @Test
    fun todosDevuelveElHistorialCompleto() = runTest {
        val a = prenda()
        val b = prenda()
        repos.calendar.set(CalendarDay("2026-11-01", outfitCon(a, b), null))
        repos.usos.registrarHasta("2026-11-02")

        assertEquals(
            listOf(app.luxion.fitarchiveapp.model.WearRecord("2026-11-01", a), app.luxion.fitarchiveapp.model.WearRecord("2026-11-01", b)),
            repos.usos.todos(),
        )
    }
}
