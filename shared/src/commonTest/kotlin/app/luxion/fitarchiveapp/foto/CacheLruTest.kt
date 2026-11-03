package app.luxion.fitarchiveapp.foto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CacheLruTest {
    private fun cache(max: Long) = CacheLru<String, Int>(max) { it.toLong() }

    @Test
    fun expulsaLaMenosUsadaAlSuperarElPeso() {
        val c = cache(10)
        c.put("a", 4); c.put("b", 4)
        c.get("a") // "b" pasa a ser la menos usada
        c.put("c", 4)
        assertNull(c.get("b"))
        assertEquals(4, c.get("a"))
        assertEquals(4, c.get("c"))
        assertEquals(8, c.pesoActual)
    }

    @Test
    fun reemplazarUnaClaveAjustaElPeso() {
        val c = cache(10)
        c.put("a", 6); c.put("a", 3)
        assertEquals(1, c.tamano)
        assertEquals(3, c.pesoActual)
    }

    @Test
    fun conservaLaUltimaAunqueSuperePorSiSola() {
        val c = cache(5)
        c.put("a", 3)
        c.put("grande", 9)
        assertNull(c.get("a"))
        assertEquals(9, c.get("grande"))
    }
}
