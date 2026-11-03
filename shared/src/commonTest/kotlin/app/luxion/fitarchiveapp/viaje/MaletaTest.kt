package app.luxion.fitarchiveapp.viaje

import app.luxion.fitarchiveapp.data.OutfitWithGarments
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.TripGarment
import app.luxion.fitarchiveapp.model.Usage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MaletaTest {
    private fun prenda(id: Long, tipo: GarmentType = GarmentType.TSHIRT, sucia: Boolean = false) = Garment(
        id = id, photoPath = "p$id.png", type = tipo, color = GarmentColor.BLACK, season = Season.SUMMER,
        usage = Usage.CASUAL, brand = null, size = null, favorite = false, dirty = sucia, wearsSinceWash = 0,
        totalWears = 0, washAfterWears = null, createdAt = id,
    )

    private fun outfit(id: Long, nombre: String, vararg prendas: Long) = OutfitWithGarments(
        Outfit(id, nombre, Occasion.CASUAL, favorite = false, createdAt = id),
        prendas.map { OutfitGarment(id, it, 0f, 0f, 1f, 0) },
    )

    private fun valido(r: ResultadoViaje) = (r as ResultadoViaje.Valido).datos
    private fun error(r: ResultadoViaje) = (r as ResultadoViaje.Invalido).mensaje

    // --- validarViaje ---

    @Test
    fun viajeCompletoSeNormaliza() {
        val d = valido(validarViaje("  Escapada ", " Lisboa ", "2026-12-01", " 2026-12-05"))
        assertEquals(DatosViaje("Escapada", "Lisboa", "2026-12-01", "2026-12-05"), d)
    }

    @Test
    fun sinNombreUsaElDestino() {
        assertEquals("Roma", valido(validarViaje("", "Roma", "", "")).nombre)
    }

    @Test
    fun sinNombreNiDestinoEsInvalido() {
        assertTrue(error(validarViaje(" ", "  ", "", "")).isNotEmpty())
    }

    @Test
    fun lasFechasSonOpcionalesYSeGuardanComoNull() {
        val d = valido(validarViaje("Finde", "", "", ""))
        assertNull(d.destino)
        assertNull(d.inicio)
        assertNull(d.fin)
    }

    @Test
    fun fechaMalFormadaOInexistenteEsInvalida() {
        assertTrue("inicio" in error(validarViaje("V", "", "01/12/2026", "")))
        assertTrue("inicio" in error(validarViaje("V", "", "2026-02-30", "")))
        assertTrue("fin" in error(validarViaje("V", "", "2026-12-01", "mañana")))
    }

    @Test
    fun laVueltaNoPuedeSerAnteriorALaIda() {
        assertTrue(error(validarViaje("V", "", "2026-12-05", "2026-12-01")).isNotEmpty())
        assertEquals("2026-12-05", valido(validarViaje("V", "", "2026-12-05", "2026-12-05")).fin)
    }

    @Test
    fun laVueltaExigeIda() {
        assertTrue("inicio" in error(validarViaje("V", "", "", "2026-12-05")))
    }

    @Test
    fun soloIdaEsValido() {
        assertEquals("2026-12-05", valido(validarViaje("V", "", "2026-12-05", "")).inicio)
    }

    // --- formato de fechas (O10) ---

    @Test
    fun fechasDelViajeSeMuestranEnFormatoCorto() {
        assertEquals("10 dic → 14 dic 2026 · 5 días", formatearFechasViaje("2026-12-10", "2026-12-14"))
        assertEquals("10 dic → 10 dic 2026 · 1 día", formatearFechasViaje("2026-12-10", "2026-12-10"))
        assertEquals("30 dic 2026 → 1 ene 2027 · 3 días", formatearFechasViaje("2026-12-30", "2027-01-01"))
        assertEquals("10 dic 2026", formatearFechasViaje("2026-12-10", null))
        assertNull(formatearFechasViaje(null, null))
        assertNull(formatearFechasViaje(null, "2026-12-10"))
    }

    @Test
    fun fechaCortaConservaElTextoSiNoEsValido() {
        assertEquals("x", formatearFechaCorta("x"))
        assertEquals("5 mar", formatearFechaCorta("2026-03-05", conAnio = false))
    }

    // --- diasDeViaje ---

    @Test
    fun diasCuentaIdaYVuelta() {
        assertEquals(1, diasDeViaje("2026-12-05", "2026-12-05"))
        assertEquals(5, diasDeViaje("2026-12-01", "2026-12-05"))
        assertEquals(3, diasDeViaje("2026-12-30", "2027-01-01"))
    }

    @Test
    fun diasEsNullSiFaltanFechasOSonInvalidas() {
        assertNull(diasDeViaje(null, "2026-12-05"))
        assertNull(diasDeViaje("2026-12-05", null))
        assertNull(diasDeViaje("2026-12-05", "2026-12-01"))
        assertNull(diasDeViaje("x", "2026-12-01"))
    }

    // --- construirMaleta ---

    @Test
    fun maletaVaciaSinOutfitsNiPrendas() {
        val m = construirMaleta(emptyList(), emptyList(), listOf(prenda(1)))
        assertEquals(0, m.total)
        assertFalse(m.completa)
    }

    @Test
    fun uneLasPrendasDeLosOutfitsSinRepetirYRecuerdaDeQueOutfit() {
        val armario = listOf(prenda(1), prenda(2, GarmentType.JEANS), prenda(3, GarmentType.SHOES))
        val m = construirMaleta(listOf(outfit(10, "Día", 1, 2), outfit(11, "Noche", 2, 3)), emptyList(), armario)
        assertEquals(listOf(1L, 2L, 3L), m.items.map { it.prenda.id })
        assertEquals(listOf("Día", "Noche"), m.items.first { it.prenda.id == 2L }.outfits)
        assertTrue(m.items.none { it.manual || it.guardada })
    }

    @Test
    fun incluyeLasPrendasAnadidasAMano() {
        val m = construirMaleta(
            listOf(outfit(10, "Día", 1)),
            listOf(TripGarment(5, 2, packed = false)),
            listOf(prenda(1), prenda(2)),
        )
        val extra = m.items.first { it.prenda.id == 2L }
        assertTrue(extra.manual)
        assertTrue(extra.outfits.isEmpty())
        assertFalse(m.items.first { it.prenda.id == 1L }.manual)
    }

    @Test
    fun elEstadoGuardadaSaleDeLaListaDelViaje() {
        val m = construirMaleta(
            listOf(outfit(10, "Día", 1, 2)),
            listOf(TripGarment(5, 1, packed = true), TripGarment(5, 2, packed = false)),
            listOf(prenda(1), prenda(2)),
        )
        assertEquals(1, m.guardadas)
        assertEquals(2, m.total)
        assertFalse(m.completa)
        val todas = construirMaleta(listOf(outfit(10, "Día", 1)), listOf(TripGarment(5, 1, packed = true)), listOf(prenda(1)))
        assertTrue(todas.completa)
    }

    @Test
    fun ignoraPrendasQueYaNoExistenEnElArmario() {
        val m = construirMaleta(listOf(outfit(10, "Día", 1, 99)), listOf(TripGarment(5, 98, false)), listOf(prenda(1)))
        assertEquals(listOf(1L), m.items.map { it.prenda.id })
    }

    @Test
    fun ordenaPorTipoYLuegoPorAntiguedad() {
        val armario = listOf(prenda(1, GarmentType.SHOES), prenda(2, GarmentType.TSHIRT), prenda(3, GarmentType.TSHIRT))
        val m = construirMaleta(listOf(outfit(10, "Día", 1, 3, 2)), emptyList(), armario)
        assertEquals(listOf(2L, 3L, 1L), m.items.map { it.prenda.id })
    }

    @Test
    fun avisaDeLasPrendasSucias() {
        val m = construirMaleta(listOf(outfit(10, "Día", 1, 2)), emptyList(), listOf(prenda(1, sucia = true), prenda(2)))
        assertEquals(listOf(1L), m.sucias.map { it.prenda.id })
    }
}
