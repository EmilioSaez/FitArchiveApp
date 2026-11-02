package app.luxion.fitarchiveapp.weather

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeleccionCiudadTest {
    private val madrid = Ciudad("Madrid", "España", Coordenadas(40.4168, -3.7038))
    private val busquedas = mutableListOf<String>()
    private var resultados: Result<List<Ciudad>> = Result.success(listOf(madrid))

    private val servicio = object : ServicioTiempo {
        override suspend fun tiempoDeHoy(ciudadElegida: Ciudad?): Result<TiempoDia> = error("no se usa")
        override suspend fun buscarCiudades(texto: String): Result<List<Ciudad>> {
            busquedas += texto
            return resultados
        }
    }

    private class AlmacenMemoria(var valor: Ciudad? = null) : AlmacenCiudad {
        override fun cargar() = valor
        override fun guardar(ciudad: Ciudad?) {
            valor = ciudad
        }
    }

    private val almacen = AlmacenMemoria()
    private val seleccion = SeleccionCiudad(servicio, almacen)

    @Test
    fun textoCortoNoConsultaLaRed() = runTest {
        assertEquals(emptyList(), seleccion.buscar(" a ").getOrThrow())
        assertEquals(emptyList(), seleccion.buscar("").getOrThrow())
        assertTrue(busquedas.isEmpty())
    }

    @Test
    fun buscaConElTextoRecortado() = runTest {
        assertEquals(listOf(madrid), seleccion.buscar("  Madrid ").getOrThrow())
        assertEquals(listOf("Madrid"), busquedas)
    }

    @Test
    fun quitaDuplicados() = runTest {
        resultados = Result.success(listOf(madrid, madrid))
        assertEquals(listOf(madrid), seleccion.buscar("Madrid").getOrThrow())
    }

    @Test
    fun propagaElErrorDelServicio() = runTest {
        resultados = Result.failure(ErrorTiempo.Red(RuntimeException("x")))
        assertTrue(seleccion.buscar("Madrid").isFailure)
    }

    @Test
    fun guardaYQuitaLaCiudad() {
        assertNull(seleccion.ciudadGuardada())
        seleccion.elegir(madrid)
        assertEquals(madrid, seleccion.ciudadGuardada())
        seleccion.quitar()
        assertNull(seleccion.ciudadGuardada())
    }

    @Test
    fun etiquetaConYSinPais() {
        assertEquals("Madrid, España", madrid.etiqueta())
        assertEquals("Atlantis", Ciudad("Atlantis", null, Coordenadas(0.0, 0.0)).etiqueta())
    }

    @Test
    fun codecIdaYVuelta() {
        val sinPais = Ciudad("Ciudad|rara, X", null, Coordenadas(-33.5, 151.25))
        assertEquals(madrid, CiudadCodec.decodificar(CiudadCodec.codificar(madrid)))
        assertEquals(sinPais, CiudadCodec.decodificar(CiudadCodec.codificar(sinPais)))
    }

    @Test
    fun codecIgnoraDatosCorruptos() {
        assertNull(CiudadCodec.decodificar(null))
        assertNull(CiudadCodec.decodificar(""))
        assertNull(CiudadCodec.decodificar("basura"))
        assertNull(CiudadCodec.decodificar("a\u001Fb\u001F\u001FMadrid"))
        assertNull(CiudadCodec.decodificar("1\u001F2\u001F\u001F "))
    }
}
