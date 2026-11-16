package app.luxion.fitarchiveapp.avatar.ia

import app.luxion.fitarchiveapp.avatar.AjustePrenda
import app.luxion.fitarchiveapp.avatar.Subtipo
import app.luxion.fitarchiveapp.ia.AjustesIa
import app.luxion.fitarchiveapp.ia.AlmacenAjustesIa
import app.luxion.fitarchiveapp.ia.AlmacenIa
import app.luxion.fitarchiveapp.ia.AvisosIa
import app.luxion.fitarchiveapp.ia.BovedaClaveIa
import app.luxion.fitarchiveapp.ia.ClienteClaude
import app.luxion.fitarchiveapp.ia.ErrorIa
import app.luxion.fitarchiveapp.ia.ServicioIa
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
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
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
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

private fun prenda(id: Long, tipo: GarmentType, subtipo: Subtipo? = null) = Garment(
    id = id, photoPath = "p$id.png", type = tipo, color = GarmentColor.BLACK, season = Season.SUMMER,
    usage = Usage.CASUAL, brand = "MarcaSecreta", size = "M", favorite = false, dirty = false,
    wearsSinceWash = 0, totalWears = 0, washAfterWears = null, createdAt = 1, subtipo = subtipo,
)

/** Envuelve [texto] (el JSON de la propuesta) como respuesta de la API de mensajes. */
private fun respuesta(texto: String): String {
    val escapado = JsonPrimitive(texto).toString()
    return """{"content":[{"type":"text","text":$escapado}],"stop_reason":"end_turn","usage":{"input_tokens":10,"output_tokens":3}}"""
}

class AjusteIaAvatarTest {
    private val camiseta = prenda(1, GarmentType.TSHIRT)
    private val falda = prenda(2, GarmentType.SKIRT)
    private val prendas = listOf(camiseta, falda)

    private val cuerpos = mutableListOf<String>()

    private fun servicio(
        texto: String = "{}",
        ajustes: AjustesIa = AjustesIa(activada = true, avisosAceptados = setOf(AVISO_IA_AVATAR.id)),
        clave: String? = "sk-ant-api03-claveDePrueba123456",
    ): ServicioIa {
        val motor = MockEngine { req ->
            cuerpos += req.body.toByteArray().decodeToString()
            respond(respuesta(texto), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return ServicioIa(ClienteClaude(HttpClient(motor), espera = {}), AlmacenIa(Boveda(clave), Ajustes(ajustes))) { "2026-11-16" }
    }

    // --- Parseo ---

    @Test
    fun `parsea subtipo, desplazamiento y comentario validos`() {
        val p = interpretarPropuesta(
            """{"prendas":[{"id":1,"subtipo":"TOP_MANGA_LARGA","dx":4,"dy":-6},{"id":2,"subtipo":"BOTTOM_FALDA_MINI","dx":0,"dy":0}],"comentario":"  Look  fresco\ny sencillo. "}""",
            prendas,
        )!!
        assertEquals(2, p.prendas.size)
        assertEquals(PropuestaPrenda(1, Subtipo.TOP_MANGA_LARGA, AjustePrenda(4f, -6f, 1f)), p.prendas[0])
        assertEquals(Subtipo.BOTTOM_FALDA_MINI, p.prendas[1].subtipo)
        assertEquals("Look fresco y sencillo.", p.comentario)
    }

    // --- Clamp ---

    @Test
    fun `un desplazamiento absurdo se limita a un cuarto de la caja`() {
        val p = interpretarPropuesta(
            """{"prendas":[{"id":1,"subtipo":"TOP_MANGA_CORTA","dx":99999,"dy":-99999}],"comentario":""}""", prendas,
        )!!
        val caja = Subtipo.TOP_MANGA_CORTA.caja
        assertEquals(caja.w * FRACCION_MOVIMIENTO_IA, p.prendas.single().ajuste.dx)
        assertEquals(-caja.h * FRACCION_MOVIMIENTO_IA, p.prendas.single().ajuste.dy)
    }

    @Test
    fun `la escala nunca la cambia la IA aunque la mande`() {
        val p = interpretarPropuesta(
            """{"prendas":[{"id":1,"subtipo":"TOP_MANGA_CORTA","dx":0,"dy":0,"escala":9}],"comentario":"ok"}""", prendas,
        )!!
        assertEquals(AjustePrenda.ESCALA_AUTOMATICA, p.prendas.single().ajuste.escala)
    }

    @Test
    fun `dx y dy no numericos o ausentes valen cero`() {
        val p = interpretarPropuesta(
            """{"prendas":[{"id":1,"subtipo":"TOP_MANGA_CORTA","dx":"mucho","dy":null},{"id":2,"subtipo":"BOTTOM_FALDA_MIDI"}],"comentario":"ok"}""",
            prendas,
        )!!
        assertEquals(AjustePrenda.Ninguno, p.prendas[0].ajuste)
        assertEquals(AjustePrenda.Ninguno, p.prendas[1].ajuste)
    }

    // --- Descarte ---

    @Test
    fun `descarta ids desconocidos y repetidos`() {
        val p = interpretarPropuesta(
            """{"prendas":[{"id":99,"subtipo":"TOP_MANGA_LARGA","dx":0,"dy":0},{"id":1,"subtipo":"TOP_MANGA_LARGA","dx":1,"dy":1},{"id":1,"subtipo":"TOP_SIN_MANGAS_CORTO","dx":9,"dy":9},{"id":"x"}],"comentario":"ok"}""",
            prendas,
        )!!
        assertEquals(listOf(1L), p.prendas.map { it.prendaId })
        assertEquals(Subtipo.TOP_MANGA_LARGA, p.prendas.single().subtipo)
    }

    @Test
    fun `un subtipo desconocido o de otro tipo se descarta y queda el actual`() {
        val p = interpretarPropuesta(
            """{"prendas":[{"id":1,"subtipo":"SHOES_PAR","dx":0,"dy":0},{"id":2,"subtipo":"INVENTADO","dx":0,"dy":0}],"comentario":"ok"}""",
            prendas,
        )!!
        assertEquals(Subtipo.TOP_MANGA_CORTA, p.prendas[0].subtipo)
        assertEquals(Subtipo.BOTTOM_FALDA_MIDI, p.prendas[1].subtipo)
    }

    @Test
    fun `usa el subtipo guardado de la prenda como actual`() {
        val g = prenda(1, GarmentType.TSHIRT, Subtipo.TOP_LARGO_CADERA)
        val p = interpretarPropuesta("""{"prendas":[{"id":1,"subtipo":"NOPE","dx":0,"dy":0}],"comentario":""}""", listOf(g))!!
        assertEquals(Subtipo.TOP_LARGO_CADERA, p.prendas.single().subtipo)
    }

    @Test
    fun `json roto, sin objeto o vacio da null`() {
        assertNull(interpretarPropuesta("no es json", prendas))
        assertNull(interpretarPropuesta("[1,2]", prendas))
        assertNull(interpretarPropuesta("""{"prendas":[],"comentario":"   "}""", prendas))
        assertNull(interpretarPropuesta("""{"prendas":[{"id":99,"subtipo":"TOP_MANGA_LARGA"}]}""", prendas))
    }

    @Test
    fun `solo comentario es una propuesta valida y se recorta`() {
        val p = interpretarPropuesta("""{"prendas":[],"comentario":"${"a".repeat(1000)}"}""", prendas)!!
        assertTrue(p.prendas.isEmpty())
        assertEquals(MAX_COMENTARIO, p.comentario!!.length)
    }

    // --- Esquema ---

    @Test
    fun `el esquema lista todos los subtipos y exige los campos`() {
        val texto = esquemaAjusteAvatar().toString()
        Subtipo.entries.forEach { assertTrue("\"${it.name}\"" in texto) }
        assertTrue("\"additionalProperties\":false" in texto)
        assertFalse("escala" in texto)
    }

    // --- Con ServicioIa + MockEngine ---

    @Test
    fun `proponer envia solo tipo, color y subtipo con aviso propio y devuelve la propuesta`() = runTest {
        val s = servicio("""{"prendas":[{"id":1,"subtipo":"TOP_MANGA_LARGA","dx":2,"dy":3}],"comentario":"Bonito"}""")
        val p = s.proponerAjusteAvatar(prendas).getOrThrow()
        assertEquals("Bonito", p.comentario)
        val cuerpo = cuerpos.single()
        assertTrue("json_schema" in cuerpo)
        assertTrue("TSHIRT" in cuerpo && "BOTTOM_FALDA_MIDI" in cuerpo)
        assertFalse("MarcaSecreta" in cuerpo)
        assertFalse("\"type\":\"image\"" in cuerpo)
        assertNotEquals(AvisosIa.GENERAL.id, AVISO_IA_AVATAR.id)
    }

    @Test
    fun `sin aviso de avatar aceptado no llama aunque el general si`() = runTest {
        val s = servicio(ajustes = AjustesIa(activada = true, avisosAceptados = setOf(AvisosIa.GENERAL.id)))
        assertIs<ErrorIa.AvisoPendiente>(s.bloqueo(AVISO_IA_AVATAR))
        assertIs<ErrorIa.AvisoPendiente>(s.proponerAjusteAvatar(prendas).exceptionOrNull())
        assertTrue(cuerpos.isEmpty())
    }

    @Test
    fun `sin clave o desactivada falla con ErrorIa y sin red`() = runTest {
        assertIs<ErrorIa.SinClave>(servicio(clave = null).proponerAjusteAvatar(prendas).exceptionOrNull())
        val off = servicio(ajustes = AjustesIa(activada = false, avisosAceptados = setOf(AVISO_IA_AVATAR.id)))
        assertIs<ErrorIa.Desactivada>(off.proponerAjusteAvatar(prendas).exceptionOrNull())
        assertTrue(cuerpos.isEmpty())
    }

    @Test
    fun `una respuesta inutilizable da RespuestaInvalida`() = runTest {
        val r = servicio("""{"prendas":[{"id":99}],"comentario":""}""").proponerAjusteAvatar(prendas).exceptionOrNull()
        assertIs<ErrorIa.RespuestaInvalida>(r)
    }

    @Test
    fun `sin prendas no se llama`() = runTest {
        assertTrue(servicio().proponerAjusteAvatar(emptyList()).isFailure)
        assertTrue(cuerpos.isEmpty())
    }
}
