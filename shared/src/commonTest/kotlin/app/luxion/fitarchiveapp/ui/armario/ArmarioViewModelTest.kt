package app.luxion.fitarchiveapp.ui.armario

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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ArmarioViewModelTest {
    private fun prenda(tipo: GarmentType) = Garment(
        id = 0, photoPath = "p.png", type = tipo, color = GarmentColor.BLACK, season = Season.SUMMER,
        usage = Usage.CASUAL, brand = null, size = null, favorite = false, dirty = false,
        wearsSinceWash = 0, totalWears = 0, washAfterWears = null, createdAt = 1,
    )

    @Test
    fun armarioSinPrendasEstaVacio() = runTest(UnconfinedTestDispatcher()) {
        val vm = ArmarioViewModel(GarmentRepositoryEnMemoria(), backgroundScope)
        val estado = vm.estado.first { !it.cargando }
        assertTrue(estado.armarioVacio)
        assertEquals(PestanaTipo.entries.size, estado.pestanas.size)
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
}
