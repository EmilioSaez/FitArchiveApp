package app.luxion.fitarchiveapp.ui.calendario

import app.luxion.fitarchiveapp.data.CalendarRepository
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.data.OutfitWithGarments
import app.luxion.fitarchiveapp.model.CalendarDay
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class CalendarioEnMemoria : CalendarRepository {
    val datos = MutableStateFlow<Map<String, CalendarDay>>(emptyMap())
    var falla = false

    override fun observe(date: String): Flow<CalendarDay?> = datos.map { it[date] }
    override fun observeBetween(from: String, to: String): Flow<List<CalendarDay>> =
        datos.map { m -> m.values.filter { it.date in from..to }.sortedBy { it.date } }

    override suspend fun set(day: CalendarDay) {
        if (falla) error("db")
        datos.value += day.date to day
    }

    override suspend fun clear(date: String) {
        if (falla) error("db")
        datos.value -= date
    }
}

private class OutfitsFijos(lista: List<Outfit>) : OutfitRepository {
    private val datos = MutableStateFlow(lista)
    override fun observeAll(): Flow<List<Outfit>> = datos
    override fun observeFavorites(): Flow<List<Outfit>> = datos.map { l -> l.filter { it.favorite } }
    override fun observe(id: Long): Flow<OutfitWithGarments?> = datos.map { l -> l.firstOrNull { it.id == id }?.let { OutfitWithGarments(it, emptyList()) } }
    override suspend fun get(id: Long) = datos.value.firstOrNull { it.id == id }?.let { OutfitWithGarments(it, emptyList()) }
    override suspend fun create(outfit: Outfit, garments: List<OutfitGarment>) = error("no usado")
    override suspend fun update(outfit: Outfit, garments: List<OutfitGarment>) = error("no usado")
    override suspend fun setFavorite(id: Long, favorite: Boolean) = error("no usado")
    override suspend fun delete(id: Long) = error("no usado")
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarioViewModelTest {
    private val casual = Outfit(1, "Casual", Occasion.CASUAL, favorite = false, createdAt = 1)
    private val oficina = Outfit(2, "Oficina", Occasion.WORK, favorite = false, createdAt = 2)

    private class Entorno(val repo: CalendarioEnMemoria, val vm: CalendarioViewModel)

    private fun entorno(scope: CoroutineScope, hoy: String = "2026-11-02"): Entorno {
        val repo = CalendarioEnMemoria()
        return Entorno(repo, CalendarioViewModel(repo, OutfitsFijos(listOf(oficina, casual)), scope, hoy))
    }

    @Test
    fun empiezaEnElMesDeHoyConLosOutfitsDisponibles() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val s = e.vm.estado.value
        assertEquals(MesCalendario(2026, 11), s.mes)
        assertEquals("2026-11-02", s.hoy)
        assertEquals(listOf("Oficina", "Casual"), s.outfits.map { it.name })
        assertNull(s.seleccionado)
    }

    @Test
    fun asignarOutfitAUnDiaLoMuestraYLoReemplaza() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.vm.seleccionar("2026-11-10")
        e.vm.asignarOutfit(casual)
        assertEquals(casual, e.vm.estado.value.outfitDe("2026-11-10"))

        e.vm.asignarOutfit(oficina)
        assertEquals(oficina, e.vm.estado.value.outfitDe("2026-11-10"))
        assertEquals(1, e.repo.datos.value.size)
    }

    @Test
    fun asignarConservaLaNota() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.vm.seleccionar("2026-11-10")
        e.vm.guardarNota("  cena  ")
        e.vm.asignarOutfit(casual)
        assertEquals(CalendarDay("2026-11-10", 1, "cena"), e.repo.datos.value["2026-11-10"])
    }

    @Test
    fun sinDiaAbiertoLasAccionesNoHacenNada() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.vm.asignarOutfit(casual)
        e.vm.guardarNota("x")
        e.vm.quitarOutfit()
        assertTrue(e.repo.datos.value.isEmpty())
    }

    @Test
    fun quitarOutfitBorraElDiaSiNoTieneNota() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.vm.seleccionar("2026-11-10")
        e.vm.asignarOutfit(casual)
        e.vm.quitarOutfit()
        assertTrue(e.repo.datos.value.isEmpty())
        assertNull(e.vm.estado.value.outfitDe("2026-11-10"))
    }

    @Test
    fun quitarOutfitConservaLaNota() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.vm.seleccionar("2026-11-10")
        e.vm.asignarOutfit(casual)
        e.vm.guardarNota("cena")
        e.vm.quitarOutfit()
        assertEquals(CalendarDay("2026-11-10", null, "cena"), e.repo.datos.value["2026-11-10"])
    }

    @Test
    fun notaVaciaSinOutfitBorraElDia() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.vm.seleccionar("2026-11-10")
        e.vm.guardarNota("cena")
        assertEquals("cena", e.vm.estado.value.diaSeleccionado?.note)
        e.vm.guardarNota("   ")
        assertTrue(e.repo.datos.value.isEmpty())
    }

    @Test
    fun soloSeVenLosDiasDelMesVisible() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.repo.set(CalendarDay("2026-11-05", 1, null))
        e.repo.set(CalendarDay("2026-12-05", 2, null))
        assertEquals(setOf("2026-11-05"), e.vm.estado.value.dias.keys)

        e.vm.mesSiguiente()
        assertEquals(MesCalendario(2026, 12), e.vm.estado.value.mes)
        assertEquals(setOf("2026-12-05"), e.vm.estado.value.dias.keys)

        e.vm.irAHoy()
        assertEquals(MesCalendario(2026, 11), e.vm.estado.value.mes)
        assertEquals(setOf("2026-11-05"), e.vm.estado.value.dias.keys)
    }

    @Test
    fun cambiarDeMesCierraElDia() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.vm.seleccionar("2026-11-10")
        e.vm.mesAnterior()
        assertNull(e.vm.estado.value.seleccionado)
        assertEquals(MesCalendario(2026, 10), e.vm.estado.value.mes)
    }

    @Test
    fun errorAlGuardarSeMuestraYSeDescarta() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.repo.falla = true
        e.vm.seleccionar("2026-11-10")
        e.vm.asignarOutfit(casual)
        assertNotNull(e.vm.estado.value.error)
        assertTrue(e.repo.datos.value.isEmpty())
        e.vm.descartarError()
        assertNull(e.vm.estado.value.error)
    }

    @Test
    fun diaConOutfitEliminadoNoMuestraOutfit() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.repo.set(CalendarDay("2026-11-05", 99, null))
        assertNull(e.vm.estado.value.outfitDe("2026-11-05"))
    }
}
