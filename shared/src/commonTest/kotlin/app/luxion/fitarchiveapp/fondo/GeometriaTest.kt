package app.luxion.fitarchiveapp.fondo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GeometriaTest {
    @Test
    fun noAmpliaNiTocaImagenesPequenas() {
        assertEquals(800 to 600, Geometria.ajustar(800, 600))
        assertEquals(1536 to 1000, Geometria.ajustar(1536, 1000))
    }

    @Test
    fun reduceConservandoProporcion() {
        assertEquals(1536 to 1152, Geometria.ajustar(4000, 3000))
        assertEquals(1152 to 1536, Geometria.ajustar(3000, 4000))
        assertEquals(100 to 50, Geometria.ajustar(400, 200, ladoMaximo = 100))
    }

    @Test
    fun ladoMinimoNuncaEsCero() {
        assertEquals(100 to 1, Geometria.ajustar(10_000, 5, ladoMaximo = 100))
    }

    @Test
    fun dimensionesInvalidasFallan() {
        assertFailsWith<IllegalArgumentException> { Geometria.ajustar(0, 10) }
        assertFailsWith<IllegalArgumentException> { Geometria.ajustar(10, 10, 0) }
    }

    private fun px(alfa: Int) = alfa shl 24

    @Test
    fun cajaOpacaEncuentraElSujeto() {
        // 5x4, opacos en (1,1) y (3,2)
        val p = IntArray(20)
        p[1 * 5 + 1] = px(255)
        p[2 * 5 + 3] = px(200)
        assertEquals(Geometria.Caja(1, 1, 4, 3), Geometria.cajaOpaca(p, 5, 4))
    }

    @Test
    fun cajaOpacaIgnoraRuidoBajoElUmbral() {
        val p = IntArray(9)
        p[0] = px(5)
        p[4] = px(255)
        assertEquals(Geometria.Caja(1, 1, 2, 2), Geometria.cajaOpaca(p, 3, 3))
    }

    @Test
    fun cajaOpacaSinSujetoEsNula() {
        assertNull(Geometria.cajaOpaca(IntArray(6), 3, 2))
    }

    @Test
    fun cajaOpacaValidaTamano() {
        assertFailsWith<IllegalArgumentException> { Geometria.cajaOpaca(IntArray(5), 3, 2) }
    }

    @Test
    fun margenNoSaleDeLaImagen() {
        val c = Geometria.conMargen(Geometria.Caja(1, 1, 9, 9), 5, 10, 10)
        assertEquals(Geometria.Caja(0, 0, 10, 10), c)
        assertEquals(10, c.ancho)
    }
}
