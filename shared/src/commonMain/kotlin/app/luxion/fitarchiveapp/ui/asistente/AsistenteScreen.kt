package app.luxion.fitarchiveapp.ui.asistente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.asistente.AVISO_ASISTENTE
import app.luxion.fitarchiveapp.asistente.AsistenteEstilo
import app.luxion.fitarchiveapp.asistente.Intercambio
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.ia.ErrorIa
import app.luxion.fitarchiveapp.ia.ServicioIa
import app.luxion.fitarchiveapp.ui.theme.Iconos
import kotlinx.coroutines.launch

private class Burbuja(val esUsuaria: Boolean, val texto: String)

private val SUGERENCIAS = listOf(
    "¿Qué me pongo para una cena?",
    "¿Qué le falta a mi armario?",
    "¿Qué combino con mi prenda favorita?",
    "Ayúdame con la maleta de un viaje",
)

/**
 * Asistente de estilo: conversación sobre el armario. La conversación vive solo en memoria mientras la pantalla
 * está abierta. Sin IA, sin clave o sin aviso aceptado muestra un estado vacío con enlace a Perfil.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AsistenteScreen(
    servicio: ServicioIa,
    prendas: GarmentRepository,
    onVolver: () -> Unit,
    onAbrirPerfil: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by servicio.estado.collectAsState()
    val bloqueo = remember(estado) { servicio.bloqueo(AVISO_ASISTENTE) }
    val armario by prendas.observeAll().collectAsState(emptyList())
    val asistente = remember(servicio) { AsistenteEstilo(servicio) }
    val scope = rememberCoroutineScope()
    val burbujas = remember { mutableStateListOf<Burbuja>() }
    val historial = remember { mutableStateListOf<Intercambio>() }
    var borrador by remember { mutableStateOf("") }
    var pensando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val lista = rememberLazyListState()

    LaunchedEffect(burbujas.size, pensando) {
        if (burbujas.isNotEmpty()) lista.animateScrollToItem(burbujas.size)
    }

    fun enviar() {
        val texto = borrador.trim()
        if (texto.isEmpty() || pensando || bloqueo != null) return
        borrador = ""
        error = null
        pensando = true
        burbujas += Burbuja(true, texto)
        scope.launch {
            asistente.responder(armario, historial.toList(), texto).fold(
                onSuccess = {
                    burbujas += Burbuja(false, it)
                    historial += Intercambio(texto, it)
                },
                onFailure = {
                    // La pregunta no se envió bien: se devuelve al cuadro para reintentarla.
                    burbujas.removeAt(burbujas.lastIndex)
                    borrador = texto
                    error = (it as? ErrorIa)?.mensaje ?: "No se pudo obtener respuesta."
                },
            )
            pensando = false
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Iconos.Volver, contentDescription = "Volver") }
            Column(Modifier.weight(1f)) {
                Text("ASISTENTE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tu estilista", style = MaterialTheme.typography.headlineSmall)
            }
        }

        when (bloqueo) {
            is ErrorIa.AvisoPendiente -> AlertDialog(
                onDismissRequest = onVolver,
                title = { Text(AVISO_ASISTENTE.titulo) },
                text = { Text(AVISO_ASISTENTE.texto) },
                confirmButton = { TextButton(onClick = { servicio.aceptarAviso(AVISO_ASISTENTE) }) { Text("Aceptar") } },
                dismissButton = { TextButton(onClick = onVolver) { Text("Cancelar") } },
            )
            is ErrorIa.SinClave, is ErrorIa.Desactivada, is ErrorIa.TopeAlcanzado -> EstadoVacio(bloqueo.mensaje, onAbrirPerfil)
            else -> Unit
        }
        if (bloqueo is ErrorIa.SinClave || bloqueo is ErrorIa.Desactivada || bloqueo is ErrorIa.TopeAlcanzado) return@Column

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = lista,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    if (armario.isEmpty()) "Tu armario está vacío: añade prendas para que pueda aconsejarte con ellas."
                    else "Pregúntame por tu armario. Mis consejos son orientativos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(burbujas) { BurbujaMensaje(it) }
            if (pensando) item { Text("Pensando…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            if (burbujas.isEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SUGERENCIAS.forEach { s -> AssistChip(onClick = { borrador = s }, label = { Text(s) }) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = borrador,
                    onValueChange = { borrador = it.take(AsistenteEstilo.MAX_CARACTERES_MENSAJE) },
                    label = { Text("Escribe tu pregunta") },
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = ::enviar, enabled = borrador.isNotBlank() && !pensando) { Text("Enviar") }
            }
        }
    }
}

@Composable
private fun EstadoVacio(mensaje: String, onAbrirPerfil: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("El asistente de estilo necesita la IA", style = MaterialTheme.typography.titleMedium)
        Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onAbrirPerfil) { Text("Ir a Perfil") }
    }
}

@Composable
private fun BurbujaMensaje(b: Burbuja) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (b.esUsuaria) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            b.texto,
            style = MaterialTheme.typography.bodyMedium,
            color = if (b.esUsuaria) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .background(
                    if (b.esUsuaria) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
