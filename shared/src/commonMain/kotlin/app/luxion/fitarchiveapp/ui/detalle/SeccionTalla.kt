package app.luxion.fitarchiveapp.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.data.MeasurementsRepository
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.tallas.AjusteTalla
import app.luxion.fitarchiveapp.tallas.CargadorTallas
import app.luxion.fitarchiveapp.tallas.EtiquetaAjuste
import app.luxion.fitarchiveapp.tallas.MedidasUsuaria
import app.luxion.fitarchiveapp.tallas.TablaTallas

/** Resultado de talla en el detalle de la prenda (T30). */
@Composable
internal fun SeccionTalla(
    prenda: Garment,
    measurements: MeasurementsRepository,
    onAbrirPerfil: () -> Unit,
) {
    val tablas by produceState<Map<String, TablaTallas>?>(null) {
        value = runCatching { CargadorTallas.cargarTodas() }.getOrNull()
    }
    val medidas by remember(measurements) { measurements.observe() }.collectAsState(initial = null)
    val cargadas = tablas ?: return
    val ui = remember(prenda.brand, prenda.type, prenda.size, medidas, cargadas) {
        val r = AjusteTalla.calcular(MedidasUsuaria.de(medidas), prenda.brand, prenda.type, prenda.size, cargadas)
        PresentacionTalla.presentar(r, prenda.brand, cargadas)
    }

    when (ui) {
        TallaUi.Oculta -> Unit
        TallaUi.SinMedidas -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Titulo()
            Text(
                "Añade tus medidas para saber cómo te queda esta prenda.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onAbrirPerfil) { Text("Ir a Perfil") }
        }
        is TallaUi.Aviso -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Titulo()
            Text(ui.mensaje, style = MaterialTheme.typography.bodyMedium)
            ui.textoTallaIdeal?.let { Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) }
            ui.nota?.let { Nota(it) }
        }
        is TallaUi.Resultado -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Titulo()
            Text(ui.titulo, style = MaterialTheme.typography.headlineSmall, color = colorEtiqueta(ui.etiqueta))
            ui.zonas.forEach { z ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(z.zona, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(z.texto, style = MaterialTheme.typography.bodyMedium, color = colorEtiqueta(z.etiqueta))
                }
            }
            ui.textoTallaIdeal?.let { Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) }
            ui.nota?.let { Nota(it) }
        }
    }
}

@Composable
private fun Titulo() {
    Text("Talla", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Nota(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun colorEtiqueta(e: EtiquetaAjuste): Color = when (e) {
    EtiquetaAjuste.AJUSTADA -> MaterialTheme.colorScheme.error
    EtiquetaAjuste.BIEN -> MaterialTheme.colorScheme.primary
    EtiquetaAjuste.HOLGADA -> MaterialTheme.colorScheme.tertiary
}
