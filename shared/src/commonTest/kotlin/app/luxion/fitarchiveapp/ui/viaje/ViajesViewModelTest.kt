package app.luxion.fitarchiveapp.ui.viaje

import app.luxion.fitarchiveapp.data.GarmentRepositoryEnMemoria
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.data.OutfitWithGarments
import app.luxion.fitarchiveapp.data.TripRepository
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Trip
import app.luxion.fitarchiveapp.model.TripGarment
import app.luxion.fitarchiveapp.model.Usage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class ViajesEnMemoria : TripRepository {
    val viajes = MutableStateFlow<List<Trip>>(emptyList())
    val outfits = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())
    val lista = MutableStateFlow<Map<Long, List<TripGarment>>>(emptyMap())
    var falla = false
    private var siguiente = 1L

    private fun comprobar() {
        if (falla) error("db")
    }

    override fun observeAll(): Flow<List<Trip>> = viajes
    override fun observe(id: Long): Flow<Trip?> = viajes.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun get(id: Long) = viajes.value.firstOrNull { it.id == id }

    override suspend fun create(trip: Trip): Long {
        comprobar()
        val id = siguiente++
        viajes.value += trip.copy(id = id)
        return id
    }

    override suspend fun update(trip: Trip) {
        comprobar()
        viajes.value = viajes.value.map { if (it.id == trip.id) trip else it }
    }

    override suspend fun delete(id: Long) {
        comprobar()
        viajes.value = viajes.value.filterNot { it.id == id }
        outfits.value -= id
        lista.value -= id
    }

    override fun observeOutfitIds(tripId: Long): Flow<List<Long>> = outfits.map { it[tripId].orEmpty().sorted() }

    override suspend fun addOutfit(tripId: Long, outfitId: Long) {
        comprobar()
        outfits.value += tripId to (outfits.value[tripId].orEmpty() + outfitId)
    }

    override suspend fun removeOutfit(tripId: Long, outfitId: Long) {
        comprobar()
        outfits.value += tripId to (outfits.value[tripId].orEmpty() - outfitId)
    }

    override fun observePacking(tripId: Long): Flow<List<TripGarment>> = lista.map { it[tripId].orEmpty().sortedBy { g -> g.garmentId } }

    override suspend fun addGarment(tripId: Long, garmentId: Long) {
        comprobar()
        val actual = lista.value[tripId].orEmpty()
        if (actual.none { it.garmentId == garmentId }) lista.value += tripId to (actual + TripGarment(tripId, garmentId, false))
    }

    override suspend fun setPacked(tripId: Long, garmentId: Long, packed: Boolean) {
        comprobar()
        lista.value += tripId to lista.value[tripId].orEmpty().map { if (it.garmentId == garmentId) it.copy(packed = packed) else it }
    }

    override suspend fun removeGarment(tripId: Long, garmentId: Long) {
        comprobar()
        lista.value += tripId to lista.value[tripId].orEmpty().filterNot { it.garmentId == garmentId }
    }
}

private class OutfitsConPrendas(private val datos: List<OutfitWithGarments>) : OutfitRepository {
    override fun observeAll(): Flow<List<Outfit>> = MutableStateFlow(datos.map { it.outfit })
    override fun observeFavorites(): Flow<List<Outfit>> = observeAll()
    override fun observe(id: Long): Flow<OutfitWithGarments?> = MutableStateFlow(datos.firstOrNull { it.outfit.id == id })
    override suspend fun get(id: Long) = datos.firstOrNull { it.outfit.id == id }
    override suspend fun create(outfit: Outfit, garments: List<OutfitGarment>) = error("no usado")
    override suspend fun update(outfit: Outfit, garments: List<OutfitGarment>) = error("no usado")
    override suspend fun setFavorite(id: Long, favorite: Boolean) = error("no usado")
    override suspend fun delete(id: Long) = error("no usado")
}

@OptIn(ExperimentalCoroutinesApi::class)
class ViajesViewModelTest {
    private fun prenda(id: Long, tipo: GarmentType = GarmentType.TSHIRT) = Garment(
        id = id, photoPath = "p$id.png", type = tipo, color = GarmentColor.BLACK, season = Season.SUMMER,
        usage = Usage.CASUAL, brand = null, size = null, favorite = false, dirty = false, wearsSinceWash = 0,
        totalWears = 0, washAfterWears = null, createdAt = id,
    )

    private val camiseta = prenda(1)
    private val vaqueros = prenda(2, GarmentType.JEANS)
    private val zapatos = prenda(3, GarmentType.SHOES)
    private val chaqueta = prenda(4, GarmentType.JACKET)

    private fun outfit(id: Long, nombre: String, vararg prendas: Long) = OutfitWithGarments(
        Outfit(id, nombre, Occasion.CASUAL, favorite = false, createdAt = id),
        prendas.map { OutfitGarment(id, it, 0f, 0f, 1f, 0) },
    )

    private val dia = outfit(10, "Día", 1, 2)
    private val noche = outfit(11, "Noche", 2, 3)

    private class Entorno(val repo: ViajesEnMemoria, val vm: ViajesViewModel)

    private suspend fun entorno(scope: CoroutineScope): Entorno {
        val repo = ViajesEnMemoria()
        val armario = GarmentRepositoryEnMemoria(listOf(camiseta, vaqueros, zapatos, chaqueta))
        return Entorno(repo, ViajesViewModel(repo, OutfitsConPrendas(listOf(dia, noche)), armario, scope) { 100L })
    }

    private suspend fun Entorno.conViajeAbierto(): Trip {
        assertNull(vm.guardarViaje(null, "Lisboa", "", "2026-12-01", "2026-12-05"))
        return assertNotNull(vm.estado.value.abierto)
    }

    @Test
    fun empiezaSinViajes() = runTest(UnconfinedTestDispatcher()) {
        val s = entorno(backgroundScope).vm.estado.value
        assertTrue(s.vacio)
        assertEquals(listOf("Día", "Noche"), s.outfits.map { it.name })
        assertEquals(4, s.prendas.size)
    }

    @Test
    fun crearViajeLoGuardaYLoAbre() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        assertNull(e.vm.guardarViaje(null, " Escapada ", "Lisboa", "2026-12-01", "2026-12-05"))
        val s = e.vm.estado.value
        assertEquals(listOf(Trip(1, "Escapada", "Lisboa", "2026-12-01", "2026-12-05", 100)), s.viajes)
        assertEquals(1L, s.abierto?.id)
    }

    @Test
    fun datosInvalidosDevuelvenMensajeYNoGuardan() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        assertNotNull(e.vm.guardarViaje(null, "", "", "", ""))
        assertNotNull(e.vm.guardarViaje(null, "V", "", "2026-12-05", "2026-12-01"))
        assertTrue(e.vm.estado.value.viajes.isEmpty())
    }

    @Test
    fun editarViajeCambiaSusDatos() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val viaje = e.conViajeAbierto()
        assertNull(e.vm.guardarViaje(viaje.id, "Oporto", "Oporto", "2026-12-02", ""))
        val s = e.vm.estado.value
        assertEquals("Oporto", s.viajes.single().name)
        assertNull(s.viajes.single().endDate)
        assertEquals("Oporto", s.abierto?.name)
    }

    @Test
    fun elegirOutfitsLlenaLaMaletaSinRepetirPrendas() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.conViajeAbierto()
        e.vm.alternarOutfit(dia.outfit)
        e.vm.alternarOutfit(noche.outfit)
        val s = e.vm.estado.value
        assertEquals(setOf(10L, 11L), s.outfitsDelViaje)
        assertEquals(listOf(1L, 2L, 3L), s.maleta.items.map { it.prenda.id })
        assertEquals(listOf(4L), s.prendasDisponibles.map { it.id })
    }

    @Test
    fun quitarUnOutfitConservaLasPrendasQueOtroNecesita() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.conViajeAbierto()
        e.vm.alternarOutfit(dia.outfit)
        e.vm.alternarOutfit(noche.outfit)
        e.vm.alternarOutfit(dia.outfit)
        val s = e.vm.estado.value
        assertEquals(setOf(11L), s.outfitsDelViaje)
        assertEquals(listOf(2L, 3L), s.maleta.items.map { it.prenda.id })
    }

    @Test
    fun anadirYQuitarPrendasSueltas() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.conViajeAbierto()
        e.vm.anadirPrenda(chaqueta)
        assertEquals(listOf(4L), e.vm.estado.value.maleta.items.map { it.prenda.id })
        assertTrue(e.vm.estado.value.maleta.items.single().manual)
        e.vm.quitarPrenda(chaqueta)
        assertTrue(e.vm.estado.value.maleta.items.isEmpty())
    }

    @Test
    fun marcarGuardadaFuncionaTambienEnPrendasDeUnOutfit() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.conViajeAbierto()
        e.vm.alternarOutfit(dia.outfit)
        e.vm.marcarGuardada(camiseta, true)
        var m = e.vm.estado.value.maleta
        assertEquals(1, m.guardadas)
        assertFalse(m.completa)
        e.vm.marcarGuardada(vaqueros, true)
        m = e.vm.estado.value.maleta
        assertTrue(m.completa)
        e.vm.marcarGuardada(camiseta, false)
        assertEquals(1, e.vm.estado.value.maleta.guardadas)
        // Una prenda de outfit marcada no pasa a poderse quitar a mano.
        assertFalse(e.vm.estado.value.maleta.items.first { it.prenda.id == 2L }.manual)
    }

    @Test
    fun eliminarViajeNecesitaConfirmacionYCierraElDetalle() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val viaje = e.conViajeAbierto()
        e.vm.pedirEliminar(viaje)
        assertEquals(1, e.vm.estado.value.viajes.size)
        e.vm.cancelarEliminar()
        assertNull(e.vm.estado.value.porEliminar)
        e.vm.pedirEliminar(viaje)
        e.vm.confirmarEliminar()
        val s = e.vm.estado.value
        assertTrue(s.viajes.isEmpty())
        assertNull(s.abierto)
        assertTrue(s.vacio)
    }

    @Test
    fun cerrarVuelveALaLista() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.conViajeAbierto()
        e.vm.alternarOutfit(dia.outfit)
        e.vm.cerrar()
        val s = e.vm.estado.value
        assertNull(s.abierto)
        assertTrue(s.maleta.items.isEmpty())
    }

    @Test
    fun unFalloDeLaBdMuestraErrorYSePuedeDescartar() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.repo.falla = true
        assertNull(e.vm.guardarViaje(null, "V", "", "", ""))
        assertNotNull(e.vm.estado.value.error)
        e.vm.descartarError()
        assertNull(e.vm.estado.value.error)
    }
}
