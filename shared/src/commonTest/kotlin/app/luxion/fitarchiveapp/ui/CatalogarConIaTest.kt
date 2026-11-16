package app.luxion.fitarchiveapp.ui

import app.luxion.fitarchiveapp.avatar.Subtipo
import app.luxion.fitarchiveapp.avatar.subtiposValidos
import app.luxion.fitarchiveapp.foto.FotoCapturada
import app.luxion.fitarchiveapp.foto.dimensionesReducidas
import app.luxion.fitarchiveapp.ia.AjustesIa
import app.luxion.fitarchiveapp.ia.AlmacenAjustesIa
import app.luxion.fitarchiveapp.ia.AlmacenIa
import app.luxion.fitarchiveapp.ia.AvisosIa
import app.luxion.fitarchiveapp.ia.BovedaClaveIa
import app.luxion.fitarchiveapp.ia.ClienteClaude
import app.luxion.fitarchiveapp.ia.ErrorIa
import app.luxion.fitarchiveapp.ia.ServicioIa
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.ui.anadir.AVISO_CATALOGAR
import app.luxion.fitarchiveapp.ui.anadir.BorradorPrenda
import app.luxion.fitarchiveapp.ui.anadir.ESQUEMA_CATALOGAR
import app.luxion.fitarchiveapp.ui.anadir.catalogarConIa
import app.luxion.fitarchiveapp.ui.anadir.conSugerencia
import app.luxion.fitarchiveapp.ui.anadir.decodificarSugerencia
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class Boveda(var clave: String? = "sk-ant-api03-claveDePrueba123456") : BovedaClaveIa {
    override fun leer() = clave
    override fun escribir(clave: String) { this.clave = clave }
    override fun borrar() { clave = null }
}

private class Ajustes(var ajustes: AjustesIa) : AlmacenAjustesIa {
    override fun cargar() = ajustes
    override fun guardar(ajustes: AjustesIa) { this.ajustes = ajustes }
}

private val LISTA = AjustesIa(activada = true, avisosAceptados = setOf(AVISO_CATALOGAR.id))
private val FOTO = FotoCapturada(byteArrayOf(1, 2, 3))

class CatalogarConIaTest {
    private val cuerpos = mutableListOf<String>()

    private fun respuesta(textoJson: String) = buildJsonObject {
        putJsonArray("content") { add(buildJsonObject { put("type", "text"); put("text", textoJson) }) }
        put("stop_reason", "end_turn")
        putJsonObject("usage") { put("input_tokens", 100); put("output_tokens", 20) }
    }.toString()

    private fun servicio(textoJson: String, ajustes: AjustesIa = LISTA, boveda: Boveda = Boveda()): ServicioIa {
        val motor = MockEngine { req ->
            cuerpos += req.body.toByteArray().decodeToString()
            respond(respuesta(textoJson), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return ServicioIa(ClienteClaude(HttpClient(motor), espera = {}), AlmacenIa(boveda, Ajustes(ajustes))) { "2026-11-16" }
    }

    private val jpegFalso: suspend (ByteArray) -> ByteArray? = { byteArrayOf(65, 66, 67) } // "ABC"

    @Test
    fun `decodifica una respuesta completa`() {
        val s = decodificarSugerencia("""{"tipo":"JEANS","subtipo":"BOTTOM_PANTALON_LARGO","color":"BLUE","estacion":"MID_SEASON","uso":"CASUAL"}""")
        assertEquals(GarmentType.JEANS, s?.type)
        assertEquals(Subtipo.BOTTOM_PANTALON_LARGO, s?.subtipo)
        assertEquals(GarmentColor.BLUE, s?.color)
        assertEquals(Season.MID_SEASON, s?.season)
        assertEquals(Usage.CASUAL, s?.usage)
    }

    @Test
    fun `valores desconocidos o de otro tipo se ignoran sin romper`() {
        val s = decodificarSugerencia("""{"tipo":"CAPA","subtipo":7,"color":"blue","estacion":null,"uso":"PARTY","extra":"x"}""")!!
        assertNull(s.type)
        assertNull(s.subtipo)
        assertEquals(GarmentColor.BLUE, s.color) // no distingue mayúsculas
        assertNull(s.season)
        assertEquals(Usage.PARTY, s.usage)
    }

    @Test
    fun `json invalido o que no es un objeto da null`() {
        assertNull(decodificarSugerencia("esto no es json"))
        assertNull(decodificarSugerencia("[1,2]"))
        assertNull(decodificarSugerencia(""))
    }

    @Test
    fun `la sugerencia rellena el borrador y respeta marca y talla`() {
        val antes = BorradorPrenda(brand = "Zara", size = "M")
        val s = decodificarSugerencia("""{"tipo":"SHIRT","subtipo":"TOP_MANGA_CORTA","color":"WHITE","estacion":"SUMMER","uso":"WORK"}""")!!
        val b = antes.conSugerencia(s)
        assertEquals(GarmentType.SHIRT, b.type)
        assertEquals(Subtipo.TOP_MANGA_CORTA, b.subtipo)
        assertEquals(GarmentColor.WHITE, b.color)
        assertEquals(Season.SUMMER, b.season)
        assertEquals(Usage.WORK, b.usage)
        assertEquals("Zara", b.brand)
        assertEquals("M", b.size)
        assertTrue(b.completo)
    }

    @Test
    fun `un subtipo que no vale para el tipo se descarta`() {
        val s = decodificarSugerencia("""{"tipo":"SHOES","subtipo":"TOP_MANGA_LARGA","color":"BLACK","estacion":"WINTER","uso":"CASUAL"}""")!!
        val b = BorradorPrenda().conSugerencia(s)
        assertEquals(GarmentType.SHOES, b.type)
        assertNull(b.subtipo)
        assertTrue(b.subtipoEfectivo in subtiposValidos(GarmentType.SHOES))
    }

    @Test
    fun `una sugerencia parcial no borra lo que ya habia`() {
        val antes = BorradorPrenda(type = GarmentType.DRESS, color = GarmentColor.RED, season = Season.SUMMER, usage = Usage.PARTY)
        val b = antes.conSugerencia(decodificarSugerencia("""{"tipo":"???","color":"GREEN"}""")!!)
        assertEquals(GarmentType.DRESS, b.type)
        assertEquals(GarmentColor.GREEN, b.color)
        assertEquals(Season.SUMMER, b.season)
        assertEquals(Usage.PARTY, b.usage)
    }

    @Test
    fun `cambiar el tipo por la sugerencia descarta la talla que ya no vale`() {
        val b = BorradorPrenda(type = GarmentType.TSHIRT, size = "M").conSugerencia(decodificarSugerencia("""{"tipo":"SHOES"}""")!!)
        assertNull(b.size)
    }

    @Test
    fun `el esquema lista exactamente los valores de los enums`() {
        val props = ESQUEMA_CATALOGAR["properties"]!!.jsonObject
        fun valores(campo: String) = props[campo]!!.jsonObject["enum"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(GarmentType.entries.map { it.name }, valores("tipo"))
        assertEquals(Subtipo.entries.map { it.name }, valores("subtipo"))
        assertEquals(GarmentColor.entries.map { it.name }, valores("color"))
        assertEquals(Season.entries.map { it.name }, valores("estacion"))
        assertEquals(Usage.entries.map { it.name }, valores("uso"))
        assertEquals(props.keys, ESQUEMA_CATALOGAR["required"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet())
        assertEquals(JsonPrimitive(false), ESQUEMA_CATALOGAR["additionalProperties"])
    }

    @Test
    fun `el aviso es propio y menciona la foto`() {
        assertTrue(AVISO_CATALOGAR.id != AvisosIa.GENERAL.id)
        assertTrue("foto" in AVISO_CATALOGAR.texto)
    }

    @Test
    fun `catalogar envia la imagen en base64 con el esquema y devuelve la sugerencia`() = runTest {
        val s = servicio("""{"tipo":"COAT","subtipo":"OUTER_ABRIGO","color":"NAVY","estacion":"WINTER","uso":"WORK"}""")
        val r = catalogarConIa(s, FOTO, jpegFalso).getOrThrow()
        assertEquals(GarmentType.COAT, r.type)
        assertEquals(GarmentColor.NAVY, r.color)
        val c = cuerpos.single()
        assertTrue("\"data\":\"QUJD\"" in c) // "ABC" en base64
        assertTrue("\"media_type\":\"image/jpeg\"" in c)
        assertTrue("\"json_schema\"" in c)
    }

    @Test
    fun `sin aviso aceptado, sin clave o desactivada no llama a la red`() = runTest {
        val sinAviso = servicio("{}", ajustes = LISTA.copy(avisosAceptados = setOf(AvisosIa.GENERAL.id)))
        assertIs<ErrorIa.AvisoPendiente>(catalogarConIa(sinAviso, FOTO, jpegFalso).exceptionOrNull())
        assertIs<ErrorIa.SinClave>(catalogarConIa(servicio("{}", boveda = Boveda(null)), FOTO, jpegFalso).exceptionOrNull())
        assertIs<ErrorIa.Desactivada>(catalogarConIa(servicio("{}", ajustes = LISTA.copy(activada = false)), FOTO, jpegFalso).exceptionOrNull())
        assertTrue(cuerpos.isEmpty())
    }

    @Test
    fun `una respuesta sin ningun valor valido es un error de formato`() = runTest {
        assertIs<ErrorIa.RespuestaInvalida>(catalogarConIa(servicio("""{"tipo":"nada"}"""), FOTO, jpegFalso).exceptionOrNull())
        assertIs<ErrorIa.RespuestaInvalida>(catalogarConIa(servicio("no json"), FOTO, jpegFalso).exceptionOrNull())
    }

    @Test
    fun `si la foto no se puede preparar no se llama a la red`() = runTest {
        val r = catalogarConIa(servicio("{}"), FOTO) { null }
        assertTrue(r.isFailure)
        assertTrue(cuerpos.isEmpty())
    }

    @Test
    fun `dimensiones reducidas mantienen la proporcion y no amplian`() {
        assertEquals(1024 to 768, dimensionesReducidas(4000, 3000))
        assertEquals(768 to 1024, dimensionesReducidas(3000, 4000))
        assertEquals(800 to 600, dimensionesReducidas(800, 600))
        assertEquals(1024 to 1, dimensionesReducidas(100000, 10))
    }
}
