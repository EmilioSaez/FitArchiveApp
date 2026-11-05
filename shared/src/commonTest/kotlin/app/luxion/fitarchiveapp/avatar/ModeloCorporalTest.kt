package app.luxion.fitarchiveapp.avatar

import app.luxion.fitarchiveapp.model.Fit
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Measurements
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModeloCorporalTest {
    private fun medidas(alto: Float? = null, pecho: Float? = null) =
        Measurements(alto, pecho, null, null, null, null, null, 0L)

    @Test
    fun sinMedidasUsaReferencia() {
        val p = PerfilCorporal.de(null)
        assertEquals(PerfilCorporal.REFERENCIA_ALTURA, p.alturaCm)
        assertTrue(p.reales.isEmpty())
    }

    @Test
    fun medidasFueraDeRangoSeIgnoran() {
        val p = PerfilCorporal.de(medidas(alto = 17f, pecho = 96f))
        assertEquals(PerfilCorporal.REFERENCIA_ALTURA, p.alturaCm)
        assertEquals(96.0, p.pechoCm)
        assertEquals(setOf(MedidaCuerpo.PECHO), p.reales)
    }

    @Test
    fun escalaMidePersonaCompleta() {
        val e = EscalaAvatar(PerfilCorporal(alturaCm = 180.0))
        assertEquals(EscalaAvatar.SUELO.toFloat() - EscalaAvatar.CORONILLA.toFloat(), e.u(180.0), 0.01f)
        // una persona más alta tiene menos unidades por cm
        assertTrue(e.unidadesPorCm < EscalaAvatar(PerfilCorporal(alturaCm = 160.0)).unidadesPorCm)
    }

    @Test
    fun puntosDeReferenciaEstanOrdenados() {
        val e = EscalaAvatar(PerfilCorporal())
        assertTrue(e.yBarbilla < e.yHombros && e.yHombros < e.yPecho && e.yPecho < e.yCintura)
        assertTrue(e.yCintura < e.yCadera && e.yCadera < e.yEntrepierna && e.yEntrepierna < e.yRodilla)
        assertTrue(e.yRodilla < e.yTobillo && e.yTobillo < EscalaAvatar.SUELO.toFloat())
    }

    @Test
    fun holguraDelCorteAgrandaLaPrenda() {
        val cuerpo = PerfilCorporal()
        fun dim(c: Fit) = DimensionesPrendaCalc.calcular(Subtipo.TOP_MANGA_CORTA, GarmentType.TSHIRT, null, null, c, cuerpo, emptyMap())
        assertTrue(dim(Fit.SLIM).pechoCm!! < dim(Fit.NORMAL).pechoCm!!)
        assertTrue(dim(Fit.NORMAL).pechoCm!! < dim(Fit.OVERSIZE).pechoCm!!)
        assertEquals(FuenteDimensiones.REFERENCIA, dim(Fit.NORMAL).fuente)
    }

    @Test
    fun prendaMasLargaOcupaMasAlto() {
        val e = EscalaAvatar(PerfilCorporal())
        fun alto(s: Subtipo): Float {
            val d = DimensionesPrendaCalc.calcular(s, GarmentType.SKIRT, null, null, Fit.NORMAL, PerfilCorporal(), emptyMap())
            return colocacionReal(s, d, e, 100, 100)!!.h
        }
        assertTrue(alto(Subtipo.BOTTOM_FALDA_MINI) < alto(Subtipo.BOTTOM_FALDA_MIDI))
        assertTrue(alto(Subtipo.BOTTOM_FALDA_MIDI) < alto(Subtipo.BOTTOM_FALDA_LARGA))
    }

    @Test
    fun mismaPrendaEsMasPequenaEnPersonaAlta() {
        val s = Subtipo.TOP_MANGA_LARGA
        fun alto(h: Double): Float {
            val c = PerfilCorporal(alturaCm = h)
            val d = DimensionesPrendaCalc.calcular(s, GarmentType.SHIRT, null, null, Fit.NORMAL, c, emptyMap())
            return colocacionReal(s, d, EscalaAvatar(c), 100, 200)!!.h
        }
        assertTrue(alto(190.0) < alto(155.0))
    }

    @Test
    fun accesoriosNoVanAEscalaReal() {
        val d = DimensionesPrendaCalc.calcular(Subtipo.ACC_COLLAR, GarmentType.ACCESSORY, null, null, Fit.NORMAL, PerfilCorporal(), emptyMap())
        assertNull(colocacionReal(Subtipo.ACC_COLLAR, d, EscalaAvatar(PerfilCorporal()), 10, 10))
    }

    @Test
    fun calzadoApoyaEnElSuelo() {
        val d = DimensionesPrendaCalc.calcular(Subtipo.SHOES_PAR, GarmentType.SHOES, null, null, Fit.NORMAL, PerfilCorporal(), emptyMap())
        val r = assertNotNull(colocacionReal(Subtipo.SHOES_PAR, d, EscalaAvatar(PerfilCorporal()), 200, 100))
        assertEquals(EscalaAvatar.SUELO.toFloat(), r.y + r.h, 6f)
    }
}
