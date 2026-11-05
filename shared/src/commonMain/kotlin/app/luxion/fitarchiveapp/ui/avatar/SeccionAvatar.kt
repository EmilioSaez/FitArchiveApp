package app.luxion.fitarchiveapp.ui.avatar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.avatar.AspectoAvatar
import app.luxion.fitarchiveapp.avatar.EstiloPelo
import app.luxion.fitarchiveapp.data.AvatarRepository
import app.luxion.fitarchiveapp.foto.rememberAlmacenFotos
import app.luxion.fitarchiveapp.foto.rememberSelectorFoto
import app.luxion.fitarchiveapp.ui.components.TituloSeccion
import kotlinx.coroutines.launch

/**
 * Personalización del avatar en Perfil: tono de piel, pelo (color y peinado) y, opcionalmente, una foto tuya
 * como cara. La vista previa usa tus medidas guardadas.
 */
@Composable
fun SeccionAvatar(repositorio: AvatarRepository, modifier: Modifier = Modifier) {
    val aspecto by remember(repositorio) { repositorio.observe() }.collectAsState(AspectoAvatar())
    val scope = rememberCoroutineScope()
    val almacen = rememberAlmacenFotos()
    var errorFoto by remember { mutableStateOf(false) }

    fun guardar(nuevo: AspectoAvatar) {
        scope.launch { repositorio.save(nuevo) }
    }

    val selector = rememberSelectorFoto { foto ->
        if (foto == null) return@rememberSelectorFoto
        scope.launch {
            val ruta = runCatching { almacen.guardar(foto) }.getOrNull()
            if (ruta == null) {
                errorFoto = true
            } else {
                errorFoto = false
                aspecto.fotoRostro?.let { almacen.borrar(it) }
                repositorio.save(aspecto.copy(fotoRostro = ruta, usarFoto = true))
            }
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloSeccion("Tu avatar")
        Text(
            "Se ajusta a tus medidas y las prendas se dibujan a su tamaño real. Aquí eliges cómo eres.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AvatarConPrendas(emptyList(), Modifier.height(320.dp))
        }

        Etiqueta("Tono de piel")
        Muestras(AspectoAvatar.PIELES, aspecto.pielRgb, "piel") { guardar(aspecto.copy(pielRgb = it)) }
        Etiqueta("Color de pelo")
        Muestras(AspectoAvatar.PELOS, aspecto.peloRgb, "pelo") { guardar(aspecto.copy(peloRgb = it)) }
        Etiqueta("Peinado")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EstiloPelo.entries.forEach { e ->
                FilterChip(
                    selected = aspecto.estiloPelo == e,
                    onClick = { guardar(aspecto.copy(estiloPelo = e)) },
                    label = { Text(e.etiqueta) },
                )
            }
        }

        Etiqueta("Tu cara")
        Text(
            "Puedes usar una foto tuya como cara del avatar. Se guarda solo en tu móvil.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = selector::tomarFoto) { Text("Hacerme una foto") }
            OutlinedButton(onClick = selector::elegirDeGaleria) { Text("Elegir de galería") }
        }
        if (aspecto.fotoRostro != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = aspecto.usarFoto,
                    onClick = { guardar(aspecto.copy(usarFoto = !aspecto.usarFoto)) },
                    label = { Text("Usar mi foto") },
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            aspecto.fotoRostro?.let { almacen.borrar(it) }
                            repositorio.save(aspecto.copy(fotoRostro = null, usarFoto = false))
                        }
                    },
                ) { Text("Quitar foto") }
            }
        }
        if (errorFoto) {
            Text("No se pudo guardar la foto. Inténtalo de nuevo.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Etiqueta(texto: String) =
    Text(texto, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun Muestras(colores: List<Int>, elegido: Int, nombre: String, alElegir: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colores.forEachIndexed { i, rgb ->
            val activo = rgb == elegido
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF000000.toInt() or rgb))
                    .border(
                        BorderStroke(if (activo) 3.dp else 1.dp, if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        CircleShape,
                    )
                    .semantics { contentDescription = "Color de $nombre ${i + 1}" + if (activo) ", elegido" else "" }
                    .clickable { alElegir(rgb) },
            )
        }
    }
}
