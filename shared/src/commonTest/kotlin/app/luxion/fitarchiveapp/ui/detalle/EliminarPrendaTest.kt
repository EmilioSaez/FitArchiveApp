package app.luxion.fitarchiveapp.ui.detalle

import app.luxion.fitarchiveapp.data.GarmentRepositoryEnMemoria
import app.luxion.fitarchiveapp.foto.AlmacenFotos
import app.luxion.fitarchiveapp.foto.FotoCapturada
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class AlmacenFalso(val falla: Boolean = false) : AlmacenFotos {
    val borradas = mutableListOf<String>()
    override suspend fun guardar(foto: FotoCapturada) = "nueva.jpg"
    override suspend fun borrar(ruta: String) {
        if (falla) error("sin acceso al archivo")
        borradas += ruta
    }
}

class EliminarPrendaTest {
    private fun prenda(foto: String) = Garment(
        id = 0, photoPath = foto, type = GarmentType.TSHIRT, color = GarmentColor.BLACK, season = Season.SUMMER,
        usage = Usage.CASUAL, brand = null, size = null, favorite = false, dirty = false,
        wearsSinceWash = 0, totalWears = 0, washAfterWears = null, createdAt = 1,
    )

    @Test
    fun eliminaLaPrendaYBorraSuFoto() = runTest {
        val repo = GarmentRepositoryEnMemoria()
        val almacen = AlmacenFalso()
        val a = repo.get(repo.add(prenda("a.jpg")))!!
        val b = repo.get(repo.add(prenda("b.jpg")))!!

        eliminarPrenda(repo, almacen, a)

        assertNull(repo.get(a.id))
        assertEquals(listOf(b.id), repo.observeAll().first().map { it.id })
        assertEquals(listOf("a.jpg"), almacen.borradas)
    }

    @Test
    fun siFallaElBorradoDeLaFotoLaPrendaSeEliminaIgualmente() = runTest {
        val repo = GarmentRepositoryEnMemoria()
        val a = repo.get(repo.add(prenda("a.jpg")))!!

        eliminarPrenda(repo, AlmacenFalso(falla = true), a)

        assertNull(repo.get(a.id))
    }
}
