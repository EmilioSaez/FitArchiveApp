package app.luxion.fitarchiveapp.tallas

import app.luxion.fitarchiveapp.model.Fit
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
        val r = calcular(m1, "Hollister", GarmentType.TSHIRT, "Q")
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
        assertEquals(24.5, MedidasUsuaria.de(m.copy(footCm = 24.5f)).pie)
    }

    // --- T29: corte ---

    private fun conCorte(m: MedidasUsuaria, tipo: GarmentType, talla: String?, corte: Fit, marca: String? = null) =
        AjusteTalla.calcular(m, marca, tipo, talla, tablas, OpcionesAjuste(corte = corte))

    @Test
    fun corteNormalEsIgualQueSinCorte() {
        val m = m1.copy(pecho = 100.0)
        assertEquals(
            calcular(m, null, GarmentType.SHIRT, "M"),
            conCorte(m, GarmentType.SHIRT, "M", Fit.NORMAL),
        )
        assertEquals(Fit.NORMAL, calcular(m, null, GarmentType.SHIRT, "M").corte)
    }

    @Test
    fun oversizeRecomiendaUnaTallaMenos() {
        // Pecho 90 → M (86-94) en genérica; oversize desplaza +4 → S (82-90 → 86-94).
        assertEquals("M", conCorte(m1, GarmentType.TSHIRT, "M", Fit.NORMAL).tallaIdeal)
        val r = conCorte(m1, GarmentType.TSHIRT, "M", Fit.OVERSIZE)
        assertEquals("S", r.tallaIdeal)
        assertEquals(Fit.OVERSIZE, r.corte)
    }

    @Test
    fun slimRecomiendaUnaTallaMas() {
        val m = MedidasUsuaria(pecho = 93.0, cintura = 74.0)
        // La tabla genérica trae letras y numeración EU; la ideal se da en el sistema de la talla de la prenda.
        assertEquals("40", conCorte(m, GarmentType.TSHIRT, "40", Fit.NORMAL).tallaIdeal)
        assertEquals("42", conCorte(m, GarmentType.TSHIRT, "40", Fit.SLIM).tallaIdeal)
        assertEquals("M", conCorte(m, GarmentType.TSHIRT, "M", Fit.NORMAL).tallaIdeal)
        assertEquals("L", conCorte(m, GarmentType.TSHIRT, "M", Fit.SLIM).tallaIdeal)
    }

    @Test
    fun oversizeToleraMasEstrechezYHolgura() {
        val r = tablas.getValue("generica-eu").prenda("camiseta")!!.talla("M")!![Medida.PECHO]!! // 86-94
        fun et(c: Double, corte: Fit) =
            AjusteTalla.etiquetar(c, r, Medida.PECHO, OpcionesAjuste(corte = corte))
        // Apretada con corte normal (95,1 > 94+1), bien en oversize.
        assertEquals(EtiquetaAjuste.AJUSTADA, et(95.1, Fit.NORMAL))
        assertEquals(EtiquetaAjuste.BIEN, et(95.1, Fit.OVERSIZE))
        assertEquals(EtiquetaAjuste.BIEN, et(100.0, Fit.OVERSIZE))
        assertEquals(EtiquetaAjuste.AJUSTADA, et(100.1, Fit.OVERSIZE))
        // Holgada con corte normal (82,9 < 86-3), bien en oversize.
        assertEquals(EtiquetaAjuste.HOLGADA, et(82.9, Fit.NORMAL))
        assertEquals(EtiquetaAjuste.BIEN, et(81.0, Fit.OVERSIZE))
        assertEquals(EtiquetaAjuste.HOLGADA, et(80.9, Fit.OVERSIZE))
    }

    @Test
    fun slimEsMasEstricto() {
        val r = tablas.getValue("generica-eu").prenda("camiseta")!!.talla("M")!![Medida.PECHO]!! // 86-94
        fun et(c: Double, corte: Fit) =
            AjusteTalla.etiquetar(c, r, Medida.PECHO, OpcionesAjuste(corte = corte))
        // Slim: rango 84-92, márgenes 0 → 92,1 ya aprieta y 83,9 ya sobra; con corte normal siguen bien.
        assertEquals(EtiquetaAjuste.BIEN, et(92.1, Fit.NORMAL))
        assertEquals(EtiquetaAjuste.AJUSTADA, et(92.1, Fit.SLIM))
        assertEquals(EtiquetaAjuste.BIEN, et(83.9, Fit.NORMAL))
        assertEquals(EtiquetaAjuste.HOLGADA, et(83.9, Fit.SLIM))
        assertEquals(EtiquetaAjuste.BIEN, et(84.0, Fit.SLIM))
    }

    @Test
    fun elCorteNoAfectaAlCalzado() {
        val m = MedidasUsuaria(pie = 24.0)
        val normal = conCorte(m, GarmentType.SHOES, "38", Fit.NORMAL)
        assertEquals(normal.porZona, conCorte(m, GarmentType.SHOES, "38", Fit.OVERSIZE).porZona)
        assertEquals(normal.tallaIdeal, conCorte(m, GarmentType.SHOES, "38", Fit.SLIM).tallaIdeal)
    }

    // --- T31: casos reales (docs/pruebas-tallas.md) ---

    @Test
    fun laTallaIdealSeDaEnElSistemaDeLaTallaDeLaPrenda() {
        // Antes la numeración EU competía con las letras: "36" recibía "S" y una "S" slim recibía "40".
        val s = MedidasUsuaria(pecho = 84.0, cintura = 66.0)
        assertEquals("36", calcular(s, null, GarmentType.TSHIRT, "36").tallaIdeal)
        assertEquals("S", calcular(s, null, GarmentType.TSHIRT, "S").tallaIdeal)
        assertTrue(conCorte(m1, GarmentType.TSHIRT, "S", Fit.SLIM).tallaIdeal!! in listOf("S", "M", "L", "XL"))
        assertEquals("M", calcular(MedidasUsuaria(cintura = 70.0), null, GarmentType.TSHIRT, "M").tallaIdeal)
        // Sin talla indicada: el sistema de la primera talla de la tabla (letras en la genérica).
        assertEquals("M", calcular(m1, null, GarmentType.TSHIRT, null).tallaIdeal)
    }

    @Test
    fun letraEnMarcaNumericaUsaLaGenericaEnVezDeFallar() {
        // La app ofrecía solo letras y los pantalones de H&M/Brownie son numéricos: "M" daba TALLA_NO_ENCONTRADA.
        for (marca in listOf("H&M", "Brownie")) {
            val r = calcular(m1, marca, GarmentType.JEANS, "M")
            assertEquals(EstadoAjuste.OK, r.estado, marca)
            assertTrue(r.esGenerica, marca)
            assertEquals("pantalon", r.clavePrenda)
        }
        // Con la talla numérica sigue usando la tabla de la marca.
        assertTrue(!calcular(m1, "H&M", GarmentType.JEANS, "38").esGenerica)
        // Si la talla no existe en ninguna tabla, se mantiene el aviso con la ideal de la marca.
        val r = calcular(m1, "H&M", GarmentType.JEANS, "Q")
        assertEquals(EstadoAjuste.TALLA_NO_ENCONTRADA, r.estado)
        assertTrue(!r.esGenerica)
        assertEquals("38", r.tallaIdeal)
    }

    @Test
    fun laTallaNoDistingueMayusculas() {
        assertEquals(EstadoAjuste.OK, calcular(m1, null, GarmentType.TSHIRT, "m").estado)
        assertEquals(EstadoAjuste.OK, calcular(m1, "Scalpers", GarmentType.TSHIRT, "xl").estado)
    }

    @Test
    fun medidasACeroONegativasCuentanComoAusentes() {
        val m = Measurements(null, 0f, -5f, 0f, null, null, null, 0L, footCm = 0f)
        assertEquals(MedidasUsuaria(), MedidasUsuaria.de(m))
        assertEquals(EstadoAjuste.SIN_MEDIDAS, calcular(MedidasUsuaria.de(m), null, GarmentType.TSHIRT, "M").estado)
    }

    @Test
    fun calzadoUnNumeroDeMasEsBienYDosDeMasEsHolgado() {
        // Pie 22,5 cm → ideal 36 (22,2-22,8). 37 sobra ~0,65 cm; 38 sobra ~1,3 cm.
        val pie = MedidasUsuaria(pie = 22.5)
        assertEquals("36", calcular(pie, null, GarmentType.SHOES, "37").tallaIdeal)
        assertEquals(EtiquetaAjuste.BIEN, calcular(pie, null, GarmentType.SHOES, "36").global)
        assertEquals(EtiquetaAjuste.BIEN, calcular(pie, null, GarmentType.SHOES, "37").global)
        assertEquals(EtiquetaAjuste.HOLGADA, calcular(pie, null, GarmentType.SHOES, "38").global)
        // Un número de menos aprieta.
        assertEquals(EtiquetaAjuste.AJUSTADA, calcular(MedidasUsuaria(pie = 24.5), null, GarmentType.SHOES, "38").global)
    }

    @Test
    fun medidaEntreDosTallasSeDaPorBuenaEnLasDosYLaIdealEsLaMasCercana() {
        // Pecho 86 / cintura 68: justo en el límite S-M de la genérica.
        val m = MedidasUsuaria(pecho = 86.0, cintura = 68.0)
        assertEquals(EtiquetaAjuste.BIEN, calcular(m, null, GarmentType.TSHIRT, "S").global)
        assertEquals(EtiquetaAjuste.BIEN, calcular(m, null, GarmentType.TSHIRT, "M").global)
        // Brownie (valores únicos): 83 queda entre XS (81) y S (85) y vale en ambas.
        val b = MedidasUsuaria(pecho = 83.0, cintura = 66.0, cadera = 91.0)
        assertEquals(EtiquetaAjuste.BIEN, calcular(b, "Brownie", GarmentType.TSHIRT, "XS").global)
        assertEquals(EtiquetaAjuste.BIEN, calcular(b, "Brownie", GarmentType.TSHIRT, "S").global)
    }

    @Test
    fun medidasFueraDeTablaNoRevientanYDanLaTallaExtrema() {
        val grande = MedidasUsuaria(pecho = 130.0, cintura = 110.0, cadera = 140.0)
        val g = calcular(grande, null, GarmentType.TSHIRT, "XL")
        assertEquals(EtiquetaAjuste.AJUSTADA, g.global)
        assertEquals("XXL", g.tallaIdeal)
        val pequena = MedidasUsuaria(pecho = 66.0, cintura = 50.0)
        val p = calcular(pequena, null, GarmentType.TSHIRT, "XS")
        assertEquals(EtiquetaAjuste.HOLGADA, p.global)
        assertEquals("XS", p.tallaIdeal)
    }

    @Test
    fun globalEsLaPeorZonaAunqueOtraSobre() {
        // Vestido M: pecho 92 bien, cintura 64 holgada, cadera 98 bien → holgada.
        val m = MedidasUsuaria(pecho = 92.0, cintura = 64.0, cadera = 98.0)
        assertEquals(EtiquetaAjuste.HOLGADA, calcular(m, null, GarmentType.DRESS, "M").global)
        // Vestido S: pecho aprieta aunque la cintura vaya bien → ajustada.
        assertEquals(EtiquetaAjuste.AJUSTADA, calcular(m, null, GarmentType.DRESS, "S").global)
    }
}
