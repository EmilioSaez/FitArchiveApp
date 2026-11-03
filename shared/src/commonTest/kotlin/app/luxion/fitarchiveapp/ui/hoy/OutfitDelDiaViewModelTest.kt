package app.luxion.fitarchiveapp.ui.hoy

import app.luxion.fitarchiveapp.data.CalendarRepository
import app.luxion.fitarchiveapp.data.GarmentRepositoryEnMemoria
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.data.OutfitWithGarments
import app.luxion.fitarchiveapp.data.UsoRepository
import app.luxion.fitarchiveapp.model.CalendarDay
import app.luxion.fitarchiveapp.model.Fit
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.model.WearRecord
import app.luxion.fitarchiveapp.suggestions.DesgloseSugerencia
import app.luxion.fitarchiveapp.suggestions.MotorSugerencias
import app.luxion.fitarchiveapp.suggestions.MotorSugerenciasPorReglas
import app.luxion.fitarchiveapp.suggestions.OutfitSugerido
import app.luxion.fitarchiveapp.suggestions.PedidoSugerencia
import app.luxion.fitarchiveapp.weather.Ciudad
import app.luxion.fitarchiveapp.weather.CondicionTiempo
import app.luxion.fitarchiveapp.weather.Coordenadas
import app.luxion.fitarchiveapp.weather.ErrorTiempo
import app.luxion.fitarchiveapp.weather.ServicioTiempo
import app.luxion.fitarchiveapp.weather.TiempoDia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class CalendarioEnMemoria : CalendarRepository {
    val datos = MutableStateFlow<Map<String, CalendarDay>>(emptyMap())
    var falla = false

    override fun observe(date: String): Flow<CalendarDay?> = datos.map { it[date] }
    override fun observeBetween(from: String, to: String): Flow<List<CalendarDay>> =
        datos.map { m -> m.values.filter { it.date in from..to }.sortedBy { it.date } }

    override suspend fun set(day: CalendarDay) {
        if (falla) error("db")
        datos.value += day.date to day
    }

    override suspend fun clear(date: String) {
        datos.value -= date
    }
}

private class OutfitsEnMemoria : OutfitRepository {
    val creados = mutableListOf<OutfitWithGarments>()
    override fun observeAll(): Flow<List<Outfit>> = MutableStateFlow(creados.map { it.outfit })
    override fun observeFavorites(): Flow<List<Outfit>> = MutableStateFlow(emptyList())
    override fun observe(id: Long): Flow<OutfitWithGarments?> = MutableStateFlow(creados.firstOrNull { it.outfit.id == id })
    override suspend fun get(id: Long) = creados.firstOrNull { it.outfit.id == id }
    override suspend fun create(outfit: Outfit, garments: List<OutfitGarment>): Long {
        val id = creados.size + 1L
        creados += OutfitWithGarments(outfit.copy(id = id), garments.map { it.copy(outfitId = id) })
        return id
    }

    override suspend fun update(outfit: Outfit, garments: List<OutfitGarment>) = error("no usado")
    override suspend fun setFavorite(id: Long, favorite: Boolean) = error("no usado")
    override suspend fun delete(id: Long) = error("no usado")
}

private class UsosFijos(var registros: List<WearRecord> = emptyList()) : UsoRepository {
    override fun observeFechas(garmentId: Long): Flow<List<String>> = MutableStateFlow(emptyList())
    override suspend fun todos() = registros
    override suspend fun registrarHasta(hoy: String) = 0
}

private class TiempoFijo(var respuesta: Result<TiempoDia>) : ServicioTiempo {
    val ciudades = mutableListOf<Ciudad?>()
    override suspend fun tiempoDeHoy(ciudadElegida: Ciudad?): Result<TiempoDia> {
        ciudades += ciudadElegida
        return respuesta
    }

    override suspend fun buscarCiudades(texto: String): Result<List<Ciudad>> = error("no usado")
}

/** Motor que anota el último pedido y devuelve lo que se le diga. */
private class MotorEspia(var resultado: List<OutfitSugerido> = emptyList()) : MotorSugerencias {
    var ultimo: PedidoSugerencia? = null
    override fun sugerir(pedido: PedidoSugerencia): List<OutfitSugerido> {
        ultimo = pedido
        return resultado
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OutfitDelDiaViewModelTest {
    private val hoy = "2026-11-02"
    private val madrid = Ciudad("Madrid", "España", Coordenadas(40.4, -3.7))
    private val templado = TiempoDia(temperaturaC = 18.0, maximaC = 21.0, minimaC = 12.0, condicion = CondicionTiempo.DESPEJADO)

    private fun prenda(id: Long, tipo: GarmentType, color: GarmentColor = GarmentColor.WHITE) = Garment(
        id = id, photoPath = "p$id", type = tipo, color = color, season = Season.MID_SEASON, usage = Usage.CASUAL,
        brand = null, size = null, favorite = false, dirty = false, wearsSinceWash = 0, totalWears = 0,
        washAfterWears = null, createdAt = id, fit = Fit.NORMAL,
    )

    private val armarioBasico = listOf(prenda(1, GarmentType.TSHIRT), prenda(2, GarmentType.JEANS, GarmentColor.NAVY), prenda(3, GarmentType.SHOES, GarmentColor.BLACK))

    private fun sugerido(vararg prendas: Garment, puntuacion: Double = 0.8) = OutfitSugerido(
        prendas.toList(), puntuacion, DesgloseSugerencia(clima = 1.0, ocasion = 1.0, colores = 0.9, frescura = 0.5),
    )

    private inner class Entorno(
        scope: CoroutineScope,
        armario: List<Garment> = armarioBasico,
        val motor: MotorSugerencias = MotorSugerenciasPorReglas(),
        val tiempo: TiempoFijo = TiempoFijo(Result.success(templado)),
        val usos: UsosFijos = UsosFijos(),
        val ciudad: Ciudad? = madrid,
    ) {
        val calendario = CalendarioEnMemoria()
        val outfits = OutfitsEnMemoria()
        val vm = OutfitDelDiaViewModel(
            motor, GarmentRepositoryEnMemoria(armario), usos, calendario, outfits, tiempo, { ciudad }, scope, hoy, { 1234L },
        )
        val estado get() = vm.estado.value
    }

    @Test
    fun construyeElPedidoConArmarioTiempoYUsos() = runTest(UnconfinedTestDispatcher()) {
        val espia = MotorEspia()
        val usos = UsosFijos(listOf(WearRecord("2026-10-30", 1)))
        val e = Entorno(backgroundScope, motor = espia, usos = usos)
        e.vm.elegirOcasion(Occasion.CASUAL)

        val pedido = espia.ultimo!!
        assertEquals(armarioBasico.map { it.id }.toSet(), pedido.armario.map { it.id }.toSet())
        assertEquals(hoy, pedido.hoy)
        assertEquals(templado, pedido.tiempo)
        assertEquals(Occasion.CASUAL, pedido.ocasion)
        assertEquals(usos.registros, pedido.usos)
        assertEquals(listOf<Ciudad?>(madrid), e.tiempo.ciudades)
    }

    @Test
    fun sinSenalesDeGustoLaAfinidadVaANula() = runTest(UnconfinedTestDispatcher()) {
        val espia = MotorEspia()
        Entorno(backgroundScope, motor = espia)
        assertNull(espia.ultimo!!.afinidad)
    }

    @Test
    fun conLikesOUsosOOutfitsGuardadosSeAprendeLaAfinidad() = runTest(UnconfinedTestDispatcher()) {
        val conLike = MotorEspia()
        Entorno(backgroundScope, armario = armarioBasico.map { if (it.id == 1L) it.copy(favorite = true) else it }, motor = conLike)
        assertNotNull(conLike.ultimo!!.afinidad)

        val conUsos = MotorEspia()
        Entorno(backgroundScope, motor = conUsos, usos = UsosFijos(listOf(WearRecord("2026-10-30", 1))))
        assertNotNull(conUsos.ultimo!!.afinidad)
    }

    @Test
    fun losOutfitsGuardadosEntranEnLaAfinidadAlRegenerar() = runTest(UnconfinedTestDispatcher()) {
        val espia = MotorEspia(listOf(sugerido(armarioBasico[0], armarioBasico[1])))
        val e = Entorno(backgroundScope, motor = espia)
        assertNull(espia.ultimo!!.afinidad)

        e.vm.guardar() // el outfit guardado es ya una señal de gusto
        e.vm.regenerar()
        assertNotNull(espia.ultimo!!.afinidad)
    }

    @Test
    fun conArmarioRealSugiereYMuestraLaPrincipal() = runTest(UnconfinedTestDispatcher()) {
        val e = Entorno(backgroundScope)
        assertFalse(e.estado.cargando)
        assertEquals(EstadoTiempo.Disponible(templado), e.estado.tiempo)
        assertEquals(listOf(1L, 2L, 3L), e.estado.actual!!.prendas.map { it.id }.sorted())
        assertFalse(e.estado.vacio)
    }

    @Test
    fun sinUbicacionSugiereSinTiempoYLoAvisa() = runTest(UnconfinedTestDispatcher()) {
        val espia = MotorEspia(listOf(sugerido(armarioBasico[0], armarioBasico[1])))
        val e = Entorno(backgroundScope, motor = espia, tiempo = TiempoFijo(Result.failure(ErrorTiempo.SinUbicacion)), ciudad = null)

        assertEquals(EstadoTiempo.SinUbicacion, e.estado.tiempo)
        assertNull(espia.ultimo!!.tiempo)
        assertEquals(1, e.estado.sugerencias.size)
    }

    @Test
    fun falloDeRedSePuedeReintentar() = runTest(UnconfinedTestDispatcher()) {
        val e = Entorno(backgroundScope, tiempo = TiempoFijo(Result.failure(ErrorTiempo.Red(RuntimeException("sin red")))))
        assertEquals(EstadoTiempo.SinConexion, e.estado.tiempo)

        e.tiempo.respuesta = Result.success(templado)
        e.vm.regenerar()

        assertEquals(EstadoTiempo.Disponible(templado), e.estado.tiempo)
        assertEquals(2, e.tiempo.ciudades.size)
    }

    @Test
    fun armarioVacioMarcaElEstadoVacio() = runTest(UnconfinedTestDispatcher()) {
        val e = Entorno(backgroundScope, armario = emptyList())
        assertTrue(e.estado.vacio)
        assertTrue(e.estado.armarioSinPrendas)
        assertNull(e.estado.actual)
    }

    @Test
    fun cambiarOcasionRegeneraYVuelveALaPrimera() = runTest(UnconfinedTestDispatcher()) {
        val a = sugerido(armarioBasico[0], armarioBasico[1], puntuacion = 0.9)
        val b = sugerido(armarioBasico[0], armarioBasico[1], armarioBasico[2], puntuacion = 0.7)
        val espia = MotorEspia(listOf(a, b))
        val e = Entorno(backgroundScope, motor = espia)
        e.vm.siguiente()
        assertEquals(1, e.estado.indice)

        e.vm.elegirOcasion(Occasion.WORK)

        assertEquals(0, e.estado.indice)
        assertEquals(Occasion.WORK, espia.ultimo!!.ocasion)
    }

    @Test
    fun siguienteDaLaVueltaPorLasAlternativas() = runTest(UnconfinedTestDispatcher()) {
        val a = sugerido(armarioBasico[0], armarioBasico[1], puntuacion = 0.9)
        val b = sugerido(armarioBasico[0], armarioBasico[1], armarioBasico[2], puntuacion = 0.7)
        val e = Entorno(backgroundScope, motor = MotorEspia(listOf(a, b)))

        assertEquals(a, e.estado.actual)
        e.vm.siguiente()
        assertEquals(b, e.estado.actual)
        e.vm.siguiente()
        assertEquals(a, e.estado.actual)
        e.vm.elegir(1)
        assertEquals(b, e.estado.actual)
        e.vm.elegir(7)
        assertEquals(b, e.estado.actual)
    }

    @Test
    fun guardarCreaElOutfitUnaSolaVez() = runTest(UnconfinedTestDispatcher()) {
        val e = Entorno(backgroundScope, motor = MotorEspia(listOf(sugerido(armarioBasico[0], armarioBasico[1]))))
        e.vm.elegirOcasion(Occasion.CASUAL)

        e.vm.guardar()
        e.vm.guardar()

        val creado = e.outfits.creados.single()
        assertEquals("Outfit del 02/11", creado.outfit.name)
        assertEquals(Occasion.CASUAL, creado.outfit.occasion)
        assertEquals(1234L, creado.outfit.createdAt)
        assertEquals(setOf(1L, 2L), creado.garments.map { it.garmentId }.toSet())
        assertTrue(e.estado.guardadoActual)
        assertEquals("Outfit guardado", e.estado.aviso)
    }

    @Test
    fun dosOutfitsDistintosElMismoDiaSeNombranDistinto() = runTest(UnconfinedTestDispatcher()) {
        val e = Entorno(
            backgroundScope,
            motor = MotorEspia(listOf(sugerido(armarioBasico[0], armarioBasico[1]), sugerido(armarioBasico[0], armarioBasico[2]))),
        )

        e.vm.guardar()
        e.vm.siguiente()
        e.vm.guardar()

        assertEquals(listOf("Outfit del 02/11", "Outfit del 02/11 (2)"), e.outfits.creados.map { it.outfit.name })
    }

    @Test
    fun prendasUsadasSalenDelHistorial() = runTest(UnconfinedTestDispatcher()) {
        val e = Entorno(backgroundScope, usos = UsosFijos(listOf(WearRecord("2026-10-30", 1))))
        assertEquals(setOf(1L), e.estado.prendasUsadas)
    }

    @Test
    fun aceptarLoAsignaAHoyConsevandoLaNota() = runTest(UnconfinedTestDispatcher()) {
        val e = Entorno(backgroundScope, motor = MotorEspia(listOf(sugerido(armarioBasico[0], armarioBasico[1]))))
        e.calendario.set(CalendarDay(hoy, null, "Reunión"))

        e.vm.aceptar()

        val dia = e.calendario.datos.value.getValue(hoy)
        assertEquals(e.outfits.creados.single().outfit.id, dia.outfitId)
        assertEquals("Reunión", dia.note)
        assertTrue(e.estado.aceptadoActual)
        assertEquals("Outfit asignado a hoy", e.estado.aviso)
    }

    @Test
    fun aceptarTrasGuardarReutilizaElOutfit() = runTest(UnconfinedTestDispatcher()) {
        val e = Entorno(backgroundScope, motor = MotorEspia(listOf(sugerido(armarioBasico[0], armarioBasico[1]))))
        e.vm.guardar()
        e.vm.aceptar()

        assertEquals(1, e.outfits.creados.size)
        assertEquals(1L, e.calendario.datos.value.getValue(hoy).outfitId)
    }

    @Test
    fun trasReiniciarReconoceElOutfitYaAceptadoYNoLoDuplica() = runTest(UnconfinedTestDispatcher()) {
        val e = Entorno(backgroundScope, motor = MotorEspia(listOf(sugerido(armarioBasico[0], armarioBasico[1]))))
        e.vm.aceptar()

        // Nuevo ViewModel sobre los mismos repositorios, como al volver a abrir la app.
        val reiniciado = OutfitDelDiaViewModel(
            e.motor, GarmentRepositoryEnMemoria(armarioBasico), e.usos, e.calendario, e.outfits, e.tiempo, { e.ciudad },
            backgroundScope, hoy, { 1234L },
        )
        assertTrue(reiniciado.estado.value.guardadoActual)
        assertTrue(reiniciado.estado.value.aceptadoActual)

        reiniciado.guardar()
        assertEquals(1, e.outfits.creados.size)
    }

    @Test
    fun siOtroOutfitOcupaHoyLaActualNoCuentaComoAceptada() = runTest(UnconfinedTestDispatcher()) {
        val a = sugerido(armarioBasico[0], armarioBasico[1])
        val b = sugerido(armarioBasico[0], armarioBasico[1], armarioBasico[2])
        val e = Entorno(backgroundScope, motor = MotorEspia(listOf(a, b)))
        e.vm.aceptar()
        e.vm.siguiente()

        assertFalse(e.estado.aceptadoActual)
        e.vm.aceptar()
        assertEquals(2L, e.calendario.datos.value.getValue(hoy).outfitId)
        assertTrue(e.estado.aceptadoActual)
    }

    @Test
    fun siFallaElCalendarioMuestraErrorYSePuedeReintentar() = runTest(UnconfinedTestDispatcher()) {
        val e = Entorno(backgroundScope, motor = MotorEspia(listOf(sugerido(armarioBasico[0], armarioBasico[1]))))
        e.calendario.falla = true

        e.vm.aceptar()

        assertEquals("No se pudo asignar el outfit a hoy", e.estado.error)
        assertFalse(e.estado.ocupado)
        assertFalse(e.estado.aceptadoActual)
        e.calendario.falla = false
        e.vm.aceptar()
        assertNull(e.estado.error)
        assertTrue(e.estado.aceptadoActual)
        assertEquals(1, e.outfits.creados.size)
    }

    @Test
    fun motivoCitaLosDosCriteriosMasAltos() {
        val d = DesgloseSugerencia(clima = 1.0, ocasion = 0.9, colores = 0.8, frescura = 0.2, afinidad = 0.95)
        assertEquals("Va bien con el tiempo de hoy y es muy de tu estilo.", motivoBreve(d, hayTiempo = true, hayOcasion = true))
        // Sin tiempo ni ocasión, esos criterios no cuentan aunque puntúen 1.0.
        assertEquals("Es muy de tu estilo y los colores combinan.", motivoBreve(d, hayTiempo = false, hayOcasion = false))
        assertEquals(
            "Es la mejor combinación posible con tu armario.",
            motivoBreve(DesgloseSugerencia(0.3, 0.3, 0.3, 0.3), hayTiempo = true, hayOcasion = true),
        )
    }

    @Test
    fun motivoDiferenciaPrendasSinEstrenar() {
        val d = DesgloseSugerencia(clima = 0.2, ocasion = 0.2, colores = 0.2, frescura = 1.0)
        assertEquals("Hace días que no llevas estas prendas.", motivoBreve(d, hayTiempo = true, hayOcasion = true))
        assertEquals(
            "Aún no has estrenado estas prendas.",
            motivoBreve(d, hayTiempo = true, hayOcasion = true, sinEstrenar = true),
        )
    }

    @Test
    fun nombreUnicoNumeraLosRepetidos() {
        assertEquals("Outfit del 02/11", nombreOutfitUnico("Outfit del 02/11", emptySet()))
        assertEquals("Outfit del 02/11 (2)", nombreOutfitUnico("Outfit del 02/11", setOf("Outfit del 02/11")))
        assertEquals(
            "Outfit del 02/11 (3)",
            nombreOutfitUnico("Outfit del 02/11", setOf("Outfit del 02/11", "Outfit del 02/11 (2)")),
        )
    }

    @Test
    fun criteriosOcultanLoQueNoAplica() {
        val d = DesgloseSugerencia(clima = 1.0, ocasion = 1.0, colores = 0.5, frescura = 0.5)
        assertEquals(listOf("Colores", "Frescura"), d.criterios(hayTiempo = false, hayOcasion = false).map { it.etiqueta })
        assertEquals(
            listOf("Clima", "Ocasión", "Colores", "Frescura", "Tu estilo"),
            d.copy(afinidad = 0.4).criterios(hayTiempo = true, hayOcasion = true).map { it.etiqueta },
        )
    }
}
