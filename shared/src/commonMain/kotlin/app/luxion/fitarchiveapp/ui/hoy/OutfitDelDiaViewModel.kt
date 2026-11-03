package app.luxion.fitarchiveapp.ui.hoy

import app.luxion.fitarchiveapp.avatar.slotDe
import app.luxion.fitarchiveapp.data.CalendarRepository
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.data.UsoRepository
import app.luxion.fitarchiveapp.model.CalendarDay
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.WearRecord
import app.luxion.fitarchiveapp.suggestions.AfinidadAprendida
import app.luxion.fitarchiveapp.suggestions.AfinidadEstilo
import app.luxion.fitarchiveapp.suggestions.DesgloseSugerencia
import app.luxion.fitarchiveapp.suggestions.MotorSugerencias
import app.luxion.fitarchiveapp.suggestions.OutfitGuardado
import app.luxion.fitarchiveapp.suggestions.OutfitSugerido
import app.luxion.fitarchiveapp.suggestions.PedidoSugerencia
import app.luxion.fitarchiveapp.ui.outfit.PrendaElegida
import app.luxion.fitarchiveapp.ui.outfit.aOutfitGarments
import app.luxion.fitarchiveapp.weather.Ciudad
import app.luxion.fitarchiveapp.weather.CondicionTiempo
import app.luxion.fitarchiveapp.weather.ErrorTiempo
import app.luxion.fitarchiveapp.weather.ServicioTiempo
import app.luxion.fitarchiveapp.weather.TiempoDia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tiempo de hoy tal como lo ve la pantalla. */
sealed interface EstadoTiempo {
    data object Cargando : EstadoTiempo
    data class Disponible(val tiempo: TiempoDia) : EstadoTiempo

    /** Sin permiso de ubicación y sin ciudad elegida: se sugiere sin clima y se invita a configurarla. */
    data object SinUbicacion : EstadoTiempo

    /** Falló la consulta (sin conexión o respuesta no válida): se sugiere sin clima y se puede reintentar. */
    data object SinConexion : EstadoTiempo
}

data class OutfitDelDiaUiState(
    /** Fecha ISO de hoy. */
    val hoy: String,
    val armario: List<Garment> = emptyList(),
    val armarioCargado: Boolean = false,
    val tiempo: EstadoTiempo = EstadoTiempo.Cargando,
    val ocasion: Occasion? = null,
    /** La mejor primero; la elegida es [indice]. */
    val sugerencias: List<OutfitSugerido> = emptyList(),
    val indice: Int = 0,
    /** Outfits ya guardados en esta sesión, por las prendas (ids ordenados) que los componen. */
    val guardados: Map<List<Long>, Long> = emptyMap(),
    /** Prendas que ya constan en el historial de usos (para distinguir "sin estrenar" de "hace días"). */
    val prendasUsadas: Set<Long> = emptySet(),
    /** Outfit asignado hoy en el calendario, si hay. */
    val asignadoHoy: Long? = null,
    val ocupado: Boolean = false,
    /** Confirmación breve de la última acción. */
    val aviso: String? = null,
    val error: String? = null,
) {
    val cargando get() = !armarioCargado || tiempo == EstadoTiempo.Cargando
    val actual: OutfitSugerido? get() = sugerencias.getOrNull(indice)
    val tiempoDia: TiempoDia? get() = (tiempo as? EstadoTiempo.Disponible)?.tiempo
    private val idGuardadoActual: Long? get() = actual?.let { guardados[it.clave()] }
    val guardadoActual get() = idGuardadoActual != null
    val aceptadoActual get() = idGuardadoActual != null && idGuardadoActual == asignadoHoy
    val hayVariasOpciones get() = sugerencias.size > 1

    /** La sugerencia actual solo tiene prendas que nunca se han llevado. */
    val actualSinEstrenar get() = actual?.prendas?.all { it.id !in prendasUsadas } ?: false

    /** Sin sugerencias y sin estar cargando: la pantalla explica por qué. */
    val vacio get() = !cargando && sugerencias.isEmpty()
    val armarioSinPrendas get() = armarioCargado && armario.isEmpty()
}

/**
 * Estado de la pantalla "outfit del día" (T43): arma el [PedidoSugerencia] con armario, tiempo (T39), usos (T33)
 * y ocasión opcional, y reparte las sugerencias del [motor] (T41). Aceptar asigna el outfit a hoy en el calendario (T32).
 * La afinidad de estilo (T42) se aprende de likes, outfits guardados y usos; sin nada de eso va a null.
 */
class OutfitDelDiaViewModel(
    private val motor: MotorSugerencias,
    armario: GarmentRepository,
    private val usos: UsoRepository,
    private val calendario: CalendarRepository,
    private val outfits: OutfitRepository,
    private val tiempoServicio: ServicioTiempo,
    private val ciudad: () -> Ciudad?,
    private val scope: CoroutineScope,
    hoy: String,
    private val ahora: () -> Long,
) {
    private val _estado = MutableStateFlow(OutfitDelDiaUiState(hoy))
    val estado: StateFlow<OutfitDelDiaUiState> = _estado.asStateFlow()

    private var historial: List<WearRecord> = emptyList()
    private var guardadosParaAfinidad: List<OutfitGuardado> = emptyList()

    init {
        scope.launch {
            armario.observeAll().collect { prendas ->
                _estado.update { it.copy(armario = prendas, armarioCargado = true) }
                recalcular()
            }
        }
        scope.launch { calendario.observe(hoy).collect { dia -> _estado.update { it.copy(asignadoHoy = dia?.outfitId) } } }
        scope.launch { cargarEntorno() }
    }

    /** Vuelve a pedir tiempo y usos y regenera las sugerencias desde la primera. */
    fun regenerar() {
        _estado.update { it.copy(aviso = null, error = null) }
        scope.launch { cargarEntorno() }
    }

    /** Pasa a la siguiente alternativa (vuelve a la primera tras la última). */
    fun siguiente() = _estado.update { e ->
        if (e.sugerencias.isEmpty()) e else e.copy(indice = (e.indice + 1) % e.sugerencias.size, aviso = null)
    }

    fun elegir(indice: Int) = _estado.update { e ->
        if (indice in e.sugerencias.indices) e.copy(indice = indice, aviso = null) else e
    }

    /** Filtra por [ocasion] (null = cualquiera) y regenera. */
    fun elegirOcasion(ocasion: Occasion?) {
        _estado.update { it.copy(ocasion = ocasion, aviso = null) }
        recalcular()
    }

    /** Guarda la sugerencia actual como outfit (si no lo estaba ya). */
    fun guardar() = accion("No se pudo guardar el outfit", "Outfit guardado") { asegurarGuardado(it) }

    /** Asigna la sugerencia actual a hoy en el calendario, guardándola antes como outfit; conserva la nota del día. */
    fun aceptar() = accion("No se pudo asignar el outfit a hoy", "Outfit asignado a hoy") { sugerido ->
        val id = asegurarGuardado(sugerido)
        val nota = calendario.observe(_estado.value.hoy).first()?.note
        calendario.set(CalendarDay(_estado.value.hoy, id, nota))
    }

    fun descartarError() = _estado.update { it.copy(error = null) }

    fun descartarAviso() = _estado.update { it.copy(aviso = null) }

    private suspend fun asegurarGuardado(sugerido: OutfitSugerido): Long {
        _estado.value.guardados[sugerido.clave()]?.let { return it }
        val estado = _estado.value
        val elegidas = sugerido.prendas.associate { slotDe(it.type) to PrendaElegida(it) }
        val nombres = outfits.observeAll().first().map { it.name }.toSet()
        val id = outfits.create(
            Outfit(0, nombreOutfitUnico(nombreOutfit(estado.hoy), nombres), estado.ocasion, favorite = false, createdAt = ahora()),
            elegidas.aOutfitGarments(),
        )
        _estado.update { it.copy(guardados = it.guardados + (sugerido.clave() to id)) }
        return id
    }

    private fun accion(mensajeError: String, aviso: String, bloque: suspend (OutfitSugerido) -> Unit) {
        val sugerido = _estado.value.actual ?: return
        if (_estado.value.ocupado) return
        _estado.update { it.copy(ocupado = true, aviso = null, error = null) }
        scope.launch {
            try {
                bloque(sugerido)
                _estado.update { it.copy(ocupado = false, aviso = aviso) }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                _estado.update { it.copy(ocupado = false, error = mensajeError) }
            }
        }
    }

    private suspend fun cargarEntorno() {
        _estado.update { it.copy(tiempo = EstadoTiempo.Cargando) }
        historial = try {
            usos.todos()
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            emptyList() // Sin historial solo se pierde la rotación de prendas.
        }
        val existentes = try {
            outfits.observeAll().first().mapNotNull { o -> outfits.get(o.id) }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            emptyList() // Sin outfits solo se pierde esa señal de gusto.
        }
        val usadas = historial.map { it.garmentId }.toSet()
        _estado.update { it.copy(prendasUsadas = usadas) }
        guardadosParaAfinidad = existentes.map { OutfitGuardado.de(it.outfit, it.garments) }
        // Reconoce como ya guardados los outfits de la BD (p. ej. tras reiniciar): no se duplican al guardar o aceptar.
        val guardados = existentes.associate { o -> o.garments.map { it.garmentId }.sorted() to o.outfit.id }
        _estado.update { it.copy(guardados = guardados) }
        val tiempo = try {
            tiempoServicio.tiempoDeHoy(ciudad()).fold(
                onSuccess = { EstadoTiempo.Disponible(it) },
                onFailure = { if (it is ErrorTiempo.SinUbicacion) EstadoTiempo.SinUbicacion else EstadoTiempo.SinConexion },
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            EstadoTiempo.SinConexion
        }
        _estado.update { it.copy(tiempo = tiempo) }
        recalcular()
    }

    private fun pedido(e: OutfitDelDiaUiState) = PedidoSugerencia(
        armario = e.armario,
        hoy = e.hoy,
        tiempo = e.tiempoDia,
        ocasion = e.ocasion,
        usos = historial,
        afinidad = afinidad(e),
    )

    /** Null sin ninguna señal de gusto (likes, outfits guardados o usos): así no se muestra un "Tu estilo" neutro. */
    private fun afinidad(e: OutfitDelDiaUiState): AfinidadEstilo? {
        val hayEvidencia = e.armario.any { it.favorite } || guardadosParaAfinidad.isNotEmpty() || historial.isNotEmpty()
        return if (hayEvidencia) AfinidadAprendida.desde(e.armario, guardadosParaAfinidad, historial) else null
    }

    /** Recalcula las sugerencias cuando ya hay armario y el tiempo ha terminado de cargar. */
    private fun recalcular() {
        val e = _estado.value
        if (e.cargando) return
        val sugerencias = motor.sugerir(pedido(e))
        _estado.update { it.copy(sugerencias = sugerencias, indice = 0) }
    }
}

private fun OutfitSugerido.clave(): List<Long> = prendas.map { it.id }.sorted()

/** "Outfit del 02/11" a partir de una fecha ISO. */
internal fun nombreOutfit(hoy: String): String {
    val p = hoy.split("-")
    return if (p.size == 3) "Outfit del ${p[2]}/${p[1]}" else "Outfit del día"
}

/** [base] si está libre; si no, "[base] (2)", "[base] (3)"… para distinguir outfits guardados el mismo día. */
internal fun nombreOutfitUnico(base: String, existentes: Set<String>): String {
    if (base !in existentes) return base
    var n = 2
    while ("$base ($n)" in existentes) n++
    return "$base ($n)"
}

/** Un criterio del desglose con su puntuación de 0.0 a 1.0. */
data class CriterioPuntuado(val etiqueta: String, val valor: Double)

/** Criterios que tienen sentido mostrar: sin tiempo no hay clima, sin ocasión no hay ocasión, sin personalización no hay afinidad. */
fun DesgloseSugerencia.criterios(hayTiempo: Boolean, hayOcasion: Boolean): List<CriterioPuntuado> = listOfNotNull(
    CriterioPuntuado("Clima", clima).takeIf { hayTiempo },
    CriterioPuntuado("Ocasión", ocasion).takeIf { hayOcasion },
    CriterioPuntuado("Colores", colores),
    CriterioPuntuado("Frescura", frescura),
    afinidad?.let { CriterioPuntuado("Tu estilo", it) },
)

/** Umbral de puntuación a partir del cual un criterio se cita como motivo. */
private const val UMBRAL_MOTIVO = 0.7

/** Frase corta con los (hasta dos) criterios en que más destaca la sugerencia. */
fun motivoBreve(
    desglose: DesgloseSugerencia,
    hayTiempo: Boolean,
    hayOcasion: Boolean,
    sinEstrenar: Boolean = false,
): String {
    val frases = mapOf(
        "Clima" to "va bien con el tiempo de hoy",
        "Ocasión" to "encaja con la ocasión",
        "Colores" to "los colores combinan",
        "Frescura" to if (sinEstrenar) "aún no has estrenado estas prendas" else "hace días que no llevas estas prendas",
        "Tu estilo" to "es muy de tu estilo",
    )
    val destacados = desglose.criterios(hayTiempo, hayOcasion)
        .filter { it.valor >= UMBRAL_MOTIVO }
        .sortedByDescending { it.valor }
        .take(2)
        .mapNotNull { frases[it.etiqueta] }
    if (destacados.isEmpty()) return "Es la mejor combinación posible con tu armario."
    return destacados.joinToString(" y ").replaceFirstChar { it.uppercase() } + "."
}

fun CondicionTiempo.descripcion() = when (this) {
    CondicionTiempo.DESPEJADO -> "Despejado"
    CondicionTiempo.NUBLADO -> "Nublado"
    CondicionTiempo.NIEBLA -> "Niebla"
    CondicionTiempo.LLOVIZNA -> "Llovizna"
    CondicionTiempo.LLUVIA -> "Lluvia"
    CondicionTiempo.NIEVE -> "Nieve"
    CondicionTiempo.TORMENTA -> "Tormenta"
}
