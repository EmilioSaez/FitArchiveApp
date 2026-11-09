package app.luxion.fitarchiveapp.ui.anadir

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.avatar.Subtipo
import app.luxion.fitarchiveapp.avatar.subtiposValidos
import app.luxion.fitarchiveapp.model.GarmentType

/**
 * Selector de subtipo de dos toques: un botón con el subtipo actual (y si es el sugerido) y, al tocarlo,
 * los subtipos válidos para el [tipo]; tocar uno lo elige y vuelve a plegarlo. Aceptar la sugerencia no
 * cuesta ningún toque.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SelectorSubtipo(
    tipo: GarmentType,
    elegido: Subtipo,
    sugerido: Subtipo,
    alElegir: (Subtipo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var abierto by remember(tipo) { mutableStateOf(false) }
    val opciones = subtiposValidos(tipo)
    OutlinedButton(onClick = { abierto = !abierto }, enabled = opciones.size > 1, modifier = modifier.fillMaxWidth()) {
        Text(elegido.etiqueta() + if (elegido == sugerido) " · sugerido" else "")
    }
    if (abierto) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { s ->
                FilterChip(
                    selected = s == elegido,
                    onClick = { alElegir(s); abierto = false },
                    label = { Text(s.etiqueta() + if (s == sugerido) " · sugerido" else "") },
                )
            }
        }
    }
}
