package app.luxion.fitarchiveapp.ui.outfit

import app.luxion.fitarchiveapp.avatar.AjustePrenda
import app.luxion.fitarchiveapp.avatar.Rect
import app.luxion.fitarchiveapp.avatar.Slot
import app.luxion.fitarchiveapp.avatar.ajustar
import app.luxion.fitarchiveapp.avatar.ajusteGuardado
import app.luxion.fitarchiveapp.avatar.subtipoPorDefecto
import app.luxion.fitarchiveapp.data.GarmentRepositoryEnMemoria
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.data.OutfitWithGarments
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class OutfitsFalso(var falla: Boolean = false) : OutfitRepository {
    var guardado: Pair<Outfit, List<OutfitGarment>>? = null

    override suspend fun create(outfit: Outfit, garments: List<OutfitGarment>): Long {
        if (falla) error("FK")
        guardado = outfit to garments
        return 7
    }

    override fun observeAll(): Flow<List<Outfit>> = flowOf(emptyList())
    override fun observeFavorites(): Flow<List<Outfit>> = flowOf(emptyList())
    override fun observe(id: Long): Flow<OutfitWithGarments?> = flowOf(null)
    override suspend fun get(id: Long): OutfitWithGarments? = null
    override suspend fun update(outfit: Outfit, garments: List<OutfitGarment>) = Unit
    override suspend fun setFavorite(id: Long, favorite: Boolean) = Unit
    override suspend fun delete(id: Long) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class EditorOutfitViewModelTest {
    private fun prenda(tipo: GarmentType) = Garment(
        id = 0, photoPath = "p.png", type = tipo, color = GarmentColor.BLACK, season = Season.SUMMER,
        usage = Usage.CASUAL, brand = null, size = null, favorite = false, dirty = false,
        wearsSinceWash = 0, totalWears = 0, washAfterWears = null, createdAt = 1,
    )

    private class Entorno(val armario: GarmentRepositoryEnMemoria, val outfits: OutfitsFalso, val vm: EditorOutfitViewModel)

    private fun entorno(scope: kotlinx.coroutines.CoroutineScope, outfits: OutfitsFalso = OutfitsFalso()): Entorno {
        val armario = GarmentRepositoryEnMemoria()
        return Entorno(armario, outfits, EditorOutfitViewModel(outfits, armario, scope) { 1234L })
    }

    private suspend fun Entorno.nueva(tipo: GarmentType): Garment {
        val id = armario.add(prenda(tipo))
        return armario.get(id)!!
    }

    @Test
    fun candidatasSonLasPrendasDelSlotActivo() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.nueva(GarmentType.TSHIRT)
        e.nueva(GarmentType.JEANS)
        assertEquals(listOf(GarmentType.TSHIRT), e.vm.estado.value.candidatas.map { it.type })
        e.vm.seleccionarSlot(Slot.BOTTOM)
        assertEquals(listOf(GarmentType.JEANS), e.vm.estado.value.candidatas.map { it.type })
    }

    @Test
    fun elegirReemplazaEnElSlotYTocarDeNuevoLaQuita() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val a = e.nueva(GarmentType.TSHIRT)
        val b = e.nueva(GarmentType.SHIRT)
        e.vm.alternarPrenda(a)
        e.vm.alternarPrenda(b)
        assertEquals(b.id, e.vm.estado.value.elegidas.getValue(Slot.TOP).prenda.id)
        e.vm.alternarPrenda(b)
        assertTrue(e.vm.estado.value.elegidas.isEmpty())
    }

    @Test
    fun vestidoExcluyeTopYBottom() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val top = e.nueva(GarmentType.TSHIRT)
        val bottom = e.nueva(GarmentType.SKIRT)
        val vestido = e.nueva(GarmentType.DRESS)
        val abrigo = e.nueva(GarmentType.COAT)
        e.vm.alternarPrenda(top); e.vm.alternarPrenda(bottom); e.vm.alternarPrenda(abrigo)
        e.vm.alternarPrenda(vestido)
        assertEquals(setOf(Slot.DRESS, Slot.OUTER), e.vm.estado.value.elegidas.keys)
        e.vm.alternarPrenda(top)
        assertEquals(setOf(Slot.TOP, Slot.OUTER), e.vm.estado.value.elegidas.keys)
    }

    @Test
    fun ajusteSeLimitaALaCajaYLaEscalaSiempreEsAutomatica() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val camiseta = e.nueva(GarmentType.TSHIRT)
        e.vm.alternarPrenda(camiseta)
        val caja = subtipoPorDefecto(GarmentType.TSHIRT).caja

        e.vm.mover(10_000f, -10_000f)
        var ajuste = e.vm.estado.value.elegidaActiva!!.ajuste
        assertEquals(caja.w / 2f, ajuste.dx)
        assertEquals(-caja.h / 2f, ajuste.dy)
        assertEquals(AjustePrenda.ESCALA_AUTOMATICA, ajuste.escala)

        e.vm.restablecerAjuste()
        ajuste = e.vm.estado.value.elegidaActiva!!.ajuste
        assertEquals(AjustePrenda.Ninguno, ajuste)
    }

    @Test
    fun ajustarSinPrendaActivaNoHaceNada() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.vm.mover(5f, 5f)
        assertTrue(e.vm.estado.value.elegidas.isEmpty())
    }

    @Test
    fun puedeGuardarExigeNombreYPrendas() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        assertFalse(e.vm.estado.value.puedeGuardar)
        e.vm.alternarPrenda(e.nueva(GarmentType.TSHIRT))
        assertFalse(e.vm.estado.value.puedeGuardar)
        e.vm.cambiarNombre("   ")
        assertFalse(e.vm.estado.value.puedeGuardar)
        e.vm.cambiarNombre("Look")
        assertTrue(e.vm.estado.value.puedeGuardar)
    }

    @Test
    fun mostrarErrorYDescartarlo() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.vm.mostrarError("No se pudo compartir el outfit")
        assertEquals("No se pudo compartir el outfit", e.vm.estado.value.error)
        e.vm.descartarError()
        assertEquals(null, e.vm.estado.value.error)
    }

    @Test
    fun guardarPersistePosicionEscalaAutomaticaYCapaPorPrenda() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val camiseta = e.nueva(GarmentType.TSHIRT)
        val zapatos = e.nueva(GarmentType.SHOES)
        e.vm.alternarPrenda(camiseta)
        e.vm.mover(12f, -8f)
        e.vm.alternarPrenda(zapatos)
        e.vm.cambiarNombre("  Look de oficina ")
        e.vm.cambiarOcasion(Occasion.WORK)

        e.vm.guardar()

        val (outfit, prendas) = assertNotNull(e.outfits.guardado)
        assertEquals("Look de oficina", outfit.name)
        assertEquals(Occasion.WORK, outfit.occasion)
        assertEquals(1234L, outfit.createdAt)
        assertEquals(7L, e.vm.estado.value.idGuardado)
        assertEquals(listOf(zapatos.id, camiseta.id), prendas.map { it.garmentId }) // por z: calzado (20) < top (40)
        val top = prendas.last()
        assertEquals(12f, top.x)
        assertEquals(-8f, top.y)
        assertEquals(AjustePrenda.ESCALA_AUTOMATICA, top.scale)
        assertEquals(Slot.TOP.z, top.zIndex)
        assertFalse(e.vm.estado.value.puedeGuardar)
    }

    @Test
    fun errorAlGuardarSeMuestraYPermiteReintentar() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope, OutfitsFalso(falla = true))
        e.vm.alternarPrenda(e.nueva(GarmentType.TSHIRT))
        e.vm.cambiarNombre("Look")
        e.vm.guardar()
        assertNotNull(e.vm.estado.value.error)
        assertNull(e.vm.estado.value.idGuardado)
        assertTrue(e.vm.estado.value.puedeGuardar)

        e.outfits.falla = false
        e.vm.guardar()
        assertNull(e.vm.estado.value.error)
        assertEquals(7L, e.vm.estado.value.idGuardado)
    }

    @Test
    fun prendaEliminadaDelArmarioSaleDelOutfit() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val camiseta = e.nueva(GarmentType.TSHIRT)
        e.vm.alternarPrenda(camiseta)
        e.armario.delete(camiseta.id)
        assertTrue(e.vm.estado.first { it.armario.isEmpty() }.elegidas.isEmpty())
    }

    @Test
    fun laEscalaGuardadaALaManoSeIgnora() {
        val guardada = OutfitGarment(1, 2, 5f, -3f, 1.4f, 40)
        assertEquals(AjustePrenda(5f, -3f, AjustePrenda.ESCALA_AUTOMATICA), guardada.ajusteGuardado())
    }

    @Test
    fun ajustarEscalaDesdeElCentroYDesplaza() {
        val base = Rect(100f, 200f, 80f, 120f)
        assertEquals(base, ajustar(base, AjustePrenda.Ninguno))
        val r = ajustar(base, AjustePrenda(dx = 10f, dy = -5f, escala = 0.5f))
        assertEquals(Rect(130f, 225f, 40f, 60f), r) // centro (140,260)+(10,-5)=(150,255); 40x60
    }

    @Test
    fun ajusteLimitadoMantieneElCentroEnLaCaja() {
        val caja = subtipoPorDefecto(GarmentType.SHOES).caja
        val a = AjustePrenda(dx = -1000f, dy = 1000f, escala = 0.1f).limitado(caja)
        assertEquals(-caja.w / 2f, a.dx)
        assertEquals(caja.h / 2f, a.dy)
        assertEquals(AjustePrenda.ESCALA_MIN, a.escala)
        assertEquals(AjustePrenda(3f, 4f, 1f), AjustePrenda(3f, 4f, 1f).limitado(caja))
    }
}
