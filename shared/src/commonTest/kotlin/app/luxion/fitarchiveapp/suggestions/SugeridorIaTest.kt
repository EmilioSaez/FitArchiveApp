package app.luxion.fitarchiveapp.suggestions

import app.luxion.fitarchiveapp.ia.AjustesIa
import app.luxion.fitarchiveapp.ia.AvisosIa
import app.luxion.fitarchiveapp.ia.ErrorIa
import app.luxion.fitarchiveapp.model.Fit
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.weather.CondicionTiempo
import app.luxion.fitarchiveapp.weather.TiempoDia
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun prenda(id: Long, tipo: GarmentType, sucia: Boolean = false, favorita: Boolean = false) = Garment(
    id = id, photoPath = "secreto/foto$id.jpg", type = tipo, color = GarmentColor.NAVY, season = Season.MID_SEASON,
    usage = Usage.CASUAL, brand = "MarcaPrivada", size = "M", favorite = favorita, dirty = sucia, wearsSinceWash = 0,
    totalWears = 0, washAfterWears = null, createdAt = id, fit = Fit.NORMAL,
)

private val camiseta = prenda(1, GarmentType.TSHIRT)
private val vaqueros = prenda(2, GarmentType.JEANS)
private val zapatos = prenda(3, GarmentType.SHOES)
private val chaqueta = prenda(4, GarmentType.JACKET)
private val vestido = prenda(5, GarmentType.DRESS)
private val falda = prenda(6, GarmentType.SKIRT)
private val camisaSucia = prenda(7, GarmentType.SHIRT, sucia = true)
private val bolso = prenda(8, GarmentType.BAG)
private val armario = listOf(camiseta, vaqueros, zapatos, chaqueta, vestido, falda, camisaSucia, bolso)

private fun respuesta(vararg outfits: Pair<String, String>) =
    """{"outfits":[${outfits.joinToString(",") { """{"prendas":${it.first},"explicacion":"${it.second}"}""" }}]}"""

class SugeridorIaTest {
    private fun sugeridor(
        texto: String = respuesta(),
        estado: HttpStatusCode = HttpStatusCode.OK,
        ajustes: AjustesDePrueba = AjustesDePrueba(AJUSTES_CON_AVISO_DE_OUTFITS),
        boveda: BovedaDePrueba = BovedaDePrueba(),
    ): Pair<SugeridorIa, ServicioIaDePrueba> {
        val s = ServicioIaDePrueba(texto, estado, boveda, ajustes)
        return SugeridorIa(s.servicio) to s
    }

    private suspend fun SugeridorIa.pedir(
        tiempo: TiempoDia? = null,
        maximo: Int = SugeridorIa.MAX_OUTFITS,
        favoritos: List<List<Long>> = emptyList(),
    ) = sugerir(PedidoIa(armario, tiempo, Occasion.CASUAL, favoritos, maximo))

    private fun ids(r: List<OutfitIa>) = r.map { o -> o.prendas.map { it.id } }

    // --- Qué se envía ---

    @Test
    fun `solo envia datos minimos de prendas limpias sin fotos nombre ni marca`() = runTest {
        val (sug, s) = sugeridor(respuesta("[1,2]" to "ok"))
        sug.pedir(favoritos = listOf(listOf(1L, 2L)))
        val cuerpo = s.cuerpos.single()
        for (prohibido in listOf("secreto", "foto", "MarcaPrivada", "photoPath", "brand", "size", "ubicacion", "latitud")) {
            assertFalse(prohibido in cuerpo, "No debe viajar: $prohibido")
        }
        for (campo in listOf("tipo", "subtipo", "color", "estacion", "uso", "favorita", "TSHIRT", "NAVY", "MID_SEASON", "CASUAL")) {
            assertTrue(campo in cuerpo, "Falta $campo")
        }
        // La prenda sucia (7) y el bolso (8) no se envían; la camiseta (1) sí.
        assertFalse("\\\"id\\\":7" in cuerpo)
        assertFalse("\\\"id\\\":8" in cuerpo)
        assertTrue("\\\"id\\\":1," in cuerpo)
        assertTrue("outfitsFavoritos" in cuerpo)
    }

    @Test
    fun `con calor no envia capas`() {
        val calor = TiempoDia(30.0, 33.0, 24.0, CondicionTiempo.DESPEJADO)
        assertFalse(chaqueta in SugeridorIa.candidatas(PedidoIa(armario, calor)))
        assertTrue(chaqueta in SugeridorIa.candidatas(PedidoIa(armario, TiempoDia(10.0, 12.0, 6.0, CondicionTiempo.LLUVIA))))
    }

    @Test
    fun `sin arriba y abajo ni vestido no gasta una consulta`() = runTest {
        val (sug, s) = sugeridor()
        val r = sug.sugerir(PedidoIa(listOf(camiseta, zapatos)))
        assertEquals(emptyList(), r.getOrThrow())
        assertTrue(s.cuerpos.isEmpty())
    }

    // --- Parseo y validación ---

    @Test
    fun `una respuesta valida se convierte en outfits ordenados con su explicacion`() = runTest {
        val (sug, _) = sugeridor(respuesta("[3,2,1,4]" to "Clásico y\\ncómodo", "[5]" to "Vestido solo"))
        val r = sug.pedir().getOrThrow()
        assertEquals(listOf(listOf(4L, 1L, 2L, 3L), listOf(5L)), ids(r)) // capa, arriba, abajo, calzado
        assertEquals("Clásico y cómodo", r[0].explicacion)
    }

    @Test
    fun `descarta ids inexistentes prendas sucias y no enviadas`() = runTest {
        val (sug, _) = sugeridor(
            respuesta("[1,99]" to "no existe", "[7,2]" to "sucia", "[1,8]" to "bolso no enviado", "[1,2]" to "buena"),
        )
        assertEquals(listOf(listOf(1L, 2L)), ids(sug.pedir().getOrThrow()))
    }

    @Test
    fun `descarta outfits incoherentes`() = runTest {
        val (sug, _) = sugeridor(
            respuesta(
                "[2,6]" to "dos partes de abajo",
                "[1,5]" to "vestido con camiseta",
                "[1]" to "sin abajo",
                "[1,2,3,3]" to "calzado repetido",
                "[1,1,2]" to "id duplicado",
                "[]" to "vacio",
            ),
        )
        assertEquals(emptyList(), sug.pedir().getOrThrow())
    }

    @Test
    fun `no repite outfits y respeta el maximo`() = runTest {
        val (sug, _) = sugeridor(respuesta("[1,2]" to "a", "[2,1]" to "repetido", "[1,6]" to "b", "[5]" to "c", "[5,3]" to "d"))
        assertEquals(listOf(listOf(1L, 2L), listOf(1L, 6L), listOf(5L)), ids(sug.pedir(maximo = 3).getOrThrow()))
    }

    @Test
    fun `una capa con calor se descarta`() = runTest {
        val (sug, _) = sugeridor(respuesta("[1,2,4]" to "con chaqueta"))
        val calor = TiempoDia(30.0, 33.0, 24.0, CondicionTiempo.DESPEJADO)
        assertEquals(emptyList(), sug.pedir(tiempo = calor).getOrThrow())
    }

    @Test
    fun `ids no enteros se descartan`() = runTest {
        val (sug, _) = sugeridor(
            """{"outfits":[{"prendas":["1","2"],"explicacion":"x"},{"prendas":[1.5,2],"explicacion":"y"},{"prendas":[1,2],"explicacion":"ok"}]}""",
        )
        assertEquals(listOf(listOf(1L, 2L)), ids(sug.pedir().getOrThrow()))
    }

    @Test
    fun `la explicacion se limpia y se acota`() {
        assertEquals("Combinación propuesta por la IA.", SugeridorIa.limpiarExplicacion("  \n\t "))
        assertEquals("Combinación propuesta por la IA.", SugeridorIa.limpiarExplicacion(null))
        assertEquals("a b c", SugeridorIa.limpiarExplicacion("a\n\nb\u0007  c"))
        val larga = SugeridorIa.limpiarExplicacion("x".repeat(1000))
        assertEquals(240, larga.length)
        assertTrue(larga.endsWith("…"))
    }

    @Test
    fun `json roto o sin la lista falla con RespuestaInvalida`() = runTest {
        for (texto in listOf("no es json", """{"otra":1}""", """{"outfits":"x"}""", "[]")) {
            val (sug, _) = sugeridor(texto)
            assertIs<ErrorIa.RespuestaInvalida>(sug.pedir().exceptionOrNull(), texto)
        }
    }

    // --- Bloqueos y errores del servicio ---

    @Test
    fun `sin clave no llama a la red`() = runTest {
        val (sug, s) = sugeridor(boveda = BovedaDePrueba(null))
        assertIs<ErrorIa.SinClave>(sug.pedir().exceptionOrNull())
        assertIs<ErrorIa.SinClave>(sug.bloqueo())
        assertTrue(s.cuerpos.isEmpty())
    }

    @Test
    fun `el aviso general no basta hace falta el de outfits`() = runTest {
        val (sug, s) = sugeridor(ajustes = AjustesDePrueba(AjustesIa(activada = true, avisosAceptados = setOf(AvisosIa.GENERAL.id))))
        val error = sug.pedir().exceptionOrNull()
        assertIs<ErrorIa.AvisoPendiente>(error)
        assertEquals(AVISO_OUTFITS_IA, error.aviso)
        assertTrue(AVISO_OUTFITS_IA.id != AvisosIa.GENERAL.id)
        assertTrue(s.cuerpos.isEmpty())
    }

    @Test
    fun `desactivada o con tope alcanzado bloquea`() = runTest {
        val (desactivada, _) = sugeridor(ajustes = AjustesDePrueba(AJUSTES_CON_AVISO_DE_OUTFITS.copy(activada = false)))
        assertIs<ErrorIa.Desactivada>(desactivada.bloqueo())
        val tope = AJUSTES_CON_AVISO_DE_OUTFITS.copy(topeDiario = 1, fechaUso = "2026-11-16", usosHoy = 1)
        val (sug, s) = sugeridor(ajustes = AjustesDePrueba(tope))
        assertIs<ErrorIa.TopeAlcanzado>(sug.pedir().exceptionOrNull())
        assertTrue(s.cuerpos.isEmpty())
    }

    @Test
    fun `un error del servidor llega como ErrorIa con mensaje`() = runTest {
        val (sug, _) = sugeridor(estado = HttpStatusCode.InternalServerError)
        val error = sug.pedir().exceptionOrNull()
        assertIs<ErrorIa.Servidor>(error)
        assertTrue(error.mensaje.isNotBlank())
    }
}
