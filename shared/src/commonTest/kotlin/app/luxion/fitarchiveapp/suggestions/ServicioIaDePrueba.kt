package app.luxion.fitarchiveapp.suggestions

import app.luxion.fitarchiveapp.ia.AjustesIa
import app.luxion.fitarchiveapp.ia.AlmacenAjustesIa
import app.luxion.fitarchiveapp.ia.AlmacenIa
import app.luxion.fitarchiveapp.ia.AvisosIa
import app.luxion.fitarchiveapp.ia.BovedaClaveIa
import app.luxion.fitarchiveapp.ia.ClienteClaude
import app.luxion.fitarchiveapp.ia.ServicioIa
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class BovedaDePrueba(var clave: String? = "sk-ant-api03-claveDePrueba123456") : BovedaClaveIa {
    override fun leer() = clave
    override fun escribir(clave: String) { this.clave = clave }
    override fun borrar() { clave = null }
}

internal class AjustesDePrueba(var ajustes: AjustesIa) : AlmacenAjustesIa {
    override fun cargar() = ajustes
    override fun guardar(ajustes: AjustesIa) { this.ajustes = ajustes }
}

internal val AJUSTES_CON_AVISO_DE_OUTFITS = AjustesIa(
    activada = true,
    avisosAceptados = setOf(AvisosIa.GENERAL.id, AVISO_OUTFITS_IA.id),
)

/** Respuesta 200 de la API cuyo texto es [texto] (el JSON estructurado de la IA). */
internal fun respuestaDeClaude(texto: String): String = buildJsonObject {
    put("content", buildJsonArray { add(buildJsonObject { put("type", "text"); put("text", texto) }) })
    put("stop_reason", "end_turn")
    put("usage", buildJsonObject { put("input_tokens", 10); put("output_tokens", 5) })
}.toString()

/** [ServicioIa] con MockEngine: responde [texto] (o el error HTTP [estado] si no es 200) y apunta los cuerpos enviados. */
internal class ServicioIaDePrueba(
    texto: String = """{"outfits":[]}""",
    estado: HttpStatusCode = HttpStatusCode.OK,
    boveda: BovedaDePrueba = BovedaDePrueba(),
    val ajustes: AjustesDePrueba = AjustesDePrueba(AJUSTES_CON_AVISO_DE_OUTFITS),
) {
    val cuerpos = mutableListOf<String>()
    val servicio = ServicioIa(
        ClienteClaude(
            HttpClient(MockEngine { req ->
                cuerpos += req.body.toByteArray().decodeToString()
                val cabeceras = headersOf(HttpHeaders.ContentType, "application/json")
                if (estado == HttpStatusCode.OK) respond(respuestaDeClaude(texto), HttpStatusCode.OK, cabeceras)
                else respond("{}", estado, cabeceras)
            }),
            espera = {},
        ),
        AlmacenIa(boveda, ajustes),
    ) { "2026-11-16" }
}
