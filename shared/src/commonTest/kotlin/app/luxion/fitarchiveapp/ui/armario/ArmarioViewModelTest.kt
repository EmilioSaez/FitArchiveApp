package app.luxion.fitarchiveapp.ui.armario

import app.luxion.fitarchiveapp.data.GarmentFilter
import app.luxion.fitarchiveapp.data.GarmentRepositoryEnMemoria
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ArmarioViewModelTest {
    private fun prenda(
        tipo: GarmentType,
        color: GarmentColor = GarmentColor.BLACK,
        estacion: Season = Season.SUMMER,
        uso: Usage = Usage.CASUAL,
        favorita: Boolean = false,
    ) = Garment(
        id = 0, photoPath = "p.png", type = tipo, color = color, season = estacion,
        usage = uso, brand = null, size = null, favorite = favorita, dirty = false,
        wearsSinceWash = 0, totalWears = 0, washAfterWears = null, createdAt = 1,
    )

    @Test
    fun armarioSinPrendasEstaVacio() = runTest(UnconfinedTestDispatcher()) {
        val vm = ArmarioViewModel(GarmentRepositoryEnMemoria(), backgroundScope)
        val estado = vm.estado.first { !it.cargando }
        assertTrue(estado.armarioVacio)
        assertEquals(PestanaTipo.entries.size + 1, estado.pestanas.size)
    }

    @Test
    fun repartePrendasPorPestanaYReaccionaAAltas() = runTest(UnconfinedTestDispatcher()) {
        val repo = GarmentRepositoryEnMemoria()
        val vm = ArmarioViewModel(repo, backgroundScope)
        repo.add(prenda(GarmentType.TSHIRT))
        repo.add(prenda(GarmentType.SHIRT))
        repo.add(prenda(GarmentType.SHOES))

        val camisetas = vm.estado.first { it.total == 3 }
        assertEquals(2, camisetas.prendas.size)
        assertEquals(2, camisetas.conteos[PestanaTipo.Camisetas.ordinal])
        assertEquals(1, camisetas.conteos[PestanaTipo.Calzado.ordinal])

        vm.seleccionarPestana(PestanaTipo.Calzado.ordinal)
        val calzado = vm.estado.first { it.pestanaActual == PestanaTipo.Calzado.ordinal }
        assertEquals(listOf(GarmentType.SHOES), calzado.prendas.map { it.type })
    }

    @Test
    fun todosLosTiposTienenPestana() {
        val cubiertos = PestanaTipo.entries.flatMap { it.tipos }.toSet()
        assertEquals(GarmentType.entries.toSet(), cubiertos)
    }

    @Test
    fun agrupaPorColor() = runTest(UnconfinedTestDispatcher()) {
        val repo = GarmentRepositoryEnMemoria()
        val vm = ArmarioViewModel(repo, backgroundScope)
        repo.add(prenda(GarmentType.TSHIRT, color = GarmentColor.RED))
        repo.add(prenda(GarmentType.SHOES, color = GarmentColor.RED))
        repo.add(prenda(GarmentType.SHIRT, color = GarmentColor.BLUE))

        vm.seleccionarPestana(3)
        vm.seleccionarAgrupacion(Agrupacion.PorColor)
        val estado = vm.estado.first { it.agrupacion == Agrupacion.PorColor }
        assertEquals(0, estado.pestanaActual) // al cambiar de agrupación se vuelve a la primera
        assertEquals(GarmentColor.entries.size, estado.pestanas.size)
        assertEquals(2, estado.conteos[GarmentColor.RED.ordinal])
        assertEquals(1, estado.conteos[GarmentColor.BLUE.ordinal])

        vm.seleccionarPestana(GarmentColor.RED.ordinal)
        val rojos = vm.estado.first { it.pestanaActual == GarmentColor.RED.ordinal }
        assertEquals(2, rojos.prendas.size)
    }

    @Test
    fun agrupaPorEstacion() = runTest(UnconfinedTestDispatcher()) {
        val repo = GarmentRepositoryEnMemoria()
        val vm = ArmarioViewModel(repo, backgroundScope)
        repo.add(prenda(GarmentType.COAT, estacion = Season.WINTER))
        repo.add(prenda(GarmentType.TSHIRT, estacion = Season.SUMMER))
        repo.add(prenda(GarmentType.SHIRT, estacion = Season.SUMMER))

        vm.seleccionarAgrupacion(Agrupacion.PorEstacion)
        val estado = vm.estado.first { it.agrupacion == Agrupacion.PorEstacion }
        assertEquals(Season.entries.size, estado.pestanas.size)
        assertEquals(1, estado.conteos[Season.WINTER.ordinal])
        assertEquals(2, estado.conteos[Season.SUMMER.ordinal])
        assertEquals(0, estado.conteos[Season.MID_SEASON.ordinal])
    }

    @Test
    fun agrupaPorUso() = runTest(UnconfinedTestDispatcher()) {
        val repo = GarmentRepositoryEnMemoria()
        val vm = ArmarioViewModel(repo, backgroundScope)
        repo.add(prenda(GarmentType.SHIRT, uso = Usage.WORK))
        repo.add(prenda(GarmentType.JACKET, uso = Usage.WORK))
        repo.add(prenda(GarmentType.DRESS, uso = Usage.PARTY))

        vm.seleccionarAgrupacion(Agrupacion.PorUso)
        val estado = vm.estado.first { it.agrupacion == Agrupacion.PorUso }
        assertEquals(Usage.entries.size, estado.pestanas.size)
        assertEquals(2, estado.conteos[Usage.WORK.ordinal])
        assertEquals(1, estado.conteos[Usage.PARTY.ordinal])
        assertEquals(0, estado.conteos[Usage.SPORT.ordinal])
        // Cada prenda tiene un único uso: las pestañas suman el total.
        assertEquals(estado.total, estado.conteos.sum())
        assertTrue(Agrupacion.PorUso in Agrupacion.todas)
    }

    @Test
    fun filtroPorUsoSeCombinaConAgrupacionPorUso() = runTest(UnconfinedTestDispatcher()) {
        val repo = GarmentRepositoryEnMemoria()
        val vm = ArmarioViewModel(repo, backgroundScope)
        repo.add(prenda(GarmentType.SHIRT, uso = Usage.WORK))
        repo.add(prenda(GarmentType.JACKET, uso = Usage.CASUAL))

        vm.seleccionarAgrupacion(Agrupacion.PorUso)
        vm.aplicarFiltro(GarmentFilter(usage = Usage.WORK))
        val estado = vm.estado.first { it.total == 1 && it.agrupacion == Agrupacion.PorUso }
        assertEquals(1, estado.conteos[Usage.WORK.ordinal])
        assertEquals(0, estado.conteos[Usage.CASUAL.ordinal])
        assertEquals(2, estado.totalArmario)
    }

    @Test
    fun filtroCombinaVariosCriterios() = runTest(UnconfinedTestDispatcher()) {
        val repo = GarmentRepositoryEnMemoria()
        val vm = ArmarioViewModel(repo, backgroundScope)
        repo.add(prenda(GarmentType.TSHIRT, GarmentColor.RED, Season.SUMMER, Usage.CASUAL))
        repo.add(prenda(GarmentType.TSHIRT, GarmentColor.RED, Season.WINTER, Usage.WORK))
        repo.add(prenda(GarmentType.TSHIRT, GarmentColor.BLUE, Season.SUMMER, Usage.CASUAL))
        repo.add(prenda(GarmentType.SHOES, GarmentColor.RED, Season.SUMMER, Usage.CASUAL))

        vm.aplicarFiltro(GarmentFilter(color = GarmentColor.RED, season = Season.SUMMER))
        val dos = vm.estado.first { it.total == 2 }
        assertEquals(4, dos.totalArmario)
        assertEquals(2, dos.filtro.numeroCriterios())
        assertEquals(1, dos.prendas.size) // pestaña Camisetas

        vm.aplicarFiltro(GarmentFilter(color = GarmentColor.RED, season = Season.SUMMER, usage = Usage.WORK))
        val ninguna = vm.estado.first { it.total == 0 }
        assertTrue(ninguna.filtroSinResultados)
        assertFalse(ninguna.armarioVacio)
    }

    @Test
    fun filtroSoloFavoritasYLimpiar() = runTest(UnconfinedTestDispatcher()) {
        val repo = GarmentRepositoryEnMemoria()
        val vm = ArmarioViewModel(repo, backgroundScope)
        repo.add(prenda(GarmentType.TSHIRT, favorita = true))
        repo.add(prenda(GarmentType.TSHIRT))

        vm.aplicarFiltro(GarmentFilter(favorite = true))
        val favoritas = vm.estado.first { it.total == 1 && it.hayFiltro }
        assertTrue(favoritas.prendas.single().favorite)

        vm.limpiarFiltro()
        val todas = vm.estado.first { it.total == 2 }
        assertFalse(todas.hayFiltro)
    }

    @Test
    fun elFiltroSeMantieneAlCambiarDeAgrupacion() = runTest(UnconfinedTestDispatcher()) {
        val repo = GarmentRepositoryEnMemoria()
        val vm = ArmarioViewModel(repo, backgroundScope)
        repo.add(prenda(GarmentType.TSHIRT, GarmentColor.RED))
        repo.add(prenda(GarmentType.TSHIRT, GarmentColor.BLUE))

        vm.aplicarFiltro(GarmentFilter(color = GarmentColor.RED))
        vm.seleccionarAgrupacion(Agrupacion.PorEstacion)
        val estado = vm.estado.first { it.agrupacion == Agrupacion.PorEstacion && it.total == 1 }
        assertEquals(GarmentColor.RED, estado.filtro.color)
    }

    @Test
    fun alternarFavoritaActualizaLaPestanaFavoritas() = runTest(UnconfinedTestDispatcher()) {
        val repo = GarmentRepositoryEnMemoria()
        val vm = ArmarioViewModel(repo, backgroundScope)
        repo.add(prenda(GarmentType.TSHIRT))
        repo.add(prenda(GarmentType.SHOES))
        val favoritas = vm.estado.first { it.total == 2 }.pestanas.indexOfFirst { it.id == "FAVORITAS" }
        vm.seleccionarPestana(favoritas)
        assertTrue(vm.estado.first { it.total == 2 && it.pestanaActual == favoritas }.prendas.isEmpty())

        val zapatos = vm.estado.first { it.total == 2 }.let { repo.get(2)!! }
        vm.alternarFavorita(zapatos)
        val con = vm.estado.first { it.prendas.isNotEmpty() }
        assertEquals(listOf(GarmentType.SHOES), con.prendas.map { it.type })
        assertEquals(1, con.conteos[favoritas])

        vm.alternarFavorita(con.prendas.single())
        assertTrue(vm.estado.first { it.prendas.isEmpty() }.conteos[favoritas] == 0)
    }
}
