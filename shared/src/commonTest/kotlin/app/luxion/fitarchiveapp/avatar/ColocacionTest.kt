package app.luxion.fitarchiveapp.avatar

import app.luxion.fitarchiveapp.model.GarmentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColocacionTest {
    @Test
    fun tipoASlot() {
        assertEquals(Slot.TOP, slotDe(GarmentType.TSHIRT))
        assertEquals(Slot.TOP, slotDe(GarmentType.SHIRT))
        assertEquals(Slot.TOP, slotDe(GarmentType.SWEATER))
        assertEquals(Slot.OUTER, slotDe(GarmentType.JACKET))
        assertEquals(Slot.OUTER, slotDe(GarmentType.COAT))
        assertEquals(Slot.BOTTOM, slotDe(GarmentType.TROUSERS))
        assertEquals(Slot.BOTTOM, slotDe(GarmentType.JEANS))
        assertEquals(Slot.BOTTOM, slotDe(GarmentType.SKIRT))
        assertEquals(Slot.BOTTOM, slotDe(GarmentType.SHORTS))
        assertEquals(Slot.DRESS, slotDe(GarmentType.DRESS))
        assertEquals(Slot.SHOES, slotDe(GarmentType.SHOES))
        assertEquals(Slot.ACC_HAND, slotDe(GarmentType.BAG))
    }

    @Test
    fun cajasDentroDelLienzo() {
        GarmentType.entries.forEach { tipo -> assertTrue(subtipoPorDefecto(tipo).caja.dentroDelLienzo(), "$tipo") }
        Subtipo.entries.forEach { assertTrue(it.caja.dentroDelLienzo(), "$it") }
    }

    @Test
    fun cajasCoincidenConAnchorsJson() {
        assertEquals(Caja(106f, 172f, 188f, 270f), Subtipo.TOP_MANGA_CORTA.caja)
        assertEquals(Caja(138f, 350f, 124f, 550f), Subtipo.BOTTOM_PANTALON_LARGO.caja)
        assertEquals(Caja(148f, 900f, 104f, 72f), Subtipo.SHOES_PAR.caja)
        assertTrue(Subtipo.SHOES_PIE_IZQ.reflejar)
    }

    @Test
    fun encajarContainArribaCentro() {
        val r = encajar(Caja(0f, 0f, 200f, 100f), 100, 100, Alineacion.ARRIBA_CENTRO)
        assertEquals(Rect(50f, 0f, 100f, 100f), r)
    }

    @Test
    fun encajarAlineacionVertical() {
        val caja = Caja(10f, 100f, 100f, 200f)
        assertEquals(Rect(10f, 100f, 100f, 50f), encajar(caja, 200, 100, Alineacion.ARRIBA_CENTRO))
        assertEquals(Rect(10f, 250f, 100f, 50f), encajar(caja, 200, 100, Alineacion.ABAJO_CENTRO))
        assertEquals(Rect(10f, 175f, 100f, 50f), encajar(caja, 200, 100, Alineacion.CENTRO))
    }

    @Test
    fun encajarAmpliaImagenesPequenas() {
        assertEquals(Rect(0f, 0f, 100f, 50f), encajar(Caja(0f, 0f, 100f, 100f), 10, 5, Alineacion.ARRIBA_CENTRO))
    }

    @Test
    fun encajarImagenVaciaNoFalla() {
        assertEquals(0f, encajar(Caja(0f, 0f, 10f, 10f), 0, 0, Alineacion.CENTRO).w)
    }

    @Test
    fun ordenSinPrendasEsElDelAvatar() {
        assertEquals(
            CapaAvatar.entries.sortedBy { it.z }.map { Capa.Avatar(it) },
            capasOrdenadas(emptySet()),
        )
    }

    @Test
    fun ordenIntercalaPrendasPorZ() {
        val orden = capasOrdenadas(Slot.entries.toSet() - Slot.DRESS)
        val ids = orden.map { if (it is Capa.Prenda) it.slot.name else (it as Capa.Avatar).capa.name }
        assertEquals(
            listOf(
                "BODY", "ARMS", "SHOES", "BOTTOM", "TOP", "OUTER", "ACC_WAIST", "ACC_NECK",
                "ACC_FACE", "ACC_HEAD", "ACC_HAND",
            ),
            ids,
        )
    }

    @Test
    fun vestidoExcluyeTopYBottom() {
        val orden = capasOrdenadas(setOf(Slot.DRESS, Slot.TOP, Slot.BOTTOM, Slot.SHOES, Slot.OUTER))
        assertEquals(listOf(Slot.SHOES, Slot.DRESS, Slot.OUTER), orden.filterIsInstance<Capa.Prenda>().map { it.slot })
    }

    @Test
    fun pantalonCaeSobreCalzadoYChaquetaSobreCamiseta() {
        val slots = capasOrdenadas(setOf(Slot.TOP, Slot.OUTER, Slot.BOTTOM, Slot.SHOES)).filterIsInstance<Capa.Prenda>().map { it.slot }
        assertEquals(listOf(Slot.SHOES, Slot.BOTTOM, Slot.TOP, Slot.OUTER), slots)
    }

    private fun Caja.dentroDelLienzo() = x >= 0 && y >= 0 && x + w <= LIENZO_ANCHO && y + h <= LIENZO_ALTO

    @Test
    fun vestidoConservaChaquetaYCalzado() {
        val capas = capasOrdenadas(setOf(Slot.DRESS, Slot.TOP, Slot.BOTTOM, Slot.OUTER, Slot.SHOES))
        val slots = capas.filterIsInstance<Capa.Prenda>().map { it.slot }
        assertEquals(listOf(Slot.SHOES, Slot.DRESS, Slot.OUTER), slots)
    }
}
