package app.luxion.fitarchiveapp.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.data.UsoRepository
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.ui.calendario.MesCalendario
import kotlinx.datetime.LocalDate

/** Contador de usos (total y desde el último lavado) e historial de fechas de la prenda (T33). */
@Composable
internal fun HistorialUsos(prenda: Garment, usos: UsoRepository) {
    val fechas by remember(usos, prenda.id) { usos.observeFechas(prenda.id) }.collectAsState(initial = emptyList())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilaUso("Veces usada", prenda.totalWears.toString())
        FilaUso("Desde el último lavado", prenda.wearsSinceWash.toString())
        Text("Historial", style = MaterialTheme.typography.titleMedium)
        if (fechas.isEmpty()) {
            Text(
                "Aún sin usos. Asigna un outfit con esta prenda a un día del calendario; el uso se cuenta cuando ese día ya ha pasado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            fechas.forEach { Text(formatearFecha(it), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

/** "2026-11-02" → "2 de noviembre de 2026". */
internal fun formatearFecha(iso: String): String = runCatching {
    LocalDate.parse(iso).let { "${it.day} de ${MesCalendario.NOMBRES_MESES[it.month.ordinal].lowercase()} de ${it.year}" }
}.getOrDefault(iso)

@Composable
private fun FilaUso(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
