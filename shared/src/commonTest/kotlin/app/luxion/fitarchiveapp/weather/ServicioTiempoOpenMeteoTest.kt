package app.luxion.fitarchiveapp.weather

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ServicioTiempoOpenMeteoTest {
    private val urls = mutableListOf<String>()
    private var respuesta: () -> String = { TIEMPO_JSON }
    private val cliente = ClienteHttp { urls += it; respuesta() }
    private val madrid = Ciudad("Madrid", "España", Coordenadas(40.4168, -3.7038))

    private fun servicio(gps: Coordenadas?) = ServicioTiempoOpenMeteo(cliente) { gps }

    @Test
    fun usaUbicacionDelMovilSiHayPermiso() = runTest {
        val t = servicio(Coordenadas(41.3874, 2.1686)).tiempoDeHoy(madrid).getOrThrow()
        assertEquals(18.5, t.temperaturaC)
        assertEquals(22.0, t.maximaC)
        assertEquals(11.0, t.minimaC)
        assertEquals(CondicionTiempo.LLUVIA, t.condicion)
        assertTrue("latitude=41.4&longitude=2.2" in urls.single(), urls.single())
    }

    @Test
    fun usaCiudadElegidaSinPermiso() = runTest {
        servicio(null).tiempoDeHoy(madrid).getOrThrow()
        assertTrue("latitude=40.4&longitude=-3.7" in urls.single(), urls.single())
    }

    @Test
    fun sinUbicacionNiCiudadFallaSinRed() = runTest {
        val e = servicio(null).tiempoDeHoy(null).exceptionOrNull()
        assertEquals(ErrorTiempo.SinUbicacion, e)
        assertTrue(urls.isEmpty())
    }

    @Test
    fun errorDeRedSeEncapsula() = runTest {
        respuesta = { error("sin conexión") }
        assertIs<ErrorTiempo.Red>(servicio(null).tiempoDeHoy(madrid).exceptionOrNull())
    }

    @Test
    fun respuestaInvalidaSeEncapsula() = runTest {
        respuesta = { "{}" }
        assertIs<ErrorTiempo.Red>(servicio(null).tiempoDeHoy(madrid).exceptionOrNull())
    }

    @Test
    fun buscaCiudadesYCodificaElTexto() = runTest {
        respuesta = { CIUDADES_JSON }
        val r = servicio(null).buscarCiudades("A Coruña").getOrThrow()
        assertEquals(listOf(Ciudad("A Coruña", "España", Coordenadas(43.37, -8.4))), r)
        assertTrue("name=A%20Coru%C3%B1a" in urls.single(), urls.single())
    }

    @Test
    fun busquedaSinResultadosDevuelveListaVacia() = runTest {
        respuesta = { "{\"generationtime_ms\":0.5}" }
        assertEquals(emptyList(), servicio(null).buscarCiudades("zzzz").getOrThrow())
    }

    @Test
    fun busquedaEnBlancoNoLlamaALaRed() = runTest {
        assertEquals(emptyList(), servicio(null).buscarCiudades("  ").getOrThrow())
        assertTrue(urls.isEmpty())
    }

    // --- Caché corta (O18) ---

    private var reloj = 1_000_000_000_000L
    private val cache = CacheTiempo(vidaMs = 30 * 60_000L, ahora = { reloj }, zona = { TimeZone.UTC })
    private fun conCache(gps: Coordenadas? = null) = ServicioTiempoOpenMeteo(cliente, cache) { gps }

    @Test
    fun conCacheLaSegundaConsultaNoUsaLaRed() = runTest {
        val primero = conCache().tiempoDeHoy(madrid).getOrThrow()
        reloj += 5 * 60_000L
        val segundo = conCache().tiempoDeHoy(madrid).getOrThrow() // otro servicio: la caché es compartida
        assertEquals(primero, segundo)
        assertEquals(1, urls.size)
    }

    @Test
    fun laCacheCaducaConElTiempo() = runTest {
        conCache().tiempoDeHoy(madrid).getOrThrow()
        reloj += 31 * 60_000L
        conCache().tiempoDeHoy(madrid).getOrThrow()
        assertEquals(2, urls.size)
    }

    @Test
    fun laCacheCaducaAlCambiarDeDia() = runTest {
        reloj = 1_000_000_000_000L - 1_000_000_000_000L % 86_400_000L + 86_400_000L - 60_000L // 23:59 UTC
        conCache().tiempoDeHoy(madrid).getOrThrow()
        reloj += 2 * 60_000L // 00:01 del día siguiente, aún dentro de la vida de la caché
        conCache().tiempoDeHoy(madrid).getOrThrow()
        assertEquals(2, urls.size)
    }

    @Test
    fun cambiarDeCiudadNoUsaLaCache() = runTest {
        conCache().tiempoDeHoy(madrid).getOrThrow()
        conCache().tiempoDeHoy(Ciudad("Lima", "Perú", Coordenadas(-12.05, -77.04))).getOrThrow()
        assertEquals(2, urls.size)
    }

    @Test
    fun losFallosNoSeCachean() = runTest {
        respuesta = { error("sin conexión") }
        assertIs<ErrorTiempo.Red>(conCache().tiempoDeHoy(madrid).exceptionOrNull())
        respuesta = { TIEMPO_JSON }
        conCache().tiempoDeHoy(madrid).getOrThrow()
        assertEquals(2, urls.size)
    }

    @Test
    fun vaciarLaCacheVuelveAConsultar() = runTest {
        conCache().tiempoDeHoy(madrid).getOrThrow()
        cache.vaciar()
        conCache().tiempoDeHoy(madrid).getOrThrow()
        assertEquals(2, urls.size)
    }

    @Test
    fun mapeaCodigosWmo() {
        assertEquals(CondicionTiempo.DESPEJADO, condicionDeCodigo(0))
        assertEquals(CondicionTiempo.NUBLADO, condicionDeCodigo(3))
        assertEquals(CondicionTiempo.NIEBLA, condicionDeCodigo(45))
        assertEquals(CondicionTiempo.LLOVIZNA, condicionDeCodigo(53))
        assertEquals(CondicionTiempo.LLUVIA, condicionDeCodigo(81))
        assertEquals(CondicionTiempo.NIEVE, condicionDeCodigo(73))
        assertEquals(CondicionTiempo.TORMENTA, condicionDeCodigo(95))
    }

    private companion object {
        const val TIEMPO_JSON = """{"latitude":41.4,"current":{"time":"2026-11-02T10:00","temperature_2m":18.5,"weather_code":63},
            "daily":{"time":["2026-11-02"],"temperature_2m_max":[22.0],"temperature_2m_min":[11.0]}}"""
        const val CIUDADES_JSON = """{"results":[{"id":1,"name":"A Coruña","latitude":43.37,"longitude":-8.4,"country":"España"}]}"""
    }
}
