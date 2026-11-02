package com.emilio.fitarchive.weather

/** Coordenadas del móvil o de una ciudad. */
data class Coordenadas(val latitud: Double, val longitud: Double) {
    /** Redondeadas a ~11 km: el tiempo no necesita precisión y así enviamos menos. */
    fun aproximadas(): Coordenadas = Coordenadas(redondear1(latitud), redondear1(longitud))
}

private fun redondear1(v: Double): Double = kotlin.math.round(v * 10) / 10

/** Ciudad elegida a mano (resultado de la búsqueda). */
data class Ciudad(val nombre: String, val pais: String?, val coordenadas: Coordenadas)

enum class CondicionTiempo { DESPEJADO, NUBLADO, NIEBLA, LLOVIZNA, LLUVIA, NIEVE, TORMENTA }

data class TiempoDia(
    val temperaturaC: Double,
    val maximaC: Double,
    val minimaC: Double,
    val condicion: CondicionTiempo,
)

/** Error del servicio del tiempo; la UI decide qué mensaje mostrar. */
sealed class ErrorTiempo(cause: Throwable? = null) : Exception(cause) {
    /** Sin permiso de ubicación y sin ciudad elegida. */
    data object SinUbicacion : ErrorTiempo()

    /** Sin conexión o respuesta no válida del servicio. */
    class Red(cause: Throwable) : ErrorTiempo(cause)
}

/** Ubicación aproximada del móvil; `null` si no hay permiso o no se pudo obtener. Implementación por plataforma. */
fun interface ProveedorUbicacion {
    suspend fun ubicacionAproximada(): Coordenadas?
}

/** Interfaz común del servicio del tiempo. */
interface ServicioTiempo {
    /**
     * Tiempo de hoy. Usa la ubicación del móvil si hay permiso; si no, [ciudadElegida].
     * Falla con [ErrorTiempo.SinUbicacion] si no hay ninguna de las dos.
     */
    suspend fun tiempoDeHoy(ciudadElegida: Ciudad?): Result<TiempoDia>

    /** Busca ciudades por nombre para el selector manual. */
    suspend fun buscarCiudades(texto: String): Result<List<Ciudad>>
}

/** Cliente HTTP mínimo (GET que devuelve el cuerpo); se sustituye por uno simulado en tests. */
fun interface ClienteHttp {
    suspend fun get(url: String): String
}
