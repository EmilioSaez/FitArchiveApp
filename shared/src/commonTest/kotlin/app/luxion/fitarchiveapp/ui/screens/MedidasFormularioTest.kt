package app.luxion.fitarchiveapp.ui.screens

import app.luxion.fitarchiveapp.model.Measurements
import kotlin.test.Test
import kotlin.test.assertEquals

class MedidasFormularioTest {
    @Test
    fun vacioEsSinDato() = assertEquals(Lectura.Valor(null), leer("  ", Campo.Altura))

    @Test
    fun aceptaComaDecimal() = assertEquals(Lectura.Valor(65.5f), leer("65,5", Campo.Cintura))

    @Test
    fun fueraDeRangoOTextoEsInvalido() {
        assertEquals(Lectura.Invalido, leer("99", Campo.Altura))
        assertEquals(Lectura.Invalido, leer("231", Campo.Altura))
        assertEquals(Lectura.Invalido, leer("abc", Campo.Pecho))
    }

    @Test
    fun limitesSonValidos() {
        assertEquals(Lectura.Valor(100f), leer("100", Campo.Altura))
        assertEquals(Lectura.Valor(230f), leer("230", Campo.Altura))
    }

    @Test
    fun comoTexto() {
        assertEquals("", null.comoTexto())
        assertEquals("170", 170f.comoTexto())
        assertEquals("65.5", 65.5f.comoTexto())
    }

    @Test
    fun sinMedidasTodosLosTextosVacios() {
        assertEquals(Campo.entries.toSet(), null.aTextos().keys)
        assertEquals(setOf(""), null.aTextos().values.toSet())
    }

    @Test
    fun idaYVueltaMedidasTextosMedidas() {
        val medidas = Measurements(170f, 90f, 65.5f, 95f, 40f, 58f, 80f, updatedAt = 123L)
        val lecturas = medidas.aTextos().mapValues { (c, t) -> leer(t, c) }
        assertEquals(medidas.copy(updatedAt = 0L), lecturas.aMedidas())
    }

    @Test
    fun camposInvalidosSeGuardanVacios() {
        val m = mapOf(Campo.Altura to Lectura.Valor(170f), Campo.Pecho to Lectura.Invalido).aMedidas()
        assertEquals(170f, m.heightCm)
        assertEquals(null, m.bustCm)
    }
}
