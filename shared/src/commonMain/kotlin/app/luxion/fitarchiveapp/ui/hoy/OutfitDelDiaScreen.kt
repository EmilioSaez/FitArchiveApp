package app.luxion.fitarchiveapp.ui.hoy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.data.Repositories
import app.luxion.fitarchiveapp.foto.cargarImagen
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.suggestions.MotorSugerenciasPorReglas
import app.luxion.fitarchiveapp.ui.armario.FotoPrenda
import app.luxion.fitarchiveapp.ui.avatar.AvatarConPrendas
import app.luxion.fitarchiveapp.ui.avatar.PrendaAvatar
import app.luxion.fitarchiveapp.ui.components.Titular
import app.luxion.fitarchiveapp.ui.components.TituloSeccion
import app.luxion.fitarchiveapp.ui.outfit.etiqueta
import app.luxion.fitarchiveapp.weather.Ciudad
import app.luxion.fitarchiveapp.weather.ServicioTiempo
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.math.roundToInt
import kotlin.time.Clock

/**
 * Pestaña Hoy (T43): la sugerencia principal del motor sobre el avatar, con alternativas, motivo y acciones
 * (aceptar para hoy, guardar como outfit, siguiente, regenerar).
 */
@Composable
fun OutfitDelDiaScreen(
    repositories: Repositories,
    tiempo: ServicioTiempo,
    ciudad: () -> Ciudad?,
    onAbrirPerfil: () -> Unit,
    onAnadirPrendas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val vm = remember(repositories, tiempo) {
        OutfitDelDiaViewModel(
            motor = MotorSugerenciasPorReglas(),
            armario = repositories.garments,
            usos = repositories.usos,
            calendario = repositories.calendar,
            outfits = repositories.outfits,
            tiempoServicio = tiempo,
            ciudad = ciudad,
            scope = scope,
            hoy = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString(),
            ahora = { Clock.System.now().toEpochMilliseconds() },
            calculo = Dispatchers.Default, // El motor con armarios grandes no debe bloquear la interfaz.
        )
    }
    val estado by vm.estado.collectAsState()

    val imagenes = remember { mutableStateMapOf<String, ImageBitmap>() }
    val actual = estado.actual
    val rutas = actual?.prendas?.map { it.photoPath }.orEmpty()
    LaunchedEffect(rutas) {
        rutas.filter { it !in imagenes }.forEach { ruta -> cargarImagen(ruta)?.let { imagenes[ruta] = it } }
    }

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Cabecera(estado)
            AvisoTiempo(estado.tiempo, onConfigurar = onAbrirPerfil, onReintentar = vm::regenerar)
            Ocasiones(estado.ocasion, vm::elegirOcasion)

            when {
                estado.cargando -> Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                actual == null -> EstadoVacio(estado, onAnadirPrendas, vm::elegirOcasion)
                else -> {
                    val prendasAvatar = actual.prendas.mapNotNull { p ->
                        imagenes[p.photoPath]?.let { PrendaAvatar.de(p, it) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AvatarConPrendas(prendasAvatar, Modifier.height(340.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "${(actual.puntuacion * 100).roundToInt()}%",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                motivoBreve(actual.desglose, estado.tiempoDia != null, estado.ocasion != null, estado.actualSinEstrenar),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            actual.desglose.criterios(estado.tiempoDia != null, estado.ocasion != null).forEach { c ->
                                Criterio(c)
                            }
                        }
                    }
                    if (estado.hayVariasOpciones) {
                        TituloSeccion("Alternativas")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(estado.sugerencias) { i, s ->
                                Alternativa(s.prendas, seleccionada = i == estado.indice, onClick = { vm.elegir(i) })
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = vm::siguiente, enabled = estado.hayVariasOpciones) { Text("Siguiente") }
                        TextButton(onClick = vm::regenerar) { Text("Regenerar") }
                    }
                }
            }
        }
        // Las acciones principales quedan fijas abajo: no hace falta bajar por la pantalla para aceptar (O15).
        if (actual != null && !estado.cargando) Acciones(estado, vm)
    }
}

@Composable
private fun Cabecera(estado: OutfitDelDiaUiState) {
    val clima = estado.tiempoDia?.let { t ->
        "${t.temperaturaC.roundToInt()}° · máx ${t.maximaC.roundToInt()}° / mín ${t.minimaC.roundToInt()}° · " +
            t.condicion.descripcion()
    }
    Titular("Tu outfit del día", Modifier.fillMaxWidth(), antetitulo = "Hoy", subtitulo = clima)
}

/** Aviso cuando se sugiere sin saber el tiempo: sin ubicación (se invita a configurarla) o con fallo de red (se puede reintentar). */
@Composable
private fun AvisoTiempo(tiempo: EstadoTiempo, onConfigurar: () -> Unit, onReintentar: () -> Unit) {
    val (texto, accion, onAccion) = when (tiempo) {
        EstadoTiempo.SinUbicacion -> Triple(
            "Sin ubicación no sabemos qué tiempo hace. Activa tu ubicación aproximada o elige una ciudad para sugerirte según el clima.",
            "Configurar ubicación",
            onConfigurar,
        )
        EstadoTiempo.SinConexion -> Triple(
            "No pudimos consultar el tiempo. Te sugerimos outfits sin tenerlo en cuenta.",
            "Reintentar",
            onReintentar,
        )
        else -> return
    }
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            TextButton(onClick = onAccion) { Text(accion) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Ocasiones(elegida: Occasion?, onElegir: (Occasion?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Occasion.entries.forEach { o ->
            FilterChip(
                selected = o == elegida,
                onClick = { onElegir(if (o == elegida) null else o) },
                label = { Text(o.etiqueta()) },
            )
        }
    }
}

@Composable
private fun Criterio(c: CriterioPuntuado) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(c.etiqueta, style = MaterialTheme.typography.labelMedium)
        LinearProgressIndicator(
            progress = { c.valor.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}

@Composable
private fun Alternativa(prendas: List<Garment>, seleccionada: Boolean, onClick: () -> Unit) {
    val borde = if (seleccionada) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
    Row(
        Modifier.border(borde, MaterialTheme.shapes.small).clickable(onClick = onClick).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        prendas.forEach { FotoPrenda(it, onClick = onClick, onAlternarFavorita = null, modifier = Modifier.width(48.dp)) }
    }
}

@Composable
private fun Acciones(estado: OutfitDelDiaUiState, vm: OutfitDelDiaViewModel) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.aviso?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
            estado.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            Button(onClick = vm::aceptar, enabled = !estado.ocupado && !estado.aceptadoActual, modifier = Modifier.fillMaxWidth()) {
                Text(if (estado.aceptadoActual) "Asignado a hoy" else "Aceptar para hoy")
            }
            OutlinedButton(onClick = vm::guardar, enabled = !estado.ocupado && !estado.guardadoActual, modifier = Modifier.fillMaxWidth()) {
                Text(if (estado.guardadoActual) "Guardado en tus outfits" else "Guardar como outfit")
            }
        }
    }
}

/** Sin sugerencias: armario vacío, o nada que encaje con el tiempo y la ocasión de hoy. */
@Composable
private fun EstadoVacio(estado: OutfitDelDiaUiState, onAnadirPrendas: () -> Unit, onQuitarOcasion: (Occasion?) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val sinPrendas = estado.armarioSinPrendas
        Text(
            if (sinPrendas) "Tu armario está vacío" else "Hoy no encontramos un outfit",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            if (sinPrendas) {
                "Añade prendas para que te sugiramos qué ponerte."
            } else {
                "Necesitas una prenda de arriba y otra de abajo (o un vestido) limpias que encajen con el tiempo" +
                    (if (estado.ocasion != null) " y la ocasión" else "") + " de hoy."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (sinPrendas) {
            Button(onClick = onAnadirPrendas) { Text("Añadir prenda") }
        } else if (estado.ocasion != null) {
            OutlinedButton(onClick = { onQuitarOcasion(null) }) { Text("Probar sin ocasión") }
        }
    }
}
