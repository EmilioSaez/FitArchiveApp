package app.luxion.fitarchiveapp.suggestions

import app.luxion.fitarchiveapp.model.Fit
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentColor.*
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.GarmentType.*
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.model.WearRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AfinidadAprendidaTest {
    private fun prenda(
        id: Long,
        tipo: GarmentType = TSHIRT,
        color: GarmentColor = BLACK,
        usage: Usage = Usage.CASUAL,
        brand: String? = null,
        favorite: Boolean = false,
    ) = Garment(
        id = id, photoPath = "p$id", type = tipo, color = color, season = Season.MID_SEASON, usage = usage,
        brand = brand, size = null, favorite = favorite, dirty = false, wearsSinceWash = 0, totalWears = 0,
        washAfterWears = null, createdAt = 0, fit = Fit.NORMAL,
    )

    private fun armario() = listOf(
        prenda(1, color = BLACK, favorite = true, brand = "Zara"),
        prenda(2, color = RED),
        prenda(3, color = BLUE),
        prenda(4, color = GREEN),
        prenda(5, color = WHITE),
        prenda(6, color = BEIGE),
    )

    private fun sinLikes() = armario().map { it.copy(favorite = false) }

    @Test
    fun sinDatosEsNeutral() {
        assertEquals(0.5, AfinidadAprendida.desde(sinLikes()).puntuar(armario()), 1e-9)
        assertEquals(0.5, AfinidadAprendida.desde(emptyList()).puntuar(armario()), 1e-9)
    }

    @Test
    fun conjuntoVacioEsNeutral() {
        assertEquals(0.5, AfinidadAprendida.desde(armario()).puntuar(emptyList()), 1e-9)
    }

    @Test
    fun puntuaMasLoQueSeParecaALoQueGusta() {
        val a = AfinidadAprendida.desde(armario())
        val parecida = a.puntuar(listOf(prenda(10, color = BLACK, brand = " zara ")))
        val distinta = a.puntuar(listOf(prenda(11, color = RED)))
        assertTrue(parecida > 0.5, "parecida=$parecida")
        assertTrue(distinta < parecida, "distinta=$distinta")
        assertTrue(parecida in 0.0..1.0 && distinta in 0.0..1.0)
    }

    @Test
    fun outfitsGuardadosCuentanYLosFavoritosMas() {
        val normal = AfinidadAprendida.desde(sinLikes(), outfits = listOf(OutfitGuardado(listOf(2), favorito = false)))
        val fav = AfinidadAprendida.desde(sinLikes(), outfits = listOf(OutfitGuardado(listOf(2), favorito = true)))
        val rojo = listOf(prenda(10, color = RED))
        assertTrue(normal.puntuar(rojo) > 0.5)
        assertTrue(fav.puntuar(rojo) > normal.puntuar(rojo))
    }

    @Test
    fun elUsoCuentaConTope() {
        fun usos(n: Int) = List(n) { WearRecord("2026-10-${(it % 28 + 1).toString().padStart(2, '0')}", 3) }
        val azul = listOf(prenda(10, color = BLUE))
        val pocos = AfinidadAprendida.desde(sinLikes(), usos = usos(2)).puntuar(azul)
        val muchos = AfinidadAprendida.desde(sinLikes(), usos = usos(8)).puntuar(azul)
        val demasiados = AfinidadAprendida.desde(sinLikes(), usos = usos(30)).puntuar(azul)
        assertTrue(pocos > 0.5, "pocos=$pocos")
        assertTrue(muchos > pocos)
        assertEquals(muchos, demasiados, 1e-9)
    }

    @Test
    fun armarioPequenoSuavizaUnSoloLike() {
        val chico = listOf(prenda(1, color = BLACK, favorite = true), prenda(2, color = RED))
        val negro = listOf(prenda(10, color = BLACK))
        val a = AfinidadAprendida.desde(chico).puntuar(negro)
        assertTrue(a > 0.5 && a < 0.95, "a=$a")
    }

    @Test
    fun ignoraIdsDesconocidos() {
        val a = AfinidadAprendida.desde(
            sinLikes(),
            outfits = listOf(OutfitGuardado(listOf(999), true)),
            usos = listOf(WearRecord("2026-10-01", 888)),
        )
        assertEquals(0.5, a.puntuar(sinLikes()), 1e-9)
    }

    @Test
    fun sinMarcaNoPenaliza() {
        val s = AfinidadAprendida.desde(armario()).puntuar(listOf(prenda(10, color = BLACK, brand = null)))
        assertTrue(s > 0.5, "s=$s")
    }

    @Test
    fun seEnganchaAlMotor() {
        val armario = listOf(
            prenda(1, TSHIRT, BLACK, favorite = true), prenda(2, TSHIRT, RED),
            prenda(3, JEANS, BLUE), prenda(4, SHOES, WHITE),
        )
        val r = MotorSugerenciasPorReglas().sugerir(
            PedidoSugerencia(armario = armario, hoy = "2026-11-02", afinidad = AfinidadAprendida.desde(armario)),
        )
        assertTrue(r.isNotEmpty())
        assertNotNull(r.first().desglose.afinidad)
    }
}
