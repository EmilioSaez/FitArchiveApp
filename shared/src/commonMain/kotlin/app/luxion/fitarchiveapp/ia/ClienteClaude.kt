package app.luxion.fitarchiveapp.ia

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Parte del mensaje que se envía a Claude: texto o una imagen ya reducida y codificada en base64. */
sealed interface ParteClaude {
    data class Texto(val texto: String) : ParteClaude

    /** [base64] sin prefijo `data:`; [tipoMime] p. ej. `image/jpeg`. */
    data class Imagen(val base64: String, val tipoMime: String = "image/jpeg") : ParteClaude
}

data class PeticionClaude(
    val modelo: String,
    val partes: List<ParteClaude>,
    val sistema: String? = null,
    val maxTokens: Int = 1024,
    /** Esquema JSON de la salida estructurada; si es null la respuesta es texto libre. */
    val esquemaJson: JsonObject? = null,
)

data class RespuestaClaude(
    val texto: String,
    val motivoParada: String?,
    val tokensEntrada: Int,
    val tokensSalida: Int,
)

/** Datos del modelo devueltos por «Probar conexión». Null en una capacidad = la API no la informó. */
data class InfoModelo(val id: String, val nombre: String?, val admiteImagenes: Boolean?, val admiteSalidaEstructurada: Boolean?)

/**
 * Cliente HTTP directo de la API de Anthropic con la clave de la propia usuaria. No tiene plugin de logging a
 * propósito: la clave viaja en una cabecera y no debe acabar en ningún registro. Solo lo usa [ServicioIa].
 */
class ClienteClaude(
    private val http: HttpClient = crearHttpClaude(),
    private val espera: suspend (milisegundos: Long) -> Unit = { delay(it) },
) {
    suspend fun enviar(clave: String, peticion: PeticionClaude): RespuestaClaude = conReintentos {
        val respuesta = llamar { http.post("$URL_BASE/v1/messages") {
            cabeceras(clave)
            contentType(ContentType.Application.Json)
            setBody(cuerpo(peticion).toString())
        } }
        interpretar(respuesta.texto)
    }

    suspend fun consultarModelo(clave: String, modelo: String): InfoModelo = conReintentos {
        val respuesta = llamar { http.get("$URL_BASE/v1/models/$modelo") { cabeceras(clave) } }
        try {
            val o = json.parseToJsonElement(respuesta.texto).jsonObject
            val caps = o["capabilities"] as? JsonObject
            InfoModelo(
                id = o["id"]?.jsonPrimitive?.contentOrNull ?: modelo,
                nombre = o["display_name"]?.jsonPrimitive?.contentOrNull,
                admiteImagenes = caps?.soporta("image_input"),
                admiteSalidaEstructurada = caps?.soporta("structured_outputs"),
            )
        } catch (e: Exception) {
            throw ErrorIa.RespuestaInvalida(e)
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.cabeceras(clave: String) {
        header("x-api-key", clave)
        header("anthropic-version", VERSION_API)
    }

    private class Cruda(val estado: Int, val texto: String)

    /** Hace la llamada y traduce los fallos de red y los códigos de error HTTP a [ErrorIa]. */
    private suspend fun llamar(bloque: suspend () -> HttpResponse): Cruda {
        val r = try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ErrorIa.SinRed(e)
        }
        val estado = r.status.value
        if (estado in 200..299) return Cruda(estado, r.bodyAsText())
        throw when (estado) {
            401 -> ErrorIa.ClaveNoValida()
            402 -> ErrorIa.SinSaldo()
            403 -> ErrorIa.SinPermiso()
            404 -> ErrorIa.ModeloNoEncontrado()
            413 -> ErrorIa.DemasiadoGrande()
            429 -> ErrorIa.Limite(r.headers["retry-after"]?.toLongOrNull())
            in 500..599 -> ErrorIa.Servidor(estado)
            else -> ErrorIa.SolicitudInvalida()
        }
    }

    private suspend fun <T> conReintentos(bloque: suspend () -> T): T {
        var intento = 0
        while (true) {
            try {
                return bloque()
            } catch (e: ErrorIa) {
                if (!e.reintentable || intento >= MAX_REINTENTOS) throw e
                val sugerido = (e as? ErrorIa.Limite)?.reintentarEnSegundos?.takeIf { it <= 30 }
                espera((sugerido ?: (1L shl intento)) * 1000)
                intento++
            }
        }
    }

    private fun interpretar(texto: String): RespuestaClaude = try {
        val o = json.parseToJsonElement(texto).jsonObject
        val contenido = o["content"]!!.jsonArray
            .map { it.jsonObject }
            .filter { it["type"]?.jsonPrimitive?.contentOrNull == "text" }
            .joinToString("") { it["text"]!!.jsonPrimitive.content }
        val uso = o["usage"]?.jsonObject
        RespuestaClaude(
            texto = contenido,
            motivoParada = o["stop_reason"]?.jsonPrimitive?.contentOrNull,
            tokensEntrada = uso?.get("input_tokens")?.jsonPrimitive?.intOrNull ?: 0,
            tokensSalida = uso?.get("output_tokens")?.jsonPrimitive?.intOrNull ?: 0,
        )
    } catch (e: Exception) {
        throw ErrorIa.RespuestaInvalida(e)
    }

    private fun JsonObject.soporta(nombre: String): Boolean? =
        (this[nombre] as? JsonObject)?.get("supported")?.jsonPrimitive?.booleanOrNull

    companion object {
        const val URL_BASE = "https://api.anthropic.com"
        const val VERSION_API = "2023-06-01"
        const val MAX_REINTENTOS = 2
        private val json = Json { ignoreUnknownKeys = true }

        internal fun cuerpo(p: PeticionClaude): JsonObject = buildJsonObject {
            put("model", p.modelo)
            put("max_tokens", p.maxTokens)
            p.sistema?.let { put("system", it) }
            putJsonArray("messages") {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", buildJsonArray {
                        p.partes.forEach { parte ->
                            add(buildJsonObject {
                                when (parte) {
                                    is ParteClaude.Texto -> {
                                        put("type", "text")
                                        put("text", parte.texto)
                                    }
                                    is ParteClaude.Imagen -> {
                                        put("type", "image")
                                        putJsonObject("source") {
                                            put("type", "base64")
                                            put("media_type", parte.tipoMime)
                                            put("data", parte.base64)
                                        }
                                    }
                                }
                            })
                        }
                    })
                })
            }
            putJsonObject("output_config") {
                put("effort", "low")
                p.esquemaJson?.let { esquema ->
                    putJsonObject("format") {
                        put("type", "json_schema")
                        put("schema", esquema)
                    }
                }
            }
        }
    }
}

/** Cliente Ktor con los tiempos de espera de la IA y sin logging (la clave viaja en una cabecera). */
fun crearHttpClaude(): HttpClient = HttpClient {
    install(HttpTimeout) {
        requestTimeoutMillis = 120_000
        connectTimeoutMillis = 15_000
    }
}
