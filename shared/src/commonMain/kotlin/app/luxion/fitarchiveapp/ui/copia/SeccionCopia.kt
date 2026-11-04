package app.luxion.fitarchiveapp.ui.copia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.backup.BackupService
import app.luxion.fitarchiveapp.ui.components.TituloSeccion
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Sección "Copia de seguridad" de Perfil (T45): exportar a un archivo `.fitarchive` e importarlo. */
@Composable
fun SeccionCopia(servicio: BackupService, modifier: Modifier = Modifier) {
    val gestor = remember(servicio) { GestorCopia(servicio) }
    val selector = rememberSelectorCopia()
    val scope = rememberCoroutineScope()
    val estado by gestor.estado.collectAsState()
    val ocupado = estado == EstadoCopia.Trabajando

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloSeccion("Copia de seguridad")
        Text(
            "Guarda todas tus prendas, fotos, outfits, calendario y viajes en un archivo para no perderlos " +
                "si cambias de móvil. Todo queda en tu dispositivo; tú decides dónde guardarlo.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = {
                scope.launch {
                    val bytes = gestor.prepararExportacion() ?: return@launch
                    selector.guardar(nombreCopia(), bytes, gestor::alGuardar)
                }
            },
            enabled = !ocupado,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Exportar copia") }
        OutlinedButton(
            onClick = { selector.abrir(gestor::alElegirArchivo) },
            enabled = !ocupado,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Importar copia") }

        when (val e = estado) {
            EstadoCopia.Trabajando -> Text("Trabajando…", style = MaterialTheme.typography.bodyMedium)
            is EstadoCopia.Exito -> Text(e.mensaje, color = MaterialTheme.colorScheme.primary)
            is EstadoCopia.Error -> Text(e.mensaje, color = MaterialTheme.colorScheme.error)
            else -> Unit
        }
    }

    (estado as? EstadoCopia.ConfirmarImportar)?.let { confirmar ->
        AlertDialog(
            onDismissRequest = gestor::cancelarImportacion,
            title = { Text("¿Reemplazar todo?") },
            text = {
                Text(
                    "Importar esta copia borrará tus prendas, outfits, calendario y viajes actuales y los " +
                        "sustituirá por los de la copia. No se puede deshacer.",
                )
            },
            confirmButton = {
                TextButton(onClick = { scope.launch { gestor.importar(confirmar.bytes) } }) { Text("Reemplazar todo") }
            },
            dismissButton = { TextButton(onClick = gestor::cancelarImportacion) { Text("Cancelar") } },
        )
    }
}

internal fun nombreCopia(): String =
    "fitarchive-${Clock.System.todayIn(TimeZone.currentSystemDefault())}.fitarchive"
