package app.luxion.fitarchiveapp.ui

import app.luxion.fitarchiveapp.model.Fit
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.ui.anadir.BorradorPrenda
import app.luxion.fitarchiveapp.ui.anadir.sugerenciasDeMarca
import app.luxion.fitarchiveapp.ui.anadir.tallasPara
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BorradorPrendaTest {
    private val completo = BorradorPrenda(
        type = GarmentType.DRESS, color = GarmentColor.BURGUNDY, season = Season.WINTER, usage = Usage.PARTY,
    )

    @Test
    fun sinCamposObligatoriosNoSePuedeGuardar() {
        assertFalse(BorradorPrenda().completo)
        assertNull(BorradorPrenda(type = GarmentType.DRESS).aPrenda("f.jpg", 1))
        assertTrue(completo.completo)
    }

    @Test
    fun marcaYTallaSonOpcionales() {
        val g = completo.aPrenda("f.jpg", 5)!!
        assertNull(g.brand)
        assertNull(g.size)
        assertEquals("f.jpg", g.photoPath)
        assertEquals(5, g.createdAt)
    }

    @Test
    fun marcaSeRecortaYTallaSeConserva() {
        val g = completo.copy(brand = "  Zara ", size = "M").aPrenda("f.jpg", 1)!!
        assertEquals("Zara", g.brand)
        assertEquals("M", g.size)
        assertEquals(0, g.totalWears)
        assertFalse(g.favorite)
    }

    @Test
    fun sugerenciasPorPrefijo() {
        assertEquals(listOf("Massimo Dutti", "Mango"), sugerenciasDeMarca("ma"))
        assertEquals(emptyList(), sugerenciasDeMarca(""))
        assertEquals(emptyList(), sugerenciasDeMarca("zara"))
        assertEquals(emptyList(), sugerenciasDeMarca("Otra"))
    }

    @Test
    fun editarConservaIdFotoYContadores() {
        val original = completo.copy(brand = "Zara", size = "M").aPrenda("f.jpg", 9)!!
            .copy(id = 7, favorite = true, dirty = true, totalWears = 4, wearsSinceWash = 2, washAfterWears = 3)
        val editado = BorradorPrenda.desde(original).copy(type = GarmentType.SHIRT, brand = " ", size = null)
            .aplicarA(original)!!
        assertEquals(original.copy(type = GarmentType.SHIRT, brand = null, size = null), editado)
    }

    @Test
    fun desdeYAplicarSinCambiosEsIdempotente() {
        val g = completo.copy(brand = "Mango", size = "S").aPrenda("f.jpg", 1)!!.copy(id = 3)
        assertEquals(g, BorradorPrenda.desde(g).aplicarA(g))
    }

    @Test
    fun editarSinCampoObligatorioNoGuarda() {
        val g = completo.aPrenda("f.jpg", 1)!!
        assertNull(BorradorPrenda.desde(g).copy(color = null).aplicarA(g))
    }

    @Test
    fun elCorteEsNormalPorDefectoYSeConserva() {
        assertEquals(Fit.NORMAL, completo.aPrenda("f.jpg", 1)!!.fit)
        val g = completo.copy(fit = Fit.OVERSIZE).aPrenda("f.jpg", 1)!!
        assertEquals(Fit.OVERSIZE, g.fit)
        assertEquals(Fit.OVERSIZE, BorradorPrenda.desde(g).fit)
        val editado = BorradorPrenda.desde(g).copy(fit = Fit.SLIM).aplicarA(g)!!
        assertEquals(g.copy(fit = Fit.SLIM), editado)
    }

    @Test
    fun tallasSegunElTipo() {
        assertTrue("M" in tallasPara(GarmentType.TSHIRT) && "38" in tallasPara(GarmentType.JEANS))
        assertTrue("XXL" in tallasPara(null))
        assertEquals((35..46).map { it.toString() }, tallasPara(GarmentType.SHOES))
        assertFalse("M" in tallasPara(GarmentType.SHOES))
    }
}
