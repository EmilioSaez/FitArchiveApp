package app.luxion.fitarchiveapp.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UsosParaLavarTest {
    private fun prenda(
        type: GarmentType = GarmentType.TSHIRT,
        washAfterWears: Int? = null,
        wearsSinceWash: Int = 0,
        dirty: Boolean = false,
    ) = Garment(
        id = 1, photoPath = "p.png", type = type, color = GarmentColor.BLACK, season = Season.SUMMER, usage = Usage.CASUAL,
        brand = null, size = null, favorite = false, dirty = dirty, wearsSinceWash = wearsSinceWash, totalWears = 0,
        washAfterWears = washAfterWears, createdAt = 1,
    )

    @Test
    fun valoresPorDefectoRazonables() {
        assertEquals(1, GarmentType.TSHIRT.usosParaLavarPorDefecto())
        assertEquals(3, GarmentType.TROUSERS.usosParaLavarPorDefecto())
        assertEquals(10, GarmentType.COAT.usosParaLavarPorDefecto())
        assertNull(GarmentType.SHOES.usosParaLavarPorDefecto())
        assertNull(GarmentType.BAG.usosParaLavarPorDefecto())
    }

    @Test
    fun sinValorPropioValeElDelTipo() {
        assertEquals(3, prenda(GarmentType.JEANS).usosParaLavar())
    }

    @Test
    fun elValorPropioManda() {
        assertEquals(5, prenda(GarmentType.JEANS, washAfterWears = 5).usosParaLavar())
    }

    @Test
    fun ceroDesactivaLaSuciaAutomatica() {
        assertNull(prenda(washAfterWears = SOLO_MANUAL).usosParaLavar())
        assertFalse(prenda(washAfterWears = SOLO_MANUAL, wearsSinceWash = 50).sucioTrasUso())
    }

    @Test
    fun pasaASuciaAlAlcanzarElUmbral() {
        val pantalon = prenda(GarmentType.TROUSERS)
        assertFalse(pantalon.copy(wearsSinceWash = 1).sucioTrasUso())
        assertTrue(pantalon.copy(wearsSinceWash = 2).sucioTrasUso())
        assertTrue(prenda(GarmentType.TSHIRT).sucioTrasUso())
    }

    @Test
    fun unaPrendaSuciaSigueSucia() {
        assertTrue(prenda(GarmentType.COAT, dirty = true).sucioTrasUso())
    }

    @Test
    fun calzadoNuncaPasaASuciaSolo() {
        assertFalse(prenda(GarmentType.SHOES, wearsSinceWash = 100).sucioTrasUso())
    }
}
