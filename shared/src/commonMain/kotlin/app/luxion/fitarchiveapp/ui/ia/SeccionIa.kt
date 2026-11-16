package app.luxion.fitarchiveapp.ui.ia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.ia.AvisosIa
import app.luxion.fitarchiveapp.ia.ErrorIa
import app.luxion.fitarchiveapp.ia.InfoModelo
import app.luxion.fitarchiveapp.ia.RANGO_TOPE_DIARIO
import app.luxion.fitarchiveapp.ia.ServicioIa
import app.luxion.fitarchiveapp.ui.components.TituloSeccion
import kotlinx.coroutines.launch

private const val URL_CLAVES = "https://console.anthropic.com/settings/keys"

/** Sección "Inteligencia artificial" de Perfil: clave de API propia, activación, aviso de privacidad y tope diario. */
@Composable
fun SeccionIa(servicio: ServicioIa, modifier: Modifier = Modifier) {
    val estado by servicio.estado.collectAsState()
    val ajustes = estado.ajustes
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current

    var clave by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<Mensaje?>(null) }
    var comprobando by remember { mutableStateOf(false) }
    var mostrarAviso by remember { mutableStateOf(false) }
    var tope by remember(ajustes.topeDiario) { mutableStateOf(ajustes.topeDiario.toString()) }

    fun comprobar(prefijo: String = "") = scope.launch {
        comprobando = true
        mensaje = servicio.probarConexion().fold(
            onSuccess = { Mensaje(prefijo + describir(it), error = false) },
            onFailure = { Mensaje(prefijo + (it as? ErrorIa)?.mensaje.orEmpty().ifEmpty { "No se pudo comprobar la clave." }, error = true) },
        )
        comprobando = false
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloSeccion("Inteligencia artificial")
        Text(
            "FitArchive puede usar IA para catalogar tus prendas, sugerirte outfits y aconsejarte. Funciona con tu " +
                "propia clave de API de Anthropic: cada persona usa la suya y se guarda solo en este dispositivo, " +
                "cifrada y fuera de las copias de seguridad. Sin clave, el resto de la app funciona igual.",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (estado.tieneClave) {
            Text("Clave guardada ✓", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        } else {
            OutlinedTextField(
                value = clave,
                onValueChange = { clave = it; mensaje = null },
                label = { Text("Pega aquí tu clave (sk-ant-…)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    servicio.guardarClave(clave).fold(
                        onSuccess = {
                            clave = ""
                            comprobar(prefijo = "Clave guardada. ")
                        },
                        onFailure = { mensaje = Mensaje(it.message ?: "No se pudo guardar la clave.", error = true) },
                    )
                },
                enabled = clave.isNotBlank() && !comprobando,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar clave") }
            TextButton(onClick = { uri.openUri(URL_CLAVES) }) { Text("¿Cómo consigo una clave?") }
        }

        if (estado.tieneClave) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Usar IA", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = ajustes.activada,
                    onCheckedChange = { quiere ->
                        when {
                            !quiere -> servicio.activar(false)
                            AvisosIa.GENERAL.id in ajustes.avisosAceptados -> servicio.activar(true)
                            else -> mostrarAviso = true
                        }
                    },
                )
            }
            OutlinedTextField(
                value = tope,
                onValueChange = { texto ->
                    tope = texto.filter(Char::isDigit).take(3)
                    tope.toIntOrNull()?.takeIf { it in RANGO_TOPE_DIARIO }?.let(servicio::cambiarTopeDiario)
                },
                label = { Text("Tope de consultas al día (${RANGO_TOPE_DIARIO.first}–${RANGO_TOPE_DIARIO.last})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Hoy: ${estado.usosHoy} de ${ajustes.topeDiario} consultas · modelo ${ajustes.modelo}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { comprobar() }, enabled = !comprobando, modifier = Modifier.fillMaxWidth()) {
                Text(if (comprobando) "Comprobando…" else "Probar conexión")
            }
            OutlinedButton(
                onClick = {
                    servicio.borrarClave()
                    mensaje = null
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Borrar clave") }
        }

        mensaje?.let {
            Text(it.texto, color = if (it.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }

    if (mostrarAviso) {
        AlertDialog(
            onDismissRequest = { mostrarAviso = false },
            title = { Text(AvisosIa.GENERAL.titulo) },
            text = { Text(AvisosIa.GENERAL.texto) },
            confirmButton = {
                TextButton(onClick = {
                    servicio.aceptarAviso(AvisosIa.GENERAL)
                    servicio.activar(true)
                    mostrarAviso = false
                }) { Text("Aceptar y activar") }
            },
            dismissButton = { TextButton(onClick = { mostrarAviso = false }) { Text("Cancelar") } },
        )
    }
}

private class Mensaje(val texto: String, val error: Boolean)

private fun describir(m: InfoModelo): String {
    val base = "Conexión correcta con ${m.nombre ?: m.id}."
    val sinImagenes = if (m.admiteImagenes == false) " Ojo: este modelo no admite imágenes." else ""
    return base + sinImagenes
}
