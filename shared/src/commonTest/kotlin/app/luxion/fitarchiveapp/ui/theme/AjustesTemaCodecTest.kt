package app.luxion.fitarchiveapp.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class AjustesTemaCodecTest {
    @Test
    fun ida_y_vuelta() {
        val a = AjustesTema(ModoTema.OSCURO, Acento.AZUL)
        assertEquals(a, AjustesTemaCodec.decodificar(AjustesTemaCodec.codificar(a)))
    }

    @Test
    fun nulo_o_basura_da_defecto() {
        assertEquals(AjustesTema(), AjustesTemaCodec.decodificar(null))
        assertEquals(AjustesTema(), AjustesTemaCodec.decodificar("x|y"))
    }
}
