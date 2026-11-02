package app.luxion.fitarchiveapp.ui.outfit

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Repositorio en memoria de outfits para los tests de T19 (mismo orden que SQL: más recientes primero). */
private class OutfitsEnMemoria : OutfitRepository {
    private val datos = MutableStateFlow<List<OutfitWithGarments>>(emptyList())
    private var siguienteId = 1L
    var falla = false

    override fun observeAll(): Flow<List<Outfit>> =
        datos.map { l -> l.map { it.outfit }.sortedWith(compareByDescending<Outfit> { it.createdAt }.thenByDescending { it.id }) }

    override fun observeFavorites(): Flow<List<Outfit>> = observeAll().map { l -> l.filter { it.favorite } }
    override fun observe(id: Long): Flow<OutfitWithGarments?> = datos.map { l -> l.firstOrNull { it.outfit.id == id } }
    override suspend fun get(id: Long) = datos.value.firstOrNull { it.outfit.id == id }

    override suspend fun create(outfit: Outfit, garments: List<OutfitGarment>): Long {
        val id = siguienteId++
        datos.value += OutfitWithGarments(outfit.copy(id = id), garments.map { it.copy(outfitId = id) })
        return id
    }

    override suspend fun update(outfit: Outfit, garments: List<OutfitGarment>) {
        datos.value = datos.value.map { if (it.outfit.id == outfit.id) OutfitWithGarments(outfit, garments) else it }
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        if (falla) error("db")
        datos.value = datos.value.map { if (it.outfit.id == id) it.copy(outfit = it.outfit.copy(favorite = favorite)) else it }
    }

    override suspend fun delete(id: Long) {
        if (falla) error("db")
        datos.value = datos.value.filterNot { it.outfit.id == id }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ListaOutfitsViewModelTest {
    private fun prenda(tipo: GarmentType) = Garment(
        id = 0, photoPath = "p.png", type = tipo, color = GarmentColor.BLACK, season = Season.SUMMER,
        usage = Usage.CASUAL, brand = null, size = null, favorite = false, dirty = false,
        wearsSinceWash = 0, totalWears = 0, washAfterWears = null, createdAt = 1,
    )

    private class Entorno(val armario: GarmentRepositoryEnMemoria, val outfits: OutfitsEnMemoria, val vm: ListaOutfitsViewModel)

    private fun entorno(scope: CoroutineScope): Entorno {
        val armario = GarmentRepositoryEnMemoria()
        val outfits = OutfitsEnMemoria()
        return Entorno(armario, outfits, ListaOutfitsViewModel(outfits, armario, scope))
    }

    private suspend fun Entorno.guardar(nombre: String, creado: Long, vararg prendas: Long): Long = outfits.create(
        Outfit(0, nombre, Occasion.CASUAL, favorite = false, createdAt = creado),
        prendas.mapIndexed { i, g -> OutfitGarment(0, g, 0f, 0f, 1f, 40 + i) },
    )

    @Test
    fun sinOutfitsElEstadoEsVacio() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val estado = e.vm.estado.first { !it.cargando }
        assertTrue(estado.vacio)
    }

    @Test
    fun listaLosOutfitsMasRecientesPrimeroConSusPrendas() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val camiseta = e.armario.add(prenda(GarmentType.TSHIRT))
        val falda = e.armario.add(prenda(GarmentType.SKIRT))
        e.guardar("Viejo", 10, camiseta)
        e.guardar("Nuevo", 20, camiseta, falda)

        val lista = e.vm.estado.value.outfits
        assertEquals(listOf("Nuevo", "Viejo"), lista.map { it.outfit.name })
        assertEquals(listOf(camiseta, falda), lista[0].prendas.map { it.prenda.id })
        assertFalse(e.vm.estado.value.vacio)
    }

    @Test
    fun prendaEliminadaDelArmarioDesapareceDeLaMiniatura() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val camiseta = e.armario.add(prenda(GarmentType.TSHIRT))
        val falda = e.armario.add(prenda(GarmentType.SKIRT))
        e.guardar("Look", 10, camiseta, falda)
        e.armario.delete(falda)
        assertEquals(listOf(camiseta), e.vm.estado.value.outfits.single().prendas.map { it.prenda.id })
    }

    @Test
    fun alternarFavoritoLoMarcaYLoQuita() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val camiseta = e.armario.add(prenda(GarmentType.TSHIRT))
        e.guardar("Look", 10, camiseta)

        e.vm.alternarFavorito(e.vm.estado.value.outfits.single().outfit)
        assertTrue(e.vm.estado.value.outfits.single().outfit.favorite)
        e.vm.alternarFavorito(e.vm.estado.value.outfits.single().outfit)
        assertFalse(e.vm.estado.value.outfits.single().outfit.favorite)
    }

    @Test
    fun eliminarExigeConfirmacion() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val camiseta = e.armario.add(prenda(GarmentType.TSHIRT))
        e.guardar("Look", 10, camiseta)
        val outfit = e.vm.estado.value.outfits.single().outfit

        e.vm.pedirEliminar(outfit)
        assertEquals(outfit, e.vm.estado.value.porEliminar)
        assertEquals(1, e.vm.estado.value.outfits.size)

        e.vm.cancelarEliminar()
        assertNull(e.vm.estado.value.porEliminar)
        assertEquals(1, e.vm.estado.value.outfits.size)

        e.vm.pedirEliminar(outfit)
        e.vm.confirmarEliminar()
        assertNull(e.vm.estado.value.porEliminar)
        assertTrue(e.vm.estado.value.vacio)
    }

    @Test
    fun confirmarSinPeticionNoHaceNada() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.guardar("Look", 10, e.armario.add(prenda(GarmentType.TSHIRT)))
        e.vm.confirmarEliminar()
        assertEquals(1, e.vm.estado.value.outfits.size)
    }

    @Test
    fun errorAlEliminarSeMuestraYSeDescarta() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.guardar("Look", 10, e.armario.add(prenda(GarmentType.TSHIRT)))
        e.outfits.falla = true
        e.vm.pedirEliminar(e.vm.estado.value.outfits.single().outfit)
        e.vm.confirmarEliminar()
        assertNotNull(e.vm.estado.value.error)
        assertEquals(1, e.vm.estado.value.outfits.size)
        e.vm.descartarError()
        assertNull(e.vm.estado.value.error)
    }

    // --- Editar un outfit guardado desde la lista ---

    @Test
    fun editorCargaElOutfitGuardadoYActualizaSinCrearOtro() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val camiseta = e.armario.add(prenda(GarmentType.TSHIRT))
        val id = e.outfits.create(
            Outfit(0, "Look", Occasion.WORK, favorite = true, createdAt = 10),
            listOf(OutfitGarment(0, camiseta, 5f, -3f, 1.2f, 40)),
        )
        val editor = EditorOutfitViewModel(e.outfits, e.armario, backgroundScope, id) { 99L }

        val cargado = editor.estado.first { !it.cargando }
        assertEquals("Look", cargado.nombre)
        assertEquals(Occasion.WORK, cargado.ocasion)
        val elegida = cargado.elegidas.values.single()
        assertEquals(camiseta, elegida.prenda.id)
        assertEquals(5f, elegida.ajuste.dx)
        assertEquals(-3f, elegida.ajuste.dy)
        assertEquals(1.2f, elegida.ajuste.escala)

        editor.cambiarNombre("Look nuevo")
        editor.guardar()

        assertEquals(id, editor.estado.value.idGuardado)
        val guardado = assertNotNull(e.outfits.get(id))
        assertEquals("Look nuevo", guardado.outfit.name)
        assertTrue(guardado.outfit.favorite) // conserva favorito
        assertEquals(10L, guardado.outfit.createdAt) // y fecha
        assertEquals(id, guardado.garments.single().outfitId)
        assertEquals(1, e.vm.estado.value.outfits.size)
    }

    @Test
    fun editorDeOutfitInexistenteMuestraError() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val editor = EditorOutfitViewModel(e.outfits, e.armario, backgroundScope, 404L) { 1L }
        val estado = editor.estado.first { !it.cargando }
        assertNotNull(estado.error)
        assertFalse(estado.puedeGuardar)
    }

    @Test
    fun mostrarErrorYDescartarlo() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        e.vm.mostrarError("No se pudo compartir el outfit")
        assertEquals("No se pudo compartir el outfit", e.vm.estado.value.error)
        e.vm.descartarError()
        assertNull(e.vm.estado.value.error)
    }

    @Test
    fun editarOutfitConPrendaEliminadaLaDescartaYConservaElResto() = runTest(UnconfinedTestDispatcher()) {
        val e = entorno(backgroundScope)
        val camiseta = e.armario.add(prenda(GarmentType.TSHIRT))
        val falda = e.armario.add(prenda(GarmentType.SKIRT))
        val id = e.guardar("Look", 10, camiseta, falda)
        e.armario.delete(falda)

        val editor = EditorOutfitViewModel(e.outfits, e.armario, backgroundScope, id) { 99L }
        val cargado = editor.estado.first { !it.cargando }
        assertEquals(listOf(camiseta), cargado.elegidas.values.map { it.prenda.id })

        editor.guardar()
        assertEquals(listOf(camiseta), assertNotNull(e.outfits.get(id)).garments.map { it.garmentId })
    }
}
