package app.luxion.fitarchiveapp.weather

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Servicio del tiempo con Open-Meteo (gratuito, sin clave de API). */
class ServicioTiempoOpenMeteo(
    private val cliente: ClienteHttp,
    /** Caché corta (O18): evita consultar la red en cada apertura o regeneración. Sin caché, siempre consulta. */
    private val cache: CacheTiempo? = null,
    private val ubicacion: ProveedorUbicacion,
) : ServicioTiempo {

    override suspend fun tiempoDeHoy(ciudadElegida: Ciudad?): Result<TiempoDia> = llamar {
        val coords = (ubicacion.ubicacionAproximada() ?: ciudadElegida?.coordenadas)
            ?: throw ErrorTiempo.SinUbicacion
        val c = coords.aproximadas()
        cache?.obtener(c)?.let { return@llamar it }
        val url = "$URL_TIEMPO?latitude=${c.latitud}&longitude=${c.longitud}" +
            "&current=temperature_2m,weather_code" +
            "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
        val r = json.decodeFromString<RespuestaTiempo>(cliente.get(url))
        TiempoDia(
            temperaturaC = r.current.temperature,
            maximaC = r.daily.max.first(),
            minimaC = r.daily.min.first(),
            condicion = condicionDeCodigo(r.current.codigo),
        ).also { cache?.guardar(c, it) } // Solo se cachean las respuestas válidas: un fallo se puede reintentar.
    }

    override suspend fun buscarCiudades(texto: String): Result<List<Ciudad>> = llamar {
        if (texto.isBlank()) return@llamar emptyList()
        val url = "$URL_GEOCODIFICACION?name=${codificarUrl(texto.trim())}&count=5&language=es&format=json"
        json.decodeFromString<RespuestaCiudades>(cliente.get(url)).results.orEmpty().map {
            Ciudad(it.name, it.country, Coordenadas(it.latitude, it.longitude))
        }
    }

    private suspend fun <T> llamar(bloque: suspend () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ErrorTiempo) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(ErrorTiempo.Red(e))
    }

    @Serializable private class RespuestaTiempo(val current: Actual, val daily: Diario)

    @Serializable private class Actual(
        @SerialName("temperature_2m") val temperature: Double,
        @SerialName("weather_code") val codigo: Int,
    )

    @Serializable private class Diario(
        @SerialName("temperature_2m_max") val max: List<Double>,
        @SerialName("temperature_2m_min") val min: List<Double>,
    )

    @Serializable private class RespuestaCiudades(val results: List<Resultado>? = null)

    @Serializable private class Resultado(
        val name: String,
        val country: String? = null,
        val latitude: Double,
        val longitude: Double,
    )

    companion object {
        const val URL_TIEMPO = "https://api.open-meteo.com/v1/forecast"
        const val URL_GEOCODIFICACION = "https://geocoding-api.open-meteo.com/v1/search"
        private val json = Json { ignoreUnknownKeys = true }
    }
}

/** Códigos WMO de Open-Meteo. */
internal fun condicionDeCodigo(codigo: Int): CondicionTiempo = when (codigo) {
    0, 1 -> CondicionTiempo.DESPEJADO
    2, 3 -> CondicionTiempo.NUBLADO
    45, 48 -> CondicionTiempo.NIEBLA
    in 51..57 -> CondicionTiempo.LLOVIZNA
    in 61..67, in 80..82 -> CondicionTiempo.LLUVIA
    in 71..77, 85, 86 -> CondicionTiempo.NIEVE
    in 95..99 -> CondicionTiempo.TORMENTA
    else -> CondicionTiempo.NUBLADO
}

internal fun codificarUrl(texto: String): String = buildString {
    for (b in texto.encodeToByteArray()) {
        val c = b.toInt() and 0xFF
        val ch = c.toChar()
        if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch in "-._~") {
            append(ch)
        } else {
            append('%').append(HEX[c shr 4]).append(HEX[c and 15])
        }
    }
}

private const val HEX = "0123456789ABCDEF"

/** Cliente HTTP real (Ktor; motor según plataforma). */
class ClienteHttpKtor(private val http: HttpClient = HttpClient()) : ClienteHttp {
    override suspend fun get(url: String): String {
        val r = http.get(url)
        check(r.status.value in 200..299) { "HTTP ${r.status.value}" }
        return r.bodyAsText()
    }
}
