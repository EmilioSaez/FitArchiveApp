package app.luxion.fitarchiveapp.ia

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class BovedaEnMemoria(var clave: String? = null, var fallaAlEscribir: Boolean = false) : BovedaClaveIa {
    override fun leer() = clave
    override fun escribir(clave: String) {
        if (fallaAlEscribir) throw ErrorIa.Almacen()
        this.clave = clave
    }
    override fun borrar() { clave = null }
}

private class AjustesEnMemoria(var ajustes: AjustesIa = AjustesIa()) : AlmacenAjustesIa {
    override fun cargar() = ajustes
    override fun guardar(ajustes: AjustesIa) { this.ajustes = ajustes }
}

private const val CLAVE = "sk-ant-api03-claveDePrueba123456"

private const val RESPUESTA_OK =
    """{"content":[{"type":"text","text":"hola"}],"stop_reason":"end_turn","usage":{"input_tokens":10,"output_tokens":3}}"""

private val JSON = headersOf(HttpHeaders.ContentType, "application/json")

private val AJUSTES_LISTOS = AjustesIa(activada = true, avisosAceptados = setOf(AvisosIa.GENERAL.id))

class ServicioIaTest {
    private val peticiones = mutableListOf<io.ktor.client.request.HttpRequestData>()
    private val cuerpos = mutableListOf<String>()
    private val esperas = mutableListOf<Long>()

    /** [respuestas] se consumen en orden; la última se repite. Sin ninguna, siempre responde [RESPUESTA_OK]. */
    private fun servicio(
        boveda: BovedaEnMemoria = BovedaEnMemoria(CLAVE),
        ajustes: AjustesEnMemoria = AjustesEnMemoria(AJUSTES_LISTOS),
        respuestas: List<MockRequestHandleScope.() -> HttpResponseData> = emptyList(),
    ): ServicioIa {
        val cola = respuestas.toMutableList()
        val motor = MockEngine { req ->
            peticiones += req
            cuerpos += req.body.toByteArray().decodeToString()
            val siguiente = if (cola.size > 1) cola.removeAt(0) else cola.firstOrNull()
            siguiente?.invoke(this) ?: respond(RESPUESTA_OK, HttpStatusCode.OK, JSON)
        }
        return ServicioIa(ClienteClaude(HttpClient(motor), espera = { esperas += it }), AlmacenIa(boveda, ajustes)) { "2026-11-16" }
    }

    @Test
    fun `sin clave no llama a la red`() = runTest {
        val s = servicio(boveda = BovedaEnMemoria(null))
        assertIs<ErrorIa.SinClave>(s.preguntarTexto("hola").exceptionOrNull())
        assertTrue(peticiones.isEmpty())
    }

    @Test
    fun `desactivada, sin aviso o con tope alcanzado bloquea`() {
        assertIs<ErrorIa.Desactivada>(servicio(ajustes = AjustesEnMemoria(AJUSTES_LISTOS.copy(activada = false))).bloqueo())
        assertIs<ErrorIa.AvisoPendiente>(servicio(ajustes = AjustesEnMemoria(AJUSTES_LISTOS.copy(avisosAceptados = emptySet()))).bloqueo())
        val tope = AJUSTES_LISTOS.copy(topeDiario = 2, fechaUso = "2026-11-16", usosHoy = 2)
        assertIs<ErrorIa.TopeAlcanzado>(servicio(ajustes = AjustesEnMemoria(tope)).bloqueo())
        assertNull(servicio().bloqueo())
    }

    @Test
    fun `el contador de otro dia no cuenta`() {
        val ayer = AJUSTES_LISTOS.copy(topeDiario = 1, fechaUso = "2026-11-15", usosHoy = 1)
        assertNull(servicio(ajustes = AjustesEnMemoria(ayer)).bloqueo())
    }

    @Test
    fun `llamada correcta envia clave y cabeceras y cuenta uso y tokens`() = runTest {
        val ajustes = AjustesEnMemoria(AJUSTES_LISTOS)
        val s = servicio(ajustes = ajustes)
        assertEquals("hola", s.preguntarTexto("¿qué me pongo?", sistema = "eres estilista").getOrThrow())
        val req = peticiones.single()
        assertEquals(CLAVE, req.headers["x-api-key"])
        assertEquals("2023-06-01", req.headers["anthropic-version"])
        assertTrue("\"model\":\"claude-haiku-5-5\"" in cuerpos.single())
        assertTrue("eres estilista" in cuerpos.single())
        assertEquals(1, ajustes.ajustes.usosHoy)
        assertEquals(10, ajustes.ajustes.tokensEntradaTotal)
        assertEquals(3, ajustes.ajustes.tokensSalidaTotal)
    }

    @Test
    fun `la imagen y el esquema van en el cuerpo`() = runTest {
        val esquema = buildJsonObject { put("type", JsonPrimitive("object")) }
        servicio().preguntarJson(listOf(ParteClaude.Imagen("QUJD"), ParteClaude.Texto("describe")), esquema).getOrThrow()
        val c = cuerpos.single()
        assertTrue("\"media_type\":\"image/jpeg\"" in c)
        assertTrue("\"data\":\"QUJD\"" in c)
        assertTrue("\"json_schema\"" in c)
    }

    @Test
    fun `401 borra la clave y desactiva la IA`() = runTest {
        val boveda = BovedaEnMemoria(CLAVE)
        val ajustes = AjustesEnMemoria(AJUSTES_LISTOS)
        val s = servicio(boveda, ajustes, listOf({ respondError(HttpStatusCode.Unauthorized) }))
        assertIs<ErrorIa.ClaveNoValida>(s.preguntarTexto("x").exceptionOrNull())
        assertNull(boveda.clave)
        assertFalse(ajustes.ajustes.activada)
        assertFalse(s.estado.value.tieneClave)
    }

    @Test
    fun `429 y 5xx se reintentan, 400 no`() = runTest {
        val s = servicio(respuestas = listOf(
            { respondError(HttpStatusCode.TooManyRequests, headers = headersOf("retry-after", "3")) },
            { respondError(HttpStatusCode.InternalServerError) },
            { respond(RESPUESTA_OK, HttpStatusCode.OK, JSON) },
        ))
        assertEquals("hola", s.preguntarTexto("x").getOrThrow())
        assertEquals(3, peticiones.size)
        assertEquals(listOf(3000L, 2000L), esperas)

        peticiones.clear()
        val s400 = servicio(respuestas = listOf({ respondError(HttpStatusCode.BadRequest) }))
        assertIs<ErrorIa.SolicitudInvalida>(s400.preguntarTexto("x").exceptionOrNull())
        assertEquals(1, peticiones.size)
    }

    @Test
    fun `tras agotar los reintentos devuelve el error`() = runTest {
        val s = servicio(respuestas = listOf({ respondError(HttpStatusCode.ServiceUnavailable) }))
        assertIs<ErrorIa.Servidor>(s.preguntarTexto("x").exceptionOrNull())
        assertEquals(1 + ClienteClaude.MAX_REINTENTOS, peticiones.size)
    }

    @Test
    fun `una negativa es un error pero cuenta como uso`() = runTest {
        val ajustes = AjustesEnMemoria(AJUSTES_LISTOS)
        val negativa = """{"content":[],"stop_reason":"refusal","usage":{"input_tokens":1,"output_tokens":0}}"""
        val s = servicio(ajustes = ajustes, respuestas = listOf({ respond(negativa, HttpStatusCode.OK, JSON) }))
        assertIs<ErrorIa.Rechazo>(s.preguntarTexto("x").exceptionOrNull())
        assertEquals(1, ajustes.ajustes.usosHoy)
    }

    @Test
    fun `respuesta mal formada`() = runTest {
        val s = servicio(respuestas = listOf({ respond("no es json", HttpStatusCode.OK, JSON) }))
        assertIs<ErrorIa.RespuestaInvalida>(s.preguntarTexto("x").exceptionOrNull())
    }

    @Test
    fun `probar conexion lee las capacidades del modelo`() = runTest {
        val cuerpo = """{"id":"claude-haiku-5-5","display_name":"Claude Haiku 5.5","capabilities":{"image_input":{"supported":true},"structured_outputs":{"supported":false}}}"""
        val s = servicio(respuestas = listOf({ respond(cuerpo, HttpStatusCode.OK, JSON) }))
        val info = s.probarConexion().getOrThrow()
        assertEquals("Claude Haiku 5.5", info.nombre)
        assertEquals(true, info.admiteImagenes)
        assertEquals(false, info.admiteSalidaEstructurada)
        assertTrue(peticiones.single().url.encodedPath.endsWith("/v1/models/claude-haiku-5-5"))
    }

    @Test
    fun `guardar clave valida el formato y propaga el fallo del almacen`() {
        val boveda = BovedaEnMemoria()
        val s = servicio(boveda = boveda)
        assertTrue(s.guardarClave("hola").isFailure)
        assertTrue(s.guardarClave("sk-ant-con espacio dentro 12345").isFailure)
        assertNull(boveda.clave)
        assertTrue(s.guardarClave("  $CLAVE\n").isSuccess)
        assertEquals(CLAVE, boveda.clave)
        assertTrue(s.estado.value.tieneClave)

        val rota = servicio(boveda = BovedaEnMemoria(fallaAlEscribir = true))
        assertIs<ErrorIa.Almacen>(rota.guardarClave(CLAVE).exceptionOrNull())
    }

    @Test
    fun `borrar la clave desactiva y el tope se acota`() {
        val s = servicio()
        s.cambiarTopeDiario(9999)
        assertEquals(RANGO_TOPE_DIARIO.last, s.estado.value.ajustes.topeDiario)
        s.borrarClave()
        assertFalse(s.estado.value.ajustes.activada)
        assertFalse(s.estado.value.tieneClave)
    }

    @Test
    fun `los ajustes corruptos vuelven al defecto y los validos ida y vuelta`() {
        assertEquals(AjustesIa(), AjustesIaCodec.decodificar("{roto"))
        val a = AjustesIa(activada = true, topeDiario = 7, avisosAceptados = setOf("x"))
        assertEquals(a, AjustesIaCodec.decodificar(AjustesIaCodec.codificar(a)))
    }

    private fun mensajesEnviados() =
        Json.parseToJsonElement(cuerpos.single()).jsonObject["messages"]!!.jsonArray.map { it.jsonObject }

    @Test
    fun `el historial se envia antes del mensaje final`() = runTest {
        servicio().preguntar(
            listOf(ParteClaude.Texto("tercera")),
            historial = listOf(TurnoClaude(RolClaude.USUARIA, "primera"), TurnoClaude(RolClaude.ASISTENTE, "segunda")),
        ).getOrThrow()
        val m = mensajesEnviados()
        assertEquals(listOf("user", "assistant", "user"), m.map { it["role"]!!.jsonPrimitive.content })
        assertEquals("primera", m[0]["content"]!!.jsonPrimitive.content)
        assertEquals("segunda", m[1]["content"]!!.jsonPrimitive.content)
        assertTrue("tercera" in m[2].toString())
    }

    @Test
    fun `sin historial el cuerpo tiene un solo mensaje`() = runTest {
        servicio().preguntarTexto("hola").getOrThrow()
        assertEquals(1, mensajesEnviados().size)
    }
}
