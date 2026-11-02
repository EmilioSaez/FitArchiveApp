package app.luxion.fitarchiveapp.ui.calendario

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
    modifier: Modifier = Modifier,
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

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Calendario",
            Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::mesAnterior) { Text("‹") }
            Text(
                estado.mes.titulo,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = vm::mesSiguiente) { Text("›") }
        }
        TextButton(onClick = vm::irAHoy, Modifier.align(Alignment.CenterHorizontally)) { Text("Hoy") }
        Row(Modifier.fillMaxWidth()) {
            MesCalendario.DIAS_SEMANA.forEach {
                Text(
                    it,
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        estado.celdas.chunked(7).forEach { semana ->
            Row(Modifier.fillMaxWidth()) {
                semana.forEach { fecha ->
                    Box(Modifier.weight(1f).aspectRatio(0.8f).padding(2.dp)) {
                        if (fecha != null) CeldaDia(fecha, estado, onClick = { vm.seleccionar(fecha) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CeldaDia(fecha: String, estado: CalendarioUiState, onClick: () -> Unit) {
    val dia = estado.dias[fecha]
    val outfit = estado.outfitDe(fecha)
    val esHoy = fecha == estado.hoy
    val colores = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(if (outfit != null) colores.primaryContainer else colores.surface)
            .border(if (esHoy) 2.dp else 1.dp, if (esHoy) colores.primary else colores.outlineVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(LocalDate.parse(fecha).day.toString(), style = MaterialTheme.typography.labelLarge)
        if (outfit != null) {
            Text(outfit.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else if (!dia?.note.isNullOrBlank()) {
            Text("•", color = colores.primary)
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
