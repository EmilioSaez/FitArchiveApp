package app.luxion.fitarchiveapp.ui.asistente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.ui.components.TituloSeccion

/** Punto de entrada al asistente de estilo desde Perfil, junto a los ajustes de IA. */
@Composable
fun EntradaAsistente(onAbrir: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSeccion("Asistente de estilo")
        Text(
            "Pregúntale a la IA qué ponerte, qué le falta a tu armario o cómo preparar una maleta. Requiere la IA activada.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onAbrir, modifier = Modifier.fillMaxWidth()) { Text("Abrir el asistente") }
    }
}
