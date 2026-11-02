package app.luxion.fitarchiveapp.ui.calendario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MesCalendarioTest {
    @Test
    fun noviembre2026EmpiezaEnDomingoYOcupaSeisSemanas() {
        val celdas = MesCalendario(2026, 11).celdas()
        assertEquals(42, celdas.size)
        assertEquals(List(6) { null }, celdas.take(6))
        assertEquals("2026-11-01", celdas[6])
        assertEquals("2026-11-30", celdas.filterNotNull().last())
        assertEquals(30, celdas.filterNotNull().size)
    }

    @Test
    fun mesQueEmpiezaEnLunesNoTieneHuecosIniciales() {
        // Junio de 2026 empieza en lunes.
        val celdas = MesCalendario(2026, 6).celdas()
        assertEquals("2026-06-01", celdas.first())
        assertEquals(35, celdas.size)
    }

    @Test
    fun siempreSemanasCompletas() {
        for (mes in 1..12) assertEquals(0, MesCalendario(2026, mes).celdas().size % 7)
    }

    @Test
    fun febreroBisiestoTiene29Dias() {
        assertEquals("2028-02-29", MesCalendario(2028, 2).ultimoDia.toString())
        assertEquals("2027-02-28", MesCalendario(2027, 2).ultimoDia.toString())
    }

    @Test
    fun navegaEntreAniosYTitulo() {
        assertEquals(MesCalendario(2027, 1), MesCalendario(2026, 12).siguiente())
        assertEquals(MesCalendario(2025, 12), MesCalendario(2026, 1).anterior())
        assertEquals("Noviembre 2026", MesCalendario(2026, 11).titulo)
    }

    @Test
    fun mesDeUnaFechaIso() {
        assertEquals(MesCalendario(2026, 11), MesCalendario.de("2026-11-02"))
    }

    @Test
    fun mesFueraDeRangoEsInvalido() {
        assertFailsWith<IllegalArgumentException> { MesCalendario(2026, 13) }
        assertFailsWith<IllegalArgumentException> { MesCalendario(2026, 0) }
    }

    @Test
    fun huecosSonNulos() {
        val celdas = MesCalendario(2026, 11).celdas()
        assertNull(celdas.first())
        assertNull(celdas.last())
        assertTrue(celdas[6] != null)
    }
}
