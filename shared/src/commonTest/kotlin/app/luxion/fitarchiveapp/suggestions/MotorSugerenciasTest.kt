package app.luxion.fitarchiveapp.suggestions

import app.luxion.fitarchiveapp.model.Fit
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentColor.*
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.GarmentType.*
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.model.WearRecord
import app.luxion.fitarchiveapp.weather.CondicionTiempo
import app.luxion.fitarchiveapp.weather.TiempoDia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MotorSugerenciasTest {
    private val motor = MotorSugerenciasPorReglas()
    private val hoy = "2026-11-02"

    private fun prenda(
        id: Long,
        tipo: GarmentType,
        color: GarmentColor = BLACK,
        season: Season = Season.MID_SEASON,
        usage: Usage = Usage.CASUAL,
        dirty: Boolean = false,
    ) = Garment(
        id = id, photoPath = "p$id", type = tipo, color = color, season = season, usage = usage,
        brand = null, size = null, favorite = false, dirty = dirty, wearsSinceWash = 0, totalWears = 0,
        washAfterWears = null, createdAt = 0, fit = Fit.NORMAL,
    )

    private fun tiempo(c: Double, condicion: CondicionTiempo = CondicionTiempo.DESPEJADO) =
        TiempoDia(temperaturaC = c, maximaC = c + 2, minimaC = c - 2, condicion = condicion)

    private fun pedido(armario: List<Garment>, block: (PedidoSugerencia) -> PedidoSugerencia = { it }) =
        block(PedidoSugerencia(armario = armario, hoy = hoy))

    private fun basico() = listOf(prenda(1, TSHIRT, WHITE), prenda(2, JEANS, NAVY))

    @Test
    fun armarioVacioODeCompletarNoDaNada() {
        assertTrue(motor.sugerir(pedido(emptyList())).isEmpty())
        assertTrue(motor.sugerir(pedido(listOf(prenda(1, TSHIRT)))).isEmpty())
        assertTrue(motor.sugerir(pedido(listOf(prenda(1, SHOES), prenda(2, BAG)))).isEmpty())
    }

    @Test
    fun superiorMasInferiorOVestidoFormanOutfit() {
        val r = motor.sugerir(pedido(basico()))
        assertEquals(listOf(listOf(1L, 2L)), r.map { o -> o.prendas.map { it.id } })
        val vestido = motor.sugerir(pedido(listOf(prenda(5, DRESS))))
        assertEquals(listOf(5L), vestido.single().prendas.map { it.id })
    }

    @Test
    fun excluyeLasSucias() {
        val r = motor.sugerir(pedido(basico() + prenda(3, SHIRT, BLUE, dirty = true)))
        assertTrue(r.all { o -> o.prendas.none { it.id == 3L } })
        assertTrue(motor.sugerir(pedido(listOf(prenda(1, TSHIRT, dirty = true), prenda(2, JEANS)))).isEmpty())
    }

    @Test
    fun incluyeCalzadoSiHayYNuncaBolsosNiAccesorios() {
        val r = motor.sugerir(pedido(basico() + prenda(3, SHOES) + prenda(4, BAG) + prenda(5, ACCESSORY)))
        assertEquals(listOf(1L, 2L, 3L), r.single().prendas.map { it.id })
    }

    @Test
    fun conCalorDescartaPrendasDeInviernoYCapas() {
        val armario = listOf(
            prenda(1, TSHIRT, season = Season.SUMMER), prenda(2, SHORTS, season = Season.SUMMER),
            prenda(3, SWEATER, season = Season.WINTER), prenda(4, JACKET, season = Season.MID_SEASON),
        )
        val r = motor.sugerir(pedido(armario) { it.copy(tiempo = tiempo(30.0)) })
        assertEquals(listOf(listOf(1L, 2L)), r.map { o -> o.prendas.map { it.id } })
    }

    @Test
    fun conFrioPrefiereInviernoYAñadeCapa() {
        val armario = listOf(
            prenda(1, SWEATER, season = Season.WINTER), prenda(2, TSHIRT, season = Season.SUMMER),
            prenda(3, TROUSERS, season = Season.WINTER), prenda(4, COAT, season = Season.WINTER),
        )
        val r = motor.sugerir(pedido(armario) { it.copy(tiempo = tiempo(4.0)) })
        assertEquals(setOf(1L, 3L, 4L), r.first().prendas.map { it.id }.toSet())
        assertTrue(r.all { o -> o.prendas.none { it.id == 2L } })
    }

    @Test
    fun lluviaPideCapaAunqueNoHagaFrio() {
        val armario = basico() + prenda(3, JACKET, season = Season.MID_SEASON)
        val r = motor.sugerir(pedido(armario) { it.copy(tiempo = tiempo(18.0, CondicionTiempo.LLUVIA)) })
        assertTrue(r.first().prendas.any { it.id == 3L })
        val seco = motor.sugerir(pedido(armario) { it.copy(tiempo = tiempo(18.0)) })
        assertTrue(seco.any { o -> o.prendas.none { it.id == 3L } })
    }

    @Test
    fun filtraPorOcasion() {
        val armario = listOf(
            prenda(1, SHIRT, WHITE, usage = Usage.WORK), prenda(2, TROUSERS, NAVY, usage = Usage.WORK),
            prenda(3, TSHIRT, GREY, usage = Usage.SPORT), prenda(4, SHORTS, BLACK, usage = Usage.SPORT),
        )
        val trabajo = motor.sugerir(pedido(armario) { it.copy(ocasion = Occasion.WORK) })
        assertEquals(listOf(listOf(1L, 2L)), trabajo.map { o -> o.prendas.map { it.id } })
        val deporte = motor.sugerir(pedido(armario) { it.copy(ocasion = Occasion.SPORT) })
        assertEquals(listOf(listOf(3L, 4L)), deporte.map { o -> o.prendas.map { it.id } })
    }

    @Test
    fun combinacionDeColoresPuntuaYDescartaChoques() {
        val armario = listOf(prenda(1, TSHIRT, RED), prenda(2, SKIRT, PINK), prenda(3, JEANS, NAVY))
        val r = motor.sugerir(pedido(armario))
        // Rojo + rosa choca: solo queda rojo + navy.
        assertEquals(listOf(listOf(1L, 3L)), r.map { o -> o.prendas.map { it.id } })
        assertEquals(1.0, r.single().desglose.colores)
    }

    @Test
    fun siTodoChocaSeSugiereIgualConPuntuacionBaja() {
        val r = motor.sugerir(pedido(listOf(prenda(1, TSHIRT, RED), prenda(2, SKIRT, PINK))))
        assertEquals(1, r.size)
        assertTrue(r.single().desglose.colores < ColorRules.UMBRAL_COMBINA)
    }

    @Test
    fun evitaRepetirLoUsadoRecientemente() {
        val armario = basico() + listOf(prenda(3, SHIRT, WHITE), prenda(4, TROUSERS, NAVY))
        val usos = listOf(WearRecord("2026-11-01", 1), WearRecord("2026-11-01", 2))
        val r = motor.sugerir(pedido(armario) { it.copy(usos = usos) })
        assertEquals(setOf(3L, 4L), r.first().prendas.map { it.id }.toSet())
        // Sin historial no hay preferencia entre ambos: desempata por id.
        assertEquals(setOf(1L, 2L), motor.sugerir(pedido(armario)).first().prendas.map { it.id }.toSet())
    }

    @Test
    fun frescuraSubeConLosDiasYUsaElUltimoUso() {
        val usos = listOf(WearRecord("2026-10-01", 1), WearRecord("2026-10-30", 1), WearRecord("fecha-rota", 2))
        val r = motor.sugerir(pedido(basico()) { it.copy(usos = usos) }).single()
        // Prenda 1: hace 3 días; prenda 2: sin uso válido.
        assertEquals((3.0 / 7 + 1.0) / 2, r.desglose.frescura, 1e-9)
        val hoyMismo = motor.sugerir(pedido(basico()) { it.copy(usos = listOf(WearRecord(hoy, 1))) }).single()
        assertEquals(0.5, hoyMismo.desglose.frescura, 1e-9)
    }

    @Test
    fun ordenaPorPuntuacionYRespetaElMaximo() {
        val armario = listOf(
            prenda(1, TSHIRT, WHITE), prenda(2, TSHIRT, RED), prenda(3, TSHIRT, GREEN),
            prenda(4, JEANS, NAVY), prenda(5, TROUSERS, BLACK), prenda(6, SKIRT, PINK),
        )
        val r = motor.sugerir(pedido(armario) { it.copy(maximo = 3) })
        assertEquals(3, r.size)
        assertEquals(r.map { it.puntuacion }.sortedDescending(), r.map { it.puntuacion })
        assertTrue(r.all { it.puntuacion in 0.0..1.0 })
        assertTrue(motor.sugerir(pedido(armario) { it.copy(maximo = 0) }).isEmpty())
    }

    @Test
    fun unaPrendaNoProtagonizaTodasLasSugerencias() {
        val armario = listOf(prenda(1, TSHIRT, WHITE)) + (2L..6L).map { prenda(it, JEANS, NAVY) }
        val r = motor.sugerir(pedido(armario) { it.copy(maximo = 10) })
        assertEquals(2, r.count { o -> o.prendas.any { it.id == 1L } })
    }

    @Test
    fun esDeterminista() {
        val armario = (1L..4L).map { prenda(it, TSHIRT) } + (5L..8L).map { prenda(it, JEANS) }
        assertEquals(motor.sugerir(pedido(armario)), motor.sugerir(pedido(armario.reversed())))
    }

    @Test
    fun afinidadSubeLaPuntuacionYSeIncluyeEnElDesglose() {
        val armario = basico() + listOf(prenda(3, SHIRT, WHITE), prenda(4, TROUSERS, NAVY))
        val gusta = AfinidadEstilo { prendas -> if (prendas.any { it.id == 4L }) 1.0 else 0.0 }
        val r = motor.sugerir(pedido(armario) { it.copy(afinidad = gusta) })
        assertTrue(r.first().prendas.any { it.id == 4L })
        assertEquals(1.0, r.first().desglose.afinidad)
        assertEquals(null, motor.sugerir(pedido(armario)).first().desglose.afinidad)
        assertFalse(r.first().puntuacion == r.last().puntuacion)
    }

    @Test
    fun afinidadFueraDeRangoSeAcota() {
        val r = motor.sugerir(pedido(basico()) { it.copy(afinidad = AfinidadEstilo { 5.0 }) })
        assertEquals(1.0, r.single().desglose.afinidad)
    }

    @Test
    fun encajeDeClimaYOcasion() {
        assertEquals(Season.WINTER, MotorSugerenciasPorReglas.estacionPara(5.0))
        assertEquals(Season.MID_SEASON, MotorSugerenciasPorReglas.estacionPara(17.0))
        assertEquals(Season.SUMMER, MotorSugerenciasPorReglas.estacionPara(28.0))
        assertEquals(0.0, MotorSugerenciasPorReglas.encajeClima(Season.SUMMER, 3.0))
        assertEquals(0.6, MotorSugerenciasPorReglas.encajeClima(Season.MID_SEASON, 3.0))
        assertEquals(0.4, MotorSugerenciasPorReglas.encajeClima(Season.WINTER, 17.0))
        assertEquals(1.0, MotorSugerenciasPorReglas.encajeOcasion(Usage.PARTY, null))
        assertEquals(0.0, MotorSugerenciasPorReglas.encajeOcasion(Usage.SPORT, Occasion.WORK))
    }
}
