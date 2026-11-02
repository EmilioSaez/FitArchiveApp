package app.luxion.fitarchiveapp.ui.outfit

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PuedeCompartirTest {
    @Test
    fun sinPrendasNoSePuedeCompartir() = assertFalse(puedeCompartir(elegidas = 0, cargadas = 0))

    @Test
    fun conFotosPendientesNoSePuedeCompartir() {
        assertFalse(puedeCompartir(elegidas = 2, cargadas = 0))
        assertFalse(puedeCompartir(elegidas = 2, cargadas = 1))
    }

    @Test
    fun conTodasLasFotosCargadasSePuedeCompartir() = assertTrue(puedeCompartir(elegidas = 2, cargadas = 2))
}
