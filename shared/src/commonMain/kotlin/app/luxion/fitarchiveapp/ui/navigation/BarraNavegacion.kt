package app.luxion.fitarchiveapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.luxion.fitarchiveapp.ui.theme.Iconos

private fun Destino.icono(): ImageVector = when (this) {
    Destino.Hoy -> Iconos.Hoy
    Destino.Armario -> Iconos.Armario
    Destino.Outfits -> Iconos.Looks
    Destino.Calendario -> Iconos.Calendario
    Destino.Perfil -> Iconos.Perfil
}

/** Barra inferior de 80 dp: fondo surface, línea fina arriba, activo en primary con indicador de 56×32. */
@Composable
fun BarraNavegacion(actual: Destino, onSeleccionar: (Destino) -> Unit) {
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().height(80.dp)) {
            Destino.entries.forEach { item ->
                val activo = item == actual
                val color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    Modifier.weight(1f).height(80.dp).clickable(role = Role.Tab) { onSeleccionar(item) },
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(56.dp, 32.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (activo) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) { Icon(item.icono(), contentDescription = null, tint = color, modifier = Modifier.size(24.dp)) }
                    Text(
                        item.titulo,
                        color = color,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
