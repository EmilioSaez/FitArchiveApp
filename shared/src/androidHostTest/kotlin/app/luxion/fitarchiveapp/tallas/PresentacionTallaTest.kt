package app.luxion.fitarchiveapp.tallas

import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.ui.detalle.PresentacionTalla
import app.luxion.fitarchiveapp.ui.detalle.TallaUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PresentacionTallaTest {
    private fun camiseta(vararg tallas: TallaMarca) = mapOf("camiseta" to TablaPrenda("camiseta", tallas.toList()))

    private val tablas = mapOf(
        AjusteTalla.ID_GENERICA to TablaTallas("Genérica"),
        "zara" to TablaTallas("Zara", prendas = camiseta(TallaMarca("M", emptyMap()))),
        "mango" to TablaTallas("Mango", pendiente = listOf("guía")),
    )

    private val generica = ResultadoAjuste(
        EstadoAjuste.OK,
        mapOf(Medida.PECHO to EtiquetaAjuste.BIEN),
        EtiquetaAjuste.BIEN,
        "M",
        "Genérica",
        esGenerica = true,
    )

    @Test
    fun ok_con_zonas_ordenadas_y_talla_ideal() {
        val r = ResultadoAjuste(
            EstadoAjuste.OK,
            porZona = mapOf(Medida.CADERA to EtiquetaAjuste.HOLGADA, Medida.PECHO to EtiquetaAjuste.AJUSTADA),
            global = EtiquetaAjuste.AJUSTADA,
            tallaIdeal = "L",
            tablaUsada = "Zara",
        )
        val ui = assertIs<TallaUi.Resultado>(PresentacionTalla.presentar(r, "Zara", tablas))
        assertEquals("Corte: normal", ui.textoCorte)
        assertEquals("Ajustada", ui.titulo)
        assertEquals(listOf("Pecho", "Cadera"), ui.zonas.map { it.zona })
        assertEquals("Talla ideal en Zara: L", ui.textoTallaIdeal)
        assertNull(ui.nota)
    }

    @Test
    fun generica_sin_marca_y_marca_desconocida() {
        assertTrue(PresentacionTalla.nota(generica, null, tablas)!!.contains("genérica"))
        assertTrue(PresentacionTalla.nota(generica, " Marca X ", tablas)!!.contains("Sin tabla de Marca X"))
    }

    @Test
    fun marca_pendiente_de_datos() {
        assertTrue(PresentacionTalla.nota(generica, "Mango", tablas)!!.contains("pendiente de datos"))
    }

    @Test
    fun marca_sin_ese_tipo_de_prenda() {
        assertTrue(PresentacionTalla.nota(generica, "Zara", tablas)!!.contains("no tiene este tipo"))
    }

    @Test
    fun estados_sin_resultado() {
        assertEquals(TallaUi.Oculta, PresentacionTalla.presentar(ResultadoAjuste(EstadoAjuste.NO_APLICA), null, tablas))
        assertEquals(TallaUi.SinMedidas, PresentacionTalla.presentar(ResultadoAjuste(EstadoAjuste.SIN_MEDIDAS), null, tablas))
        assertIs<TallaUi.Aviso>(PresentacionTalla.presentar(ResultadoAjuste(EstadoAjuste.SIN_TABLA), null, tablas))
        val ui = assertIs<TallaUi.Aviso>(
            PresentacionTalla.presentar(
                ResultadoAjuste(EstadoAjuste.TALLA_NO_ENCONTRADA, tallaIdeal = "S", tablaUsada = "Zara"),
                "Zara",
                tablas,
            ),
        )
        assertEquals("Talla ideal en Zara: S", ui.textoTallaIdeal)
    }

    @Test
    fun integracion_con_el_algoritmo() {
        val t = mapOf(
            AjusteTalla.ID_GENERICA to TablaTallas("Genérica"),
            "zara" to TablaTallas(
                "Zara",
                prendas = camiseta(
                    TallaMarca("S", mapOf(Medida.PECHO to Rango(80.0, 88.0))),
                    TallaMarca("M", mapOf(Medida.PECHO to Rango(90.0, 96.0))),
                ),
            ),
        )
        val r = AjusteTalla.calcular(MedidasUsuaria(pecho = 93.0), "Zara", GarmentType.TSHIRT, "S", t)
        val ui = assertIs<TallaUi.Resultado>(PresentacionTalla.presentar(r, "Zara", t))
        assertEquals("Ajustada", ui.titulo)
        assertEquals("M", ui.tallaIdeal)
    }

    @Test
    fun laTallaIdealDeLaGenericaNoMuestraElNombreCrudo() {
        val ui = PresentacionTalla.presentar(
            ResultadoAjuste(
                EstadoAjuste.TALLA_NO_ENCONTRADA, tallaIdeal = "39", tablaUsada = "generica", esGenerica = true,
            ),
            "Zara",
            tablas,
        ) as TallaUi.Aviso
        assertEquals("Talla ideal (tabla genérica): 39", ui.textoTallaIdeal)
    }
}
