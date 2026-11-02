package app.luxion.fitarchiveapp.fondo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class ReintentosTest {
    @Test
    fun reintentaHastaQueTengaExito() = runTest {
        var llamadas = 0
        val r = conReintentos(maximo = 5, espera = 10, esReintentable = { true }) {
            if (++llamadas < 3) error("descargando") else "ok"
        }
        assertEquals("ok", r)
        assertEquals(3, llamadas)
    }

    @Test
    fun noReintentaErroresNoReintentables() = runTest {
        var llamadas = 0
        assertFailsWith<IllegalStateException> {
            conReintentos(maximo = 5, espera = 10, esReintentable = { false }) { llamadas++; error("x") }
        }
        assertEquals(1, llamadas)
    }

    @Test
    fun seRindeTrasElMaximo() = runTest {
        var llamadas = 0
        assertFailsWith<IllegalStateException> {
            conReintentos(maximo = 3, espera = 10, esReintentable = { true }) { llamadas++; error("x") }
        }
        assertEquals(3, llamadas)
    }
}
