package app.luxion.fitarchiveapp.ui.viaje

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.data.TripRepository
import app.luxion.fitarchiveapp.foto.cargarImagen
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.Trip
import app.luxion.fitarchiveapp.ui.anadir.etiqueta
import app.luxion.fitarchiveapp.viaje.ItemMaleta
import app.luxion.fitarchiveapp.viaje.formatearFechaCorta
import app.luxion.fitarchiveapp.viaje.formatearFechasViaje
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

/** Pestaña Viajes (T37): lista de viajes y, al abrir uno, su maleta con los outfits y prendas elegidos. */
@Composable
fun ViajesScreen(
    viajes: TripRepository,
    outfits: OutfitRepository,
    armario: GarmentRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val vm = remember(viajes, outfits, armario) {
        ViajesViewModel(viajes, outfits, armario, scope) { Clock.System.now().toEpochMilliseconds() }
    }
    val estado by vm.estado.collectAsState()
    var formulario by remember { mutableStateOf<FormularioAbierto?>(null) }

    formulario?.let { abierto ->
        val editado = abierto.editado
        FormularioViaje(
            editado,
            onGuardar = { nombre, destino, inicio, fin -> vm.guardarViaje(editado?.id, nombre, destino, inicio, fin) },
            onCerrar = { formulario = null },
        )
    }
    estado.porEliminar?.let { viaje ->
        AlertDialog(
            onDismissRequest = vm::cancelarEliminar,
            title = { Text("Eliminar viaje") },
            text = { Text("¿Eliminar \"${viaje.name}\" y su maleta? No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = vm::confirmarEliminar) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = vm::cancelarEliminar) { Text("Cancelar") } },
        )
    }
    estado.error?.let { mensaje ->
        AlertDialog(
            onDismissRequest = vm::descartarError,
            text = { Text(mensaje) },
            confirmButton = { TextButton(onClick = vm::descartarError) { Text("Aceptar") } },
        )
    }

    val abierto = estado.abierto
    if (abierto == null) {
        ListaViajes(estado, modifier, onNuevo = { formulario = FormularioAbierto(null) }, onAbrir = vm::abrir)
    } else {
        DetalleViaje(
            estado, abierto, vm, modifier,
            onEditar = { formulario = FormularioAbierto(abierto) },
        )
    }
}

/** Formulario de viaje abierto: [editado] es el viaje que se modifica, o null si es uno nuevo. */
private class FormularioAbierto(val editado: Trip?)

private fun Trip.fechas(): String? = formatearFechasViaje(startDate, endDate)

@Composable
private fun ListaViajes(estado: ViajesUiState, modifier: Modifier, onNuevo: () -> Unit, onAbrir: (Trip) -> Unit) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Viajes",
                Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (!estado.vacio) Button(onClick = onNuevo) { Text("Nuevo viaje") }
        }
        when {
            estado.cargando -> Box(Modifier.weight(1f))
            estado.vacio -> Column(
                Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Text("Aún no tienes viajes", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(
                    "Crea un viaje, elige los outfits que te llevas y ve marcando lo que ya está en la maleta.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onNuevo) { Text("Nuevo viaje") }
            }
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                items(estado.viajes, key = { it.id }) { viaje ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onAbrir(viaje) }
                            .padding(16.dp),
                    ) {
                        Text(viaje.name, style = MaterialTheme.typography.titleMedium)
                        viaje.destination?.takeIf { it != viaje.name }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        viaje.fechas()?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetalleViaje(
    estado: ViajesUiState,
    viaje: Trip,
    vm: ViajesViewModel,
    modifier: Modifier,
    onEditar: () -> Unit,
) {
    var anadiendoPrenda by remember { mutableStateOf(false) }
    if (anadiendoPrenda) {
        ElegirPrendaDialog(estado.prendasDisponibles, onElegir = vm::anadirPrenda, onCerrar = { anadiendoPrenda = false })
    }
    val maleta = estado.maleta
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = vm::cerrar) { Text("‹ Viajes") }
                Box(Modifier.weight(1f))
                TextButton(onClick = onEditar) { Text("Editar") }
                TextButton(onClick = { vm.pedirEliminar(viaje) }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            }
            Text(viaje.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            viaje.destination?.takeIf { it != viaje.name }?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            viaje.fechas()?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Outfits del viaje", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
            if (estado.outfits.isEmpty()) {
                Text(
                    "Aún no tienes outfits guardados. Crea uno en la pestaña Outfits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    estado.outfits.forEach { outfit ->
                        FilterChip(
                            selected = outfit.id in estado.outfitsDelViaje,
                            onClick = { vm.alternarOutfit(outfit) },
                            label = { Text(outfit.name) },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Maleta", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { anadiendoPrenda = true }, enabled = estado.prendasDisponibles.isNotEmpty()) {
                    Text("Añadir prenda")
                }
            }
            if (maleta.items.isEmpty()) {
                Text(
                    "La maleta está vacía: elige outfits o añade prendas sueltas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    if (maleta.completa) "¡Maleta lista! ${maleta.total} prendas guardadas" else "${maleta.guardadas} de ${maleta.total} guardadas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (maleta.sucias.isNotEmpty()) {
                    Text(
                        "Sucias, lávalas antes de salir: " + maleta.sucias.joinToString { it.prenda.type.etiqueta() },
                        Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        items(maleta.items, key = { it.prenda.id }) { item ->
            FilaMaleta(item, onGuardada = { vm.marcarGuardada(item.prenda, it) }, onQuitar = { vm.quitarPrenda(item.prenda) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun FilaMaleta(item: ItemMaleta, onGuardada: (Boolean) -> Unit, onQuitar: () -> Unit) {
    val prenda = item.prenda
    val imagen by produceState<ImageBitmap?>(null, prenda.photoPath) { value = cargarImagen(prenda.photoPath) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = item.guardada, onCheckedChange = onGuardada)
        Box(
            Modifier.size(48.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            imagen?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Column(Modifier.weight(1f)) {
            Text("${prenda.type.etiqueta()} · ${prenda.color.etiqueta()}", style = MaterialTheme.typography.bodyLarge)
            val detalle = when {
                item.outfits.isNotEmpty() -> item.outfits.joinToString()
                else -> "Añadida a mano"
            } + if (prenda.dirty) " · sucia" else ""
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.manual) TextButton(onClick = onQuitar) { Text("Quitar") }
    }
}

@Composable
private fun ElegirPrendaDialog(prendas: List<Garment>, onElegir: (Garment) -> Unit, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Añadir prenda") },
        text = {
            LazyColumn {
                items(prendas, key = { it.id }) { prenda ->
                    Text(
                        "${prenda.type.etiqueta()} · ${prenda.color.etiqueta()}" + (prenda.brand?.let { " · $it" } ?: ""),
                        Modifier.fillMaxWidth().clickable { onElegir(prenda); onCerrar() }.padding(vertical = 12.dp),
                    )
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
    )
}

@Composable
private fun FormularioViaje(
    editado: Trip?,
    onGuardar: (nombre: String, destino: String, inicio: String, fin: String) -> String?,
    onCerrar: () -> Unit,
) {
    var nombre by remember { mutableStateOf(editado?.name.orEmpty()) }
    var destino by remember { mutableStateOf(editado?.destination.orEmpty()) }
    var inicio by remember { mutableStateOf(editado?.startDate.orEmpty()) }
    var fin by remember { mutableStateOf(editado?.endDate.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (editado == null) "Nuevo viaje" else "Editar viaje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nombre, { nombre = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(destino, { destino = it }, Modifier.fillMaxWidth(), label = { Text("Destino") }, singleLine = true)
                CampoFecha("Ida", inicio, onCambio = { inicio = it })
                CampoFecha("Vuelta", fin, onCambio = { fin = it }, mesPorDefecto = inicio)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = onGuardar(nombre, destino, inicio, fin)
                if (error == null) onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

private const val DIA_MS = 86_400_000L

/**
 * Campo de fecha (ISO en [valor], vacío si no hay): al tocarlo abre un DatePicker; "Borrar" la quita.
 * Sin fecha, el selector abre en el mes de [mesPorDefecto] (ISO) si es válido y, si no, en el actual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoFecha(etiqueta: String, valor: String, onCambio: (String) -> Unit, mesPorDefecto: String = "") {
    var abierto by remember { mutableStateOf(false) }
    val fecha = runCatching { LocalDate.parse(valor) }.getOrNull()
    Box {
        OutlinedTextField(
            if (valor.isEmpty()) "" else formatearFechaCorta(valor),
            {},
            Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text(etiqueta) },
            singleLine = true,
            trailingIcon = if (valor.isEmpty()) null else ({
                TextButton(
                    onClick = { onCambio("") },
                    Modifier.semantics { contentDescription = "Borrar ${etiqueta.lowercase()}" },
                ) { Text("Borrar") }
            }),
        )
        // Capa transparente que recoge el toque (un campo de solo lectura no lo propaga); deja libre el botón Borrar.
        Box(
            Modifier
                .matchParentSize()
                .padding(end = if (valor.isEmpty()) 0.dp else 88.dp)
                .semantics { contentDescription = if (valor.isEmpty()) etiqueta else "$etiqueta ${formatearFechaCorta(valor)}" }
                .clickable(role = Role.Button) { abierto = true },
        )
    }
    if (abierto) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha?.let { it.toEpochDays() * DIA_MS },
            initialDisplayedMonthMillis = (fecha ?: runCatching { LocalDate.parse(mesPorDefecto) }.getOrNull())?.let { it.toEpochDays() * DIA_MS },
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onCambio(LocalDate.fromEpochDays((it / DIA_MS).toInt()).toString()) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
        ) {
            DatePicker(
                estado,
                title = { Text("Fecha de ${etiqueta.lowercase()}", Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) },
                headline = {
                    val elegida = estado.selectedDateMillis?.let { LocalDate.fromEpochDays((it / DIA_MS).toInt()).toString() }
                    Text(
                        elegida?.let { formatearFechaCorta(it) } ?: "Elige una fecha",
                        Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
            )
        }
    }
}
