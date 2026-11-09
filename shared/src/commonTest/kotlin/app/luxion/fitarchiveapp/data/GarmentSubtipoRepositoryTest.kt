package app.luxion.fitarchiveapp.data

import app.luxion.fitarchiveapp.avatar.AjustePrenda
import app.luxion.fitarchiveapp.avatar.Subtipo
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

/** Subtipo y ajuste manual de colocación guardados en la prenda. */
class GarmentSubtipoRepositoryTest {
    private val repo = Repositories(createTestDatabase(), now = { 0L }).garments

    private fun prenda(subtipo: Subtipo? = null, ajuste: AjustePrenda? = null) = Garment(
        id = 0, photoPath = "p.png", type = GarmentType.SHIRT, color = GarmentColor.BLUE, season = Season.SUMMER,
        usage = Usage.CASUAL, brand = null, size = null, favorite = false, dirty = false, wearsSinceWash = 0,
        totalWears = 0, washAfterWears = null, createdAt = 1, subtipo = subtipo, ajuste = ajuste,
    )

    @Test
    fun sinSubtipoNiAjusteSeGuardaNull() = runTest {
        val id = repo.add(prenda())
        val leida = repo.get(id)!!
        assertNull(leida.subtipo)
        assertNull(leida.ajuste)
    }

    @Test
    fun guardaYRecuperaSubtipoYAjuste() = runTest {
        val original = prenda(Subtipo.TOP_SIN_MANGAS_CORTO, AjustePrenda(12.5f, -3f, 1.25f))
        val id = repo.add(original)
        assertEquals(original.copy(id = id), repo.get(id))
        assertEquals(original.copy(id = id), repo.observeAll().first().single())
    }

    @Test
    fun updateCambiaYQuitaSubtipoYAjuste() = runTest {
        val id = repo.add(prenda())
        repo.update(repo.get(id)!!.copy(subtipo = Subtipo.TOP_MANGA_LARGA, ajuste = AjustePrenda(1f, 2f, 0.8f)))
        assertEquals(Subtipo.TOP_MANGA_LARGA, repo.get(id)!!.subtipo)
        assertEquals(AjustePrenda(1f, 2f, 0.8f), repo.get(id)!!.ajuste)

        repo.update(repo.get(id)!!.copy(subtipo = null, ajuste = null))
        assertNull(repo.get(id)!!.subtipo)
        assertNull(repo.get(id)!!.ajuste)
    }

    @Test
    fun setAjusteGuardaYRestablecerLoQuitaSinTocarElSubtipo() = runTest {
        val id = repo.add(prenda(Subtipo.TOP_MANGA_LARGA))
        repo.setAjuste(id, AjustePrenda(-7f, 4f, 1.4f))
        assertEquals(AjustePrenda(-7f, 4f, 1.4f), repo.get(id)!!.ajuste)
        assertEquals(Subtipo.TOP_MANGA_LARGA, repo.get(id)!!.subtipo)

        repo.setAjuste(id, null)
        assertNull(repo.get(id)!!.ajuste)
        assertEquals(Subtipo.TOP_MANGA_LARGA, repo.get(id)!!.subtipo)
    }

    @Test
    fun registrarUnUsoConservaSubtipoYAjuste() = runTest {
        val id = repo.add(prenda(Subtipo.TOP_LARGO_CADERA, AjustePrenda(2f, 3f, 1.1f)))
        repo.registerWear(id)
        val leida = repo.get(id)!!
        assertEquals(1, leida.totalWears)
        assertEquals(Subtipo.TOP_LARGO_CADERA, leida.subtipo)
        assertEquals(AjustePrenda(2f, 3f, 1.1f), leida.ajuste)
    }

    @Test
    fun otrasPrendasNoSeVenAfectadasPorElAjuste() = runTest {
        val a = repo.add(prenda())
        val b = repo.add(prenda())
        repo.setAjuste(a, AjustePrenda(1f, 1f, 1f))
        assertNull(repo.get(b)!!.ajuste)
    }
}
