package app.luxion.fitarchiveapp.color

import app.luxion.fitarchiveapp.model.GarmentColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DetectorColorTest {
    private fun opaco(rgb: Int) = (0xFF shl 24) or rgb
    private val transparente = 0x00FFFFFF

    @Test
    fun colorMasCercanoDeCadaReferencia() {
        assertEquals(GarmentColor.BLACK, colorMasCercano(0x000000))
        assertEquals(GarmentColor.WHITE, colorMasCercano(0xFFFFFF))
        assertEquals(GarmentColor.RED, colorMasCercano(0xFF0000))
        assertEquals(GarmentColor.BLUE, colorMasCercano(0x2060E0))
        assertEquals(GarmentColor.GREEN, colorMasCercano(0x00FF00))
        assertEquals(GarmentColor.YELLOW, colorMasCercano(0xFFFF00))
        assertEquals(GarmentColor.NAVY, colorMasCercano(0x101040))
        assertEquals(GarmentColor.GREY, colorMasCercano(0x909090))
        assertEquals(GarmentColor.BROWN, colorMasCercano(0x6B4423))
    }

    @Test
    fun ignoraElFondoTransparente() {
        // 90 % transparente (aunque con RGB blanco) y 10 % rojo: gana el rojo.
        val pixeles = IntArray(1000) { if (it < 100) opaco(0xCC2030) else transparente }
        assertEquals(GarmentColor.RED, detectarColorPrincipal(pixeles))
    }

    @Test
    fun gananLosPixelesMayoritarios() {
        val pixeles = IntArray(100) { if (it < 70) opaco(0x1A2060) else opaco(0xFFFFFF) }
        assertEquals(GarmentColor.NAVY, detectarColorPrincipal(pixeles))
    }

    @Test
    fun variosColoresSinDominanteEsMulticolor() {
        val colores = listOf(0xFF0000, 0x2060E0, 0x00FF00, 0xFFFF00, 0x000000)
        val pixeles = IntArray(100) { opaco(colores[it % colores.size]) }
        assertEquals(GarmentColor.MULTI, detectarColorPrincipal(pixeles))
    }

    @Test
    fun sinPixelesVisiblesDevuelveNull() {
        assertNull(detectarColorPrincipal(IntArray(0)))
        assertNull(detectarColorPrincipal(IntArray(50) { transparente }))
    }

    @Test
    fun imagenGrandeSeMuestrea() {
        val pixeles = IntArray(1_000_000) { opaco(0x000000) }
        assertEquals(GarmentColor.BLACK, detectarColorPrincipal(pixeles))
    }
}
