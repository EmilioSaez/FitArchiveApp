package app.luxion.fitarchiveapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Titular de pantalla al estilo revista (T46): [titulo] en serif, un filete burdeos corto y, opcionalmente,
 * un [antetitulo] en versalitas encima y un [subtitulo] discreto debajo. Todas las pestañas lo usan para que
 * tengan la misma jerarquía.
 */
@Composable
fun Titular(
    titulo: String,
    modifier: Modifier = Modifier,
    antetitulo: String? = null,
    subtitulo: String? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        antetitulo?.let {
            Text(
                it.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(titulo, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(2.dp))
        Spacer(Modifier.width(36.dp).height(2.dp).background(MaterialTheme.colorScheme.primary))
        subtitulo?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Título de sección dentro de una pantalla: serif mediano, en tinta. */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(texto, modifier, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
}
