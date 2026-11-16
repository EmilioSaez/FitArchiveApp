package app.luxion.fitarchiveapp.ia

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val MODELO_POR_DEFECTO = "claude-haiku-5-5"
const val TOPE_DIARIO_POR_DEFECTO = 30
val RANGO_TOPE_DIARIO = 1..500

/**
 * Ajustes no sensibles de la IA (la clave NO está aquí: va en [BovedaClaveIa]).
 * [fechaUso] es el día (ISO) al que corresponde [usosHoy]; si es otro día, el contador cuenta como 0.
 */
@Serializable
data class AjustesIa(
    val activada: Boolean = false,
    val modelo: String = MODELO_POR_DEFECTO,
    val avisosAceptados: Set<String> = emptySet(),
    val topeDiario: Int = TOPE_DIARIO_POR_DEFECTO,
    val fechaUso: String? = null,
    val usosHoy: Int = 0,
    val tokensEntradaTotal: Long = 0,
    val tokensSalidaTotal: Long = 0,
) {
    fun usosEn(fecha: String) = if (fechaUso == fecha) usosHoy else 0
}

/** Serialización estable de [AjustesIa] como texto; si está corrupto o ausente, cae a los valores por defecto. */
object AjustesIaCodec {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun codificar(a: AjustesIa): String = json.encodeToString(AjustesIa.serializer(), a)

    fun decodificar(texto: String?): AjustesIa =
        if (texto.isNullOrBlank()) AjustesIa() else runCatching { json.decodeFromString(AjustesIa.serializer(), texto) }.getOrDefault(AjustesIa())
}

/** Persistencia de los ajustes no sensibles de la IA. */
interface AlmacenAjustesIa {
    fun cargar(): AjustesIa
    fun guardar(ajustes: AjustesIa)
}

/**
 * Almacén seguro de la clave de API de la usuaria. Nunca en base de datos, copia de seguridad ni exportación.
 * [escribir] lanza [ErrorIa.Almacen] si no puede guardarla de forma segura (no degrada a texto plano);
 * [leer] devuelve null si no hay clave o no se puede descifrar.
 */
interface BovedaClaveIa {
    fun leer(): String?
    fun escribir(clave: String)
    fun borrar()
}

class AlmacenIa(val boveda: BovedaClaveIa, val ajustes: AlmacenAjustesIa)

/** Almacén de la IA de la plataforma: Keystore/Keychain para la clave y preferencias para los ajustes. */
@Composable
expect fun rememberAlmacenIa(): AlmacenIa
