package app.luxion.fitarchiveapp.ui.outfit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Pestaña Outfits: por ahora solo el punto de entrada al editor; T19 añade la lista de guardados. */
@Composable
fun OutfitsScreen(onNuevoOutfit: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Outfits", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Button(onClick = onNuevoOutfit) { Text("Nuevo outfit") }
    }
}
