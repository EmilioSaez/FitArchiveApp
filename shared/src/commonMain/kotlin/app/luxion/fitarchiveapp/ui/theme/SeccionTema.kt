package app.luxion.fitarchiveapp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.ui.components.TituloSeccion

/** Sección "Apariencia": modo claro/oscuro y color de acento. Los cambios se aplican en vivo. */
@Composable
fun SeccionTema(
    ajustes: AjustesTema,
    onCambio: (AjustesTema) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloSeccion("Apariencia")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ModoTema.entries.forEachIndexed { i, modo ->
                SegmentedButton(
                    selected = ajustes.modo == modo,
                    onClick = { onCambio(ajustes.copy(modo = modo)) },
                    shape = SegmentedButtonDefaults.itemShape(i, ModoTema.entries.size),
                ) { Text(modo.etiqueta) }
            }
        }
        Text("Color de acento", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Acento.entries.forEach { acento ->
                FilterChip(
                    selected = ajustes.acento == acento,
                    onClick = { onCambio(ajustes.copy(acento = acento)) },
                    label = { Text(acento.etiqueta) },
                    leadingIcon = {
                        Row(
                            Modifier.size(16.dp).clip(CircleShape).background(acento.color)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        ) {}
                    },
                )
            }
        }
    }
}
