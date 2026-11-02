package app.luxion.fitarchiveapp.ui.outfit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.avatar.AjustePrenda
import app.luxion.fitarchiveapp.avatar.subtipoPorDefecto
import app.luxion.fitarchiveapp.compartir.rememberCompartirOutfit
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.foto.cargarImagen
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.ui.avatar.AvatarConPrendas
import app.luxion.fitarchiveapp.ui.avatar.PrendaAvatar

/** Pestaña Outfits (T19): "Mis outfits" con los outfits guardados; cada tarjeta se puede compartir como imagen (T20). */
@Composable
fun OutfitsScreen(
    outfits: OutfitRepository,
    armario: GarmentRepository,
    onNuevoOutfit: () -> Unit,
    onAbrirOutfit: (Outfit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val vm = remember(outfits, armario) { ListaOutfitsViewModel(outfits, armario, scope) }
    val estado by vm.estado.collectAsState()
    val compartir = rememberCompartirOutfit()

    // Las fotos se cargan una vez por ruta y se comparten entre miniaturas.
    val imagenes = remember { mutableStateMapOf<String, ImageBitmap>() }
    val rutas = estado.outfits.flatMap { o -> o.prendas.map { it.prenda.photoPath } }.distinct()
    LaunchedEffect(rutas) {
        rutas.filter { it !in imagenes }.forEach { ruta -> cargarImagen(ruta)?.let { imagenes[ruta] = it } }
    }

    estado.porEliminar?.let { outfit ->
        AlertDialog(
            onDismissRequest = vm::cancelarEliminar,
            title = { Text("Eliminar outfit") },
            text = { Text("¿Eliminar \"${outfit.name}\"? No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = vm::confirmarEliminar) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = vm::cancelarEliminar) { Text("Cancelar") } },
        )
    }
    estado.error?.let { mensaje ->
        AlertDialog(
            onDismissRequest = vm::descartarError,
            text = { Text(mensaje) },
            confirmButton = { TextButton(onClick = vm::descartarError) { Text("Aceptar") } },
        )
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Mis outfits",
                Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (!estado.vacio) Button(onClick = onNuevoOutfit) { Text("Nuevo outfit") }
        }
        when {
            estado.cargando -> Box(Modifier.weight(1f))
            estado.vacio -> EstadoVacio(onNuevoOutfit, Modifier.weight(1f))
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(estado.outfits, key = { it.outfit.id }) { item ->
                    TarjetaOutfit(
                        item,
                        imagenes,
                        onAbrir = { onAbrirOutfit(item.outfit) },
                        onFavorito = { vm.alternarFavorito(item.outfit) },
                        onEliminar = { vm.pedirEliminar(item.outfit) },
                        onCompartir = { prendas ->
                            compartir.compartir(item.outfit.name, prendas) { vm.mostrarError("No se pudo compartir el outfit") }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EstadoVacio(onNuevoOutfit: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Aún no tienes outfits", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            "Combina las prendas de tu armario sobre el avatar y guarda tus looks favoritos.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onNuevoOutfit) { Text("Nuevo outfit") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaOutfit(
    item: OutfitEnLista,
    imagenes: Map<String, ImageBitmap>,
    onAbrir: () -> Unit,
    onFavorito: () -> Unit,
    onEliminar: () -> Unit,
    onCompartir: (List<PrendaAvatar>) -> Unit,
) {
    val prendas = item.prendas.mapNotNull { p ->
        imagenes[p.prenda.photoPath]?.let {
            PrendaAvatar(it, subtipoPorDefecto(p.prenda.type), AjustePrenda(p.colocacion.x, p.colocacion.y, p.colocacion.scale))
        }
    }
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onAbrir)
            .padding(8.dp),
    ) {
        AvatarConPrendas(prendas, Modifier.fillMaxWidth().align(Alignment.CenterHorizontally))
        Text(
            item.outfit.name,
            Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            item.outfit.occasion?.etiqueta() ?: "Sin ocasión",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // Tres acciones en media pantalla: si no caben en una línea, la última pasa a la siguiente.
            val relleno = PaddingValues(horizontal = 8.dp)
            TextButton(onClick = onFavorito, contentPadding = relleno) { Text(if (item.outfit.favorite) "★" else "☆") }
            TextButton(onClick = { onCompartir(prendas) }, enabled = prendas.isNotEmpty(), contentPadding = relleno) {
                Text("Compartir", maxLines = 1, softWrap = false)
            }
            TextButton(onClick = onEliminar, contentPadding = relleno) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error, maxLines = 1, softWrap = false)
            }
        }
    }
}
