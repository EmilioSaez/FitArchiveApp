package app.luxion.fitarchiveapp.tallas

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Prueba el cargador con los JSON reales de composeResources/files/tallas. */
class CargadorTallasTest {
    private val dir = File("src/commonMain/composeResources/files/tallas")

    private fun tabla(id: String): TablaTallas = CargadorTallas.parsear(File(dir, "$id.json").readText())

    private fun TablaPrenda.medidasUsadas(): Set<Medida> = tallas.flatMap { it.medidas.keys }.toSet()

    @Test
    fun todosLosIdsTienenFicheroYSeCargan() {
        for (id in CargadorTallas.IDS) {
            assertTrue(File(dir, "$id.json").exists(), "falta $id.json")
            val t = tabla(id)
            assertTrue(t.marca.isNotBlank(), id)
            assertEquals(emptyList(), t.avisos, "avisos en $id")
        }
        assertEquals(CargadorTallas.IDS.size, dir.listFiles { f -> f.extension == "json" }!!.size)
    }

    @Test
    fun tablasVaciasNoFallan() {
        for (id in listOf("zara", "pullbear", "bershka", "stradivarius", "massimo_dutti", "mango", "brandy_melville")) {
            val t = tabla(id)
            assertTrue(t.estaVacia, id)
            assertTrue(t.pendiente.isNotEmpty(), id)
        }
    }

    @Test
    fun tallaUnica() {
        assertTrue(tabla("brandy_melville").tallaUnica)
        assertFalse(tabla("hm").tallaUnica)
    }

    @Test
    fun genericaConCalzadoYEquivalencias() {
        val t = tabla("generica-eu")
        assertEquals("cm", t.unidad)
        assertEquals(listOf(38, 40), t.equivalencias["M"])
        val camiseta = assertNotNull(t.prenda("camiseta"))
        assertEquals(Rango(86.0, 94.0), camiseta.talla("M")!![Medida.PECHO])
        assertNull(camiseta.talla("M")!![Medida.PIE])
        val calzado = assertNotNull(t.prenda("calzado"))
        assertEquals(setOf(Medida.PIE), calzado.medidasUsadas())
        assertEquals(12, calzado.tallas.size)
    }

    @Test
    fun medidasOpcionalesPorPrenda() {
        val hollister = tabla("hollister")
        assertEquals(setOf(Medida.PECHO), hollister.prenda("parte_superior")!!.medidasUsadas())
        assertEquals(setOf(Medida.CINTURA, Medida.CADERA), hollister.prenda("parte_inferior")!!.medidasUsadas())
        assertEquals("rango", hollister.tipoValor)
    }

    @Test
    fun valoresUnicosYEquivalenciaEu() {
        val xs = tabla("brownie").prenda("parte_superior")!!.talla("XS")!!
        assertEquals(Rango(81.0, 81.0), xs[Medida.PECHO])
        assertTrue(xs[Medida.PECHO]!!.esValorUnico)
        assertEquals("38", tabla("scalpers").prenda("parte_inferior")!!.talla("M")!!.equivalenciaEu)
        val hm = tabla("hm").prenda("camiseta")!!.talla("S")!!
        assertEquals("36-38", hm.equivalenciaEu)
        assertEquals(Rango(90.0, 97.5), hm[Medida.CADERA])
    }

    @Test
    fun toleraVariantesYEntradasInvalidas() {
        val t = CargadorTallas.parsear(
            """{"marca":"X","fuente":"a","unidades":"cm","nota":"n","pendiente":"p",
              "prendas":{"camiseta":{"S":{"pecho":[80],"cintura":"x","desconocida":[1,2]},"M":5},"raro":3}}""",
        )
        assertEquals(listOf("a"), t.fuentes)
        assertEquals(listOf("n"), t.notas)
        assertEquals(listOf("p"), t.pendiente)
        assertEquals(Rango(80.0, 80.0), t.prenda("camiseta")!!.talla("S")!![Medida.PECHO])
        assertEquals(3, t.avisos.size)
        assertTrue(CargadorTallas.parsear("{}").estaVacia)
    }
}
