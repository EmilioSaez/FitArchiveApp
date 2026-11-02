package app.luxion.fitarchiveapp.ui.anadir

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Botón «Añadir» para colocar en la pantalla del armario (T10); al pulsarlo se abre [AnadirPrendaFlow]. */
@Composable
fun BotonAnadirPrenda(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) { Text("+ Añadir") }
}
