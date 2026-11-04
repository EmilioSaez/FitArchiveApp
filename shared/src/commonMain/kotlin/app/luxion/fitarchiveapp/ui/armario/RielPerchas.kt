package app.luxion.fitarchiveapp.ui.armario

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.luxion.fitarchiveapp.data.GarmentFilter
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.resources.Res
import app.luxion.fitarchiveapp.resources.percha
import app.luxion.fitarchiveapp.ui.anadir.etiqueta
import app.luxion.fitarchiveapp.ui.theme.rememberReducirMovimiento
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/** Valor siguiente de un ciclo "Todo → a → b → … → Todo"; `null` representa "Todo". */
internal fun <T> siguiente(valores: List<T>, actual: T?): T? =
    if (actual == null) valores.firstOrNull() else valores.getOrNull(valores.indexOf(actual) + 1)

private fun Season.clima() = when (this) {
    Season.WINTER -> "Frío"
    Season.SUMMER -> "Calor"
    Season.MID_SEASON -> "Entretiempo"
}

/**
 * Rail de perchas que hacen de filtros: Clima, Color, Uso y Estado. Tocar una percha pasa al siguiente valor
 * (Todo → … → Todo) y la balancea. Modifican el mismo [GarmentFilter] que la hoja de filtros.
 */
@Composable
internal fun RielPerchas(filtro: GarmentFilter, onCambiar: (GarmentFilter) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)) {
        Box(Modifier.fillMaxWidth().height(5.dp).background(MaterialTheme.colorScheme.secondary))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Percha("Clima", filtro.season?.clima()) { onCambiar(filtro.copy(season = siguiente(Season.entries, filtro.season))) }
            Percha("Color", filtro.color?.etiqueta()) { onCambiar(filtro.copy(color = siguiente(GarmentColor.entries, filtro.color))) }
            Percha("Uso", filtro.usage?.etiqueta()) { onCambiar(filtro.copy(usage = siguiente(Usage.entries, filtro.usage))) }
            Percha("Estado", filtro.dirty?.let { if (it) "Para lavar" else "Limpia" }) {
                onCambiar(filtro.copy(dirty = when (filtro.dirty) { null -> false; false -> true; true -> null }))
            }
        }
    }
}

/** Percha con etiqueta: tono madera si está en "Todo", burdeos si filtra. [valor] null = Todo. */
@Composable
private fun RowScope.Percha(nombre: String, valor: String?, onClick: () -> Unit) {
    val giro = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val reducir = rememberReducirMovimiento()
    val activa = valor != null
    Column(
        Modifier
            .weight(1f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                onClick()
                if (!reducir) scope.launch { giro.animateTo(0f, keyframes { durationMillis = 600; -6f at 180; 4f at 360; 0f at 600 }) }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0f)
                rotationZ = giro.value
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(Res.drawable.percha), contentDescription = null, modifier = Modifier.size(76.dp, 40.dp))
            Text(
                valor ?: "Todo",
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (activa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = if (activa) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            nombre.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
        )
    }
}
