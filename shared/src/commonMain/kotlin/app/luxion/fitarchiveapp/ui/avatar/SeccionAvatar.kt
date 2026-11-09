package app.luxion.fitarchiveapp.ui.avatar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.ui.components.TituloSeccion

/** Vista previa del maniquí en Perfil: se ajusta a las medidas guardadas. */
@Composable
fun SeccionAvatar(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloSeccion("Tu maniquí")
        Text(
            "Se ajusta a tus medidas y las prendas se dibujan a su tamaño real.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AvatarConPrendas(emptyList(), Modifier.height(320.dp))
        }
    }
}
