package app.luxion.fitarchiveapp.ia

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock

/** Lo que la interfaz necesita saber de la IA. [usosHoy] ya está normalizado al día actual. */
data class EstadoIa(val ajustes: AjustesIa, val tieneClave: Boolean, val usosHoy: Int) {
    val lista get() = ajustes.activada && tieneClave
}

/**
 * Único punto de entrada a la IA: nada más llama a [ClienteClaude]. Antes de cada llamada exige IA activada,
 * clave guardada, aviso aceptado y tope diario sin alcanzar. Sin clave la IA queda inactiva y el resto de la app
 * no cambia.
 */
class ServicioIa(
    private val cliente: ClienteClaude,
    private val almacen: AlmacenIa,
    private val hoy: () -> String = { Clock.System.todayIn(TimeZone.currentSystemDefault()).toString() },
) {
    private val _estado = MutableStateFlow(leerEstado())
    val estado: StateFlow<EstadoIa> = _estado.asStateFlow()

    private fun leerEstado(): EstadoIa {
        val a = almacen.ajustes.cargar()
        return EstadoIa(a, tieneClave = almacen.boveda.leer() != null, usosHoy = a.usosEn(hoy()))
    }

    private fun refrescar() {
        _estado.value = leerEstado()
    }

    private fun modificar(cambio: (AjustesIa) -> AjustesIa) {
        almacen.ajustes.guardar(cambio(almacen.ajustes.cargar()))
        refrescar()
    }

    /** Por qué no se puede llamar ahora a la IA, o null si se puede. */
    fun bloqueo(aviso: AvisoIa = AvisosIa.GENERAL): ErrorIa? {
        val e = _estado.value
        return when {
            !e.tieneClave -> ErrorIa.SinClave()
            !e.ajustes.activada -> ErrorIa.Desactivada()
            aviso.id !in e.ajustes.avisosAceptados -> ErrorIa.AvisoPendiente(aviso)
            e.usosHoy >= e.ajustes.topeDiario -> ErrorIa.TopeAlcanzado(e.ajustes.topeDiario)
            else -> null
        }
    }

    suspend fun preguntar(
        partes: List<ParteClaude>,
        sistema: String? = null,
        esquemaJson: JsonObject? = null,
        maxTokens: Int = 1024,
        aviso: AvisoIa = AvisosIa.GENERAL,
    ): Result<RespuestaClaude> {
        bloqueo(aviso)?.let { return Result.failure(it) }
        val clave = almacen.boveda.leer() ?: return Result.failure(ErrorIa.SinClave())
        val peticion = PeticionClaude(_estado.value.ajustes.modelo, partes, sistema, maxTokens, esquemaJson)
        return try {
            val r = cliente.enviar(clave, peticion)
            contar(r) // Cuenta toda respuesta 200, también una negativa o una respuesta cortada.
            when (r.motivoParada) {
                "refusal" -> Result.failure(ErrorIa.Rechazo())
                "max_tokens" -> Result.failure(ErrorIa.Truncada())
                else -> Result.success(r)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorIa) {
            if (e is ErrorIa.ClaveNoValida) invalidarClave()
            Result.failure(e)
        }
    }

    suspend fun preguntarTexto(
        pregunta: String,
        sistema: String? = null,
        maxTokens: Int = 1024,
        aviso: AvisoIa = AvisosIa.GENERAL,
    ): Result<String> = preguntar(listOf(ParteClaude.Texto(pregunta)), sistema, null, maxTokens, aviso).map { it.texto }

    /** Pide una respuesta que cumpla [esquemaJson]; devuelve el JSON como texto para decodificarlo con su tipo. */
    suspend fun preguntarJson(
        partes: List<ParteClaude>,
        esquemaJson: JsonObject,
        sistema: String? = null,
        maxTokens: Int = 1024,
        aviso: AvisoIa = AvisosIa.GENERAL,
    ): Result<String> = preguntar(partes, sistema, esquemaJson, maxTokens, aviso).map { it.texto }

    private fun contar(r: RespuestaClaude) = modificar {
        val fecha = hoy()
        it.copy(
            fechaUso = fecha,
            usosHoy = it.usosEn(fecha) + 1,
            tokensEntradaTotal = it.tokensEntradaTotal + r.tokensEntrada,
            tokensSalidaTotal = it.tokensSalidaTotal + r.tokensSalida,
        )
    }

    // --- Ajustes y clave ---

    /** Valida y guarda la clave. No la comprueba contra la API: para eso, [probarConexion]. */
    fun guardarClave(clave: String): Result<Unit> {
        val limpia = clave.trim()
        if (!esClavePlausible(limpia)) {
            return Result.failure(IllegalArgumentException("La clave no parece válida: debe empezar por sk-ant- y no tener espacios."))
        }
        return try {
            almacen.boveda.escribir(limpia)
            refrescar()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e as? ErrorIa ?: ErrorIa.Almacen(e))
        }
    }

    /** Borra la clave y desactiva la IA. */
    fun borrarClave() {
        almacen.boveda.borrar()
        modificar { it.copy(activada = false) }
    }

    private fun invalidarClave() = borrarClave()

    /** Comprueba la clave contra `GET /v1/models/{modelo}`. Una clave rechazada (401) se borra y desactiva la IA. */
    suspend fun probarConexion(): Result<InfoModelo> {
        val clave = almacen.boveda.leer() ?: return Result.failure(ErrorIa.SinClave())
        return try {
            Result.success(cliente.consultarModelo(clave, _estado.value.ajustes.modelo))
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorIa) {
            if (e is ErrorIa.ClaveNoValida) invalidarClave()
            Result.failure(e)
        }
    }

    fun activar(activada: Boolean) = modificar { it.copy(activada = activada) }

    fun aceptarAviso(aviso: AvisoIa) = modificar { it.copy(avisosAceptados = it.avisosAceptados + aviso.id) }

    fun cambiarModelo(modelo: String) {
        val limpio = modelo.trim()
        if (limpio.isNotEmpty()) modificar { it.copy(modelo = limpio) }
    }

    fun cambiarTopeDiario(tope: Int) = modificar { it.copy(topeDiario = tope.coerceIn(RANGO_TOPE_DIARIO)) }

    companion object {
        /** Comprobación local previa a guardar: prefijo de las claves de Anthropic, longitud mínima y sin espacios. */
        fun esClavePlausible(clave: String) =
            clave.startsWith("sk-ant-") && clave.length >= 20 && clave.none { it.isWhitespace() }
    }
}
