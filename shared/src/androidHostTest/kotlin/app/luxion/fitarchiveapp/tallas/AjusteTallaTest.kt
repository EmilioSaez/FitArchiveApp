package app.luxion.fitarchiveapp.tallas

import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Measurements
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Prueba el algoritmo con las tablas reales de composeResources/files/tallas. */
class AjusteTallaTest {
    private val dir = File("src/commonMain/composeResources/files/tallas")
    private val tablas: Map<String, TablaTallas> =
        CargadorTallas.IDS.associateWith { CargadorTallas.parsear(File(dir, "$it.json").readText()) }

    private fun calcular(m: MedidasUsuaria, marca: String?, tipo: GarmentType, talla: String?) =
        AjusteTalla.calcular(m, marca, tipo, talla, tablas)

    private val m1 = MedidasUsuaria(pecho = 90.0, cintura = 72.0, cadera = 98.0)

    @Test
    fun genericaBien() {
        val r = calcular(m1, null, GarmentType.TSHIRT, "M")
        assertEquals(EstadoAjuste.OK, r.estado)
        assertEquals(EtiquetaAjuste.BIEN, r.global)
        assertEquals(mapOf(Medida.PECHO to EtiquetaAjuste.BIEN, Medida.CINTURA to EtiquetaAjuste.BIEN), r.porZona)
        assertEquals("M", r.tallaIdeal)
        assertTrue(r.esGenerica)
        assertEquals("camiseta", r.clavePrenda)
    }

    @Test
    fun genericaAjustadaYGlobalLaPeorZona() {
        val r = calcular(m1.copy(pecho = 100.0), null, GarmentType.SHIRT, "M")
        assertEquals(EtiquetaAjuste.AJUSTADA, r.porZona[Medida.PECHO])
        assertEquals(EtiquetaAjuste.BIEN, r.porZona[Medida.CINTURA])
        assertEquals(EtiquetaAjuste.AJUSTADA, r.global)
        assertEquals("L", r.tallaIdeal)
    }

    @Test
    fun genericaHolgada() {
        val r = calcular(MedidasUsuaria(pecho = 80.0, cintura = 62.0), null, GarmentType.TSHIRT, "L")
        assertEquals(EtiquetaAjuste.HOLGADA, r.global)
        assertEquals("XS", r.tallaIdeal)
    }

    @Test
    fun margenes() {
        val r = tablas.getValue("generica-eu").prenda("camiseta")!!.talla("M")!![Medida.PECHO]!! // 86-94
        val o = OpcionesAjuste()
        assertEquals(EtiquetaAjuste.BIEN, AjusteTalla.etiquetar(95.0, r, Medida.PECHO, o))
        assertEquals(EtiquetaAjuste.AJUSTADA, AjusteTalla.etiquetar(95.1, r, Medida.PECHO, o))
        assertEquals(EtiquetaAjuste.BIEN, AjusteTalla.etiquetar(83.0, r, Medida.PECHO, o))
        assertEquals(EtiquetaAjuste.HOLGADA, AjusteTalla.etiquetar(82.9, r, Medida.PECHO, o))
    }

    @Test
    fun marcaConRangosHollister() {
        val r = calcular(MedidasUsuaria(pecho = 89.0), "Hollister", GarmentType.TSHIRT, "M")
        assertEquals("Hollister", r.tablaUsada)
        assertEquals("parte_superior", r.clavePrenda)
        assertEquals(EtiquetaAjuste.BIEN, r.global)
        assertEquals("M", r.tallaIdeal)
    }

    @Test
    fun hollisterNumericaUsSeEligePorLaTalla() {
        val r = calcular(MedidasUsuaria(cintura = 67.0, cadera = 93.0), "Hollister", GarmentType.JEANS, "5")
        assertEquals("parte_inferior_numerica_us", r.clavePrenda)
        assertEquals(EtiquetaAjuste.BIEN, r.global)
        assertEquals("5", r.tallaIdeal)
    }

    @Test
    fun marcaConValoresUnicosBrownie() {
        val m = MedidasUsuaria(pecho = 89.0, cintura = 72.0, cadera = 97.0)
        assertEquals(EtiquetaAjuste.BIEN, calcular(m, "Brownie", GarmentType.SHIRT, "M").global)
        val apretada = calcular(m.copy(pecho = 95.0), "Brownie", GarmentType.SHIRT, "M")
        assertEquals(EtiquetaAjuste.AJUSTADA, apretada.porZona[Medida.PECHO])
        assertEquals("M-L", apretada.tallaIdeal)
    }

    @Test
    fun hmPantalonNumerico() {
        val r = calcular(MedidasUsuaria(cintura = 72.0, cadera = 95.0), "H&M", GarmentType.TROUSERS, "38")
        assertEquals("H&M", r.tablaUsada)
        assertEquals(EtiquetaAjuste.BIEN, r.global)
    }

    @Test
    fun scalpersAbrigoYVestido() {
        assertEquals("abrigos_chaquetas", calcular(m1, "Scalpers", GarmentType.COAT, "S").clavePrenda)
        assertEquals("vestidos", calcular(m1, "Scalpers", GarmentType.DRESS, "M").clavePrenda)
    }

    @Test
    fun marcaVaciaOPendienteCaeEnLaGenerica() {
        for (marca in listOf("Zara", "Pull&Bear", "Massimo Dutti", "Mango", "Brandy Melville", "Primark", null)) {
            val r = calcular(m1, marca, GarmentType.TSHIRT, "M")
            assertTrue(r.esGenerica, marca ?: "null")
            assertEquals(EstadoAjuste.OK, r.estado, marca ?: "null")
        }
    }

    @Test
    fun marcaConTablaPeroSinEseTipoCaeEnLaGenerica() {
        // Brownie no tiene calzado
        val r = calcular(MedidasUsuaria(pie = 24.0), "Brownie", GarmentType.SHOES, "38")
        assertTrue(r.esGenerica)
    }

    @Test
    fun calzado() {
        val r = calcular(MedidasUsuaria(pie = 24.0), "Zara", GarmentType.SHOES, "39")
        assertEquals(EtiquetaAjuste.BIEN, r.global)
        assertEquals("38", r.tallaIdeal)
        val corto = calcular(MedidasUsuaria(pie = 24.8), null, GarmentType.SHOES, "37")
        assertEquals(EtiquetaAjuste.AJUSTADA, corto.porZona[Medida.PIE])
        assertEquals("39", corto.tallaIdeal)
    }

    @Test
    fun medidasAusentes() {
        val parcial = calcular(MedidasUsuaria(pecho = 90.0), null, GarmentType.TSHIRT, "M")
        assertEquals(setOf(Medida.PECHO), parcial.porZona.keys)
        assertEquals(EstadoAjuste.OK, parcial.estado)

        val sin = calcular(MedidasUsuaria(pecho = 90.0), null, GarmentType.TROUSERS, "M")
        assertEquals(EstadoAjuste.SIN_MEDIDAS, sin.estado)
        assertNull(sin.global)
        assertNull(sin.tallaIdeal)

        assertEquals(EstadoAjuste.SIN_MEDIDAS, calcular(MedidasUsuaria(), null, GarmentType.TSHIRT, "M").estado)
        assertEquals(EstadoAjuste.SIN_MEDIDAS, calcular(MedidasUsuaria(pecho = 90.0), null, GarmentType.SHOES, "38").estado)
    }

    @Test
    fun tallaNoEncontradaOSinTalla() {
        val r = calcular(m1, "Hollister", GarmentType.TSHIRT, "XXL")
        assertEquals(EstadoAjuste.TALLA_NO_ENCONTRADA, r.estado)
        assertEquals("M", r.tallaIdeal)
        assertNull(r.global)
        assertEquals(EstadoAjuste.TALLA_NO_ENCONTRADA, calcular(m1, null, GarmentType.TSHIRT, null).estado)
    }

    @Test
    fun noAplicaYSinTabla() {
        assertEquals(EstadoAjuste.NO_APLICA, calcular(m1, null, GarmentType.BAG, "M").estado)
        val sinGenerica = AjusteTalla.calcular(m1, "Zara", GarmentType.TSHIRT, "M", tablas - "generica-eu")
        assertEquals(EstadoAjuste.SIN_TABLA, sinGenerica.estado)
    }

    @Test
    fun todosLosTiposTienenClaveEnLaGenericaOSonAccesorios() {
        for (t in GarmentType.entries) {
            val claves = AjusteTalla.clavesPara(t)
            if (t == GarmentType.BAG || t == GarmentType.ACCESSORY) {
                assertTrue(claves.isEmpty())
            } else {
                assertTrue(tablas.getValue("generica-eu").prendas.keys.any { it in claves }, "genérica sin $t")
            }
        }
    }

    @Test
    fun normalizaMarcas() {
        assertEquals("pullbear", AjusteTalla.normalizarMarca("Pull&Bear"))
        assertEquals(AjusteTalla.normalizarMarca("massimo_dutti"), AjusteTalla.normalizarMarca("Massimo Dutti"))
    }

    @Test
    fun desdeMeasurements() {
        val m = Measurements(165f, 90f, 72f, 98f, null, null, null, 0L)
        assertEquals(MedidasUsuaria(90.0, 72.0, 98.0, null), MedidasUsuaria.de(m))
    }
}
