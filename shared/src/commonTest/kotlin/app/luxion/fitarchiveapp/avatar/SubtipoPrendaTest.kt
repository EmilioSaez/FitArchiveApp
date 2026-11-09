package app.luxion.fitarchiveapp.avatar

import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubtipoPrendaTest {
    private fun prenda(tipo: GarmentType = GarmentType.TSHIRT) = Garment(
        1, "p.png", tipo, GarmentColor.BLACK, Season.SUMMER, Usage.CASUAL, null, null,
        favorite = false, dirty = false, wearsSinceWash = 0, totalWears = 0, washAfterWears = null, createdAt = 1,
    )

    @Test
    fun cadaTipoOfreceSuSubtipoPorDefectoPrimeroYSinRepetidos() {
        GarmentType.entries.forEach { tipo ->
            val validos = subtiposValidos(tipo)
            assertEquals(subtipoPorDefecto(tipo), validos.first(), "$tipo")
            assertEquals(validos.size, validos.toSet().size, "$tipo")
        }
    }

    @Test
    fun losSubtiposValidosDeUnTipoCompartenSlotSalvoAccesorios() {
        GarmentType.entries.filter { it != GarmentType.ACCESSORY }.forEach { tipo ->
            assertEquals(setOf(slotDe(tipo)), subtiposValidos(tipo).map { it.slot }.toSet(), "$tipo")
        }
    }

    @Test
    fun camisetaOfreceMangaYLongitud() {
        val validos = subtiposValidos(GarmentType.TSHIRT)
        assertTrue(Subtipo.TOP_SIN_MANGAS_CORTO in validos)
        assertTrue(Subtipo.TOP_MANGA_CORTA in validos)
        assertTrue(Subtipo.TOP_MANGA_LARGA in validos)
        assertTrue(Subtipo.TOP_LARGO_CADERA in validos)
        assertTrue(Subtipo.BOTTOM_SHORTS !in validos)
    }

    @Test
    fun subtipoValidoParaDescartaLoQueNoVale() {
        assertEquals(Subtipo.TOP_MANGA_LARGA, subtipoValidoPara(GarmentType.TSHIRT, Subtipo.TOP_MANGA_LARGA))
        assertNull(subtipoValidoPara(GarmentType.JEANS, Subtipo.TOP_MANGA_LARGA))
        assertNull(subtipoValidoPara(null, Subtipo.TOP_MANGA_LARGA))
        assertNull(subtipoValidoPara(GarmentType.TSHIRT, null))
    }

    @Test
    fun sinProporcionLaSugerenciaEsElSubtipoPorDefecto() {
        GarmentType.entries.forEach { assertEquals(subtipoPorDefecto(it), sugerirSubtipo(it), "$it") }
    }

    @Test
    fun elSubtipoEfectivoEsElGuardadoOElPorDefecto() {
        assertEquals(Subtipo.TOP_MANGA_CORTA, prenda().subtipoEfectivo)
        assertEquals(Subtipo.TOP_MANGA_LARGA, prenda().copy(subtipo = Subtipo.TOP_MANGA_LARGA).subtipoEfectivo)
        assertEquals(AjustePrenda.Ninguno, prenda().ajusteManual)
        assertEquals(AjustePrenda(1f, 2f, 1.2f), prenda().copy(ajuste = AjustePrenda(1f, 2f, 1.2f)).ajusteManual)
    }

    @Test
    fun sumarAjustesSumaDesplazamientosYMultiplicaEscalas() {
        assertEquals(AjustePrenda(4f, -1f, 1.5f), AjustePrenda(1f, 2f, 1.5f) + AjustePrenda(3f, -3f, 1f))
        assertEquals(AjustePrenda(1f, 2f, 1.5f), AjustePrenda(1f, 2f, 1.5f) + AjustePrenda.Ninguno)
    }

    @Test
    fun elAjusteSeAplicaSobreLaCajaDelSubtipoYNoLaSobrepasa() {
        val caja = Subtipo.TOP_MANGA_LARGA.caja
        val base = encajar(caja, 100, 100, Slot.TOP.alineacion)
        assertEquals(base, ajustarEnCaja(base, AjustePrenda.Ninguno, caja))
        assertEquals(ajustar(base, AjustePrenda(10f, -5f, 1.2f)), ajustarEnCaja(base, AjustePrenda(10f, -5f, 1.2f), caja))
        // Un ajuste enorme se limita: el centro se queda en la caja y la escala en 1,5x.
        val r = ajustarEnCaja(base, AjustePrenda(dx = 5000f, dy = -5000f, escala = 10f), caja)
        assertEquals(ajustar(base, AjustePrenda(caja.w / 2f, -caja.h / 2f, AjustePrenda.ESCALA_MAX)), r)
    }
}
