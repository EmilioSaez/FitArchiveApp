package app.luxion.fitarchiveapp.ui.avatar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.avatar.ia.AVISO_IA_AVATAR
import app.luxion.fitarchiveapp.avatar.ia.PropuestaAvatar
import app.luxion.fitarchiveapp.avatar.ia.proponerAjusteAvatar
import app.luxion.fitarchiveapp.avatar.subtipoEfectivo
import app.luxion.fitarchiveapp.ia.ErrorIa
import app.luxion.fitarchiveapp.ia.ServicioIa
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.ui.anadir.etiqueta
import kotlinx.coroutines.launch

/**
 * "Ajustar con IA" del editor de outfit: propone subtipo y colocación de las [prendas] puestas y un comentario del
 * look. Nada cambia hasta que la usuaria pulsa "Aplicar" ([onAplicar]); "Descartar" deja el outfit como estaba.
 * Sin IA, clave o con el tope alcanzado el botón sale deshabilitado y dice por qué.
 */
@Composable
fun BotonAjusteIa(
    servicio: ServicioIa,
    prendas: List<Garment>,
    onAplicar: (PropuestaAvatar) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by servicio.estado.collectAsState()
    val scope = rememberCoroutineScope()
    var trabajando by remember { mutableStateOf(false) }
    var mostrarAviso by remember { mutableStateOf(false) }
    var propuesta by remember { mutableStateOf<PropuestaAvatar?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val bloqueo = remember(estado) { servicio.bloqueo(AVISO_IA_AVATAR) }
    // Falta solo el aviso: el botón sigue activo y al pulsarlo se pide. Cualquier otro bloqueo lo deshabilita.
    val deshabilitado = bloqueo != null && bloqueo !is ErrorIa.AvisoPendiente

    fun consultar() {
        error = null
        trabajando = true
        val puestas = prendas
        scope.launch {
            servicio.proponerAjusteAvatar(puestas).fold(
                onSuccess = { propuesta = it },
                onFailure = { error = (it as? ErrorIa)?.mensaje ?: "No se pudo ajustar el look." },
            )
            trabajando = false
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            onClick = { if (bloqueo is ErrorIa.AvisoPendiente) mostrarAviso = true else consultar() },
            enabled = prendas.isNotEmpty() && !deshabilitado && !trabajando,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(if (trabajando) "Ajustando…" else "Ajustar con IA") }
        val aviso = error ?: if (deshabilitado && prendas.isNotEmpty()) bloqueo?.mensaje else null
        aviso?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (mostrarAviso) {
        AlertDialog(
            onDismissRequest = { mostrarAviso = false },
            title = { Text(AVISO_IA_AVATAR.titulo) },
            text = { Text(AVISO_IA_AVATAR.texto) },
            confirmButton = {
                TextButton(onClick = {
                    servicio.aceptarAviso(AVISO_IA_AVATAR)
                    mostrarAviso = false
                    consultar()
                }) { Text("Aceptar y ajustar") }
            },
            dismissButton = { TextButton(onClick = { mostrarAviso = false }) { Text("Cancelar") } },
        )
    }

    propuesta?.let { p ->
        AlertDialog(
            onDismissRequest = { propuesta = null },
            title = { Text("Propuesta de la IA") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    p.comentario?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    p.prendas.forEach { pp ->
                        val g = prendas.firstOrNull { it.id == pp.prendaId } ?: return@forEach
                        val cambia = if (pp.subtipo != g.subtipoEfectivo) "${g.subtipoEfectivo.nombre()} → ${pp.subtipo.nombre()}" else pp.subtipo.nombre()
                        Text("${g.type.etiqueta()}: $cambia", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "Al aplicar, la colocación de estas prendas cambia; puedes volver a moverlas a mano.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onAplicar(p); propuesta = null },
                    enabled = p.prendas.isNotEmpty(),
                ) { Text("Aplicar") }
            },
            dismissButton = { TextButton(onClick = { propuesta = null }) { Text("Descartar") } },
        )
    }
}

/** `TOP_MANGA_LARGA` → "manga larga". */
private fun app.luxion.fitarchiveapp.avatar.Subtipo.nombre() = name.substringAfter('_').replace('_', ' ').lowercase()
