package app.luxion.fitarchiveapp.ui.calendario

import app.luxion.fitarchiveapp.ui.components.Titular
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import app.luxion.fitarchiveapp.data.TripRepository
import app.luxion.fitarchiveapp.model.Trip
import app.luxion.fitarchiveapp.resources.Res
import app.luxion.fitarchiveapp.resources.maleta_cuerpo
import app.luxion.fitarchiveapp.resources.maleta_tapa
import app.luxion.fitarchiveapp.ui.theme.Iconos
import app.luxion.fitarchiveapp.viaje.formatearFechasViaje
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.data.CalendarRepository
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.model.Outfit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Pestaña Calendario (T32): cuadrícula mensual; al tocar un día se asigna un outfit y una nota. */
@Composable
fun CalendarioScreen(
    calendario: CalendarRepository,
    outfits: OutfitRepository,
    viajesRepo: TripRepository,
    modifier: Modifier = Modifier,
    /** Abre la maleta de un viaje (tarjeta "Próximo viaje"). */
    onAbrirViaje: (Trip) -> Unit = {},
    /** Abre la lista de viajes (crear y editar). */
    onAbrirViajes: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val vm = remember(calendario, outfits) {
        CalendarioViewModel(calendario, outfits, scope, Clock.System.todayIn(TimeZone.currentSystemDefault()).toString())
    }
    val estado by vm.estado.collectAsState()

    estado.seleccionado?.let { fecha ->
        DiaDialog(
            fecha, estado,
            onAsignar = vm::asignarOutfit,
            onQuitar = vm::quitarOutfit,
            onGuardarNota = vm::guardarNota,
            onCerrar = vm::cerrarDia,
        )
    }
    estado.error?.let { mensaje ->
        AlertDialog(
            onDismissRequest = vm::descartarError,
            text = { Text(mensaje) },
            confirmButton = { TextButton(onClick = vm::descartarError) { Text("Aceptar") } },
        )
    }

    val viajes by viajesRepo.observeAll().collectAsState(emptyList())
    val proximo = viajes
        .filter { (it.endDate ?: it.startDate)?.let { fin -> fin >= estado.hoy } == true }
        .minByOrNull { it.startDate ?: "9999-12-31" }
    val colores = MaterialTheme.colorScheme

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Titular("Calendario", Modifier.weight(1f))
                TextButton(onClick = onAbrirViajes) { Text("Viajes") }
            }
        }
        item {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = vm::mesAnterior) { Icon(Iconos.Volver, contentDescription = "Mes anterior") }
                    Text(
                        estado.mes.titulo,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    // Volver a hoy: solo cuando no se está en el mes actual.
                    if (estado.mes != MesCalendario.de(estado.hoy)) {
                        IconButton(onClick = vm::irAHoy) { Icon(Iconos.Hoy, contentDescription = "Ir a hoy") }
                    }
                    IconButton(onClick = vm::mesSiguiente) {
                        Icon(Iconos.Volver, contentDescription = "Mes siguiente", modifier = Modifier.rotate(180f))
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    MesCalendario.DIAS_SEMANA.forEach {
                        Text(
                            it,
                            Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = colores.onSurfaceVariant,
                        )
                    }
                }
                estado.celdas.chunked(7).forEach { semana ->
                    Row(Modifier.fillMaxWidth()) {
                        semana.forEach { fecha ->
                            Box(Modifier.weight(1f).height(48.dp)) {
                                if (fecha != null) CeldaDia(fecha, estado, viajes, onClick = { vm.seleccionar(fecha) })
                            }
                        }
                    }
                }
            }
        }
        item { TarjetaHoy(estado.outfitDe(estado.hoy)?.name, onClick = { vm.seleccionar(estado.hoy) }) }
        proximo?.let { viaje -> item { TarjetaProximoViaje(viaje, viajesRepo, estado.hoy) { onAbrirViaje(viaje) } } }
    }
}

/**
 * Celda de 48 dp: un punto primary de 5 dp marca un día con outfit; hoy es un círculo onSurface con el número blanco;
 * los días de un viaje llevan una banda primaryContainer con los extremos redondeados de 14.
 */
@Composable
private fun CeldaDia(fecha: String, estado: CalendarioUiState, viajes: List<Trip>, onClick: () -> Unit) {
    val dia = estado.dias[fecha]
    val tieneOutfit = estado.outfitDe(fecha) != null
    val esHoy = fecha == estado.hoy
    val colores = MaterialTheme.colorScheme
    val viaje = viajes.firstOrNull { v -> v.startDate != null && fecha >= v.startDate!! && fecha <= (v.endDate ?: v.startDate!!) }
    val esInicio = viaje != null && fecha == viaje.startDate
    val esFin = viaje != null && fecha == (viaje.endDate ?: viaje.startDate)
    Box(Modifier.fillMaxSize().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (viaje != null) {
            Box(
                Modifier.fillMaxWidth().height(28.dp).background(
                    colores.primaryContainer,
                    RoundedCornerShape(
                        topStart = if (esInicio) 14.dp else 0.dp, bottomStart = if (esInicio) 14.dp else 0.dp,
                        topEnd = if (esFin) 14.dp else 0.dp, bottomEnd = if (esFin) 14.dp else 0.dp,
                    ),
                ),
            )
        }
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(if (esHoy) colores.onSurface else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                LocalDate.parse(fecha).day.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (esHoy) Color.White else colores.onSurface,
            )
        }
        if (tieneOutfit || !dia?.note.isNullOrBlank()) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(5.dp).clip(CircleShape)
                    .background(if (tieneOutfit) colores.primary else colores.outline),
            )
        }
    }
}

/** Tarjeta "Hoy" con el outfit del día. */
@Composable
private fun TarjetaHoy(outfit: String?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("HOY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(outfit ?: "Sin outfit para hoy", style = MaterialTheme.typography.titleLarge)
        Text(
            if (outfit == null) "Toca para elegir uno" else "Toca para cambiarlo o añadir una nota",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Tarjeta "Próximo viaje" en secondaryContainer: destino, fechas, progreso de la maleta y la maleta cerrada. */
@Composable
private fun TarjetaProximoViaje(viaje: Trip, viajes: TripRepository, hoy: String, onClick: () -> Unit) {
    val packing by viajes.observePacking(viaje.id).collectAsState(emptyList())
    val guardadas = packing.count { it.packed }
    val total = packing.size
    val colores = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colores.secondaryContainer)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("PRÓXIMO VIAJE", style = MaterialTheme.typography.labelSmall, color = colores.onSecondaryContainer)
            Text(viaje.destination ?: viaje.name, style = MaterialTheme.typography.titleLarge, color = colores.onSecondaryContainer)
            formatearFechasViaje(viaje.startDate, viaje.endDate)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = colores.onSecondaryContainer)
            }
            Text(
                if (total == 0) "Maleta sin preparar" else "$guardadas de $total prendas en la maleta",
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { guardadas.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = colores.tertiary,
                    trackColor = colores.surface,
                )
            }
        }
        // Maleta cerrada: cuerpo + tapa a escala reducida.
        Box(Modifier.size(62.dp, 87.dp)) {
            Image(painterResource(Res.drawable.maleta_cuerpo), null, Modifier.fillMaxSize())
            Image(painterResource(Res.drawable.maleta_tapa), null, Modifier.offset(y = 7.dp).size(62.dp, 75.dp))
        }
    }
}

@Composable
private fun DiaDialog(
    fecha: String,
    estado: CalendarioUiState,
    onAsignar: (Outfit) -> Unit,
    onQuitar: () -> Unit,
    onGuardarNota: (String) -> Unit,
    onCerrar: () -> Unit,
) {
    val dia = estado.dias[fecha]
    val actual = estado.outfitDe(fecha)
    var nota by remember(fecha) { mutableStateOf(dia?.note.orEmpty()) }
    val d = LocalDate.parse(fecha)
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("${d.day} de ${MesCalendario.NOMBRES_MESES[d.month.ordinal].lowercase()} de ${d.year}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    actual?.let { "Outfit: ${it.name}" } ?: "Sin outfit asignado",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (estado.outfits.isEmpty()) {
                    Text("Aún no tienes outfits guardados. Crea uno en la pestaña Outfits.")
                } else {
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(estado.outfits, key = { it.id }) { outfit ->
                            val elegido = outfit.id == actual?.id
                            Text(
                                (if (elegido) "✓ " else "") + outfit.name,
                                Modifier.fillMaxWidth().clickable { onAsignar(outfit) }.padding(vertical = 10.dp),
                                color = if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            HorizontalDivider()
                        }
                    }
                }
                if (actual != null) TextButton(onClick = onQuitar) { Text("Quitar outfit") }
                OutlinedTextField(nota, { nota = it }, Modifier.fillMaxWidth(), label = { Text("Nota") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardarNota(nota); onCerrar() }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
    )
}
