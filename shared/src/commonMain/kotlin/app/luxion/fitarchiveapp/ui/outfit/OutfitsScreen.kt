package app.luxion.fitarchiveapp.ui.outfit

import app.luxion.fitarchiveapp.ui.components.Titular
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.luxion.fitarchiveapp.ui.theme.Iconos
import app.luxion.fitarchiveapp.ui.theme.estiloCartela
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.avatar.ajusteGuardado
import app.luxion.fitarchiveapp.avatar.subtipoPorDefecto
import app.luxion.fitarchiveapp.compartir.rememberCompartirOutfit
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.foto.rememberMiniaturas
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

    var soloFavoritos by remember { mutableStateOf(true) }
    val visibles = if (soloFavoritos) estado.outfits.filter { it.outfit.favorite } else estado.outfits

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Titular("Tus looks", Modifier.weight(1f), subtitulo = "Sujetos con clips, como en un tendedero")
            if (!estado.vacio) Button(onClick = onNuevoOutfit) { Text("Nuevo outfit") }
        }
        when {
            estado.cargando -> Box(Modifier.weight(1f))
            estado.vacio -> EstadoVacio(onNuevoOutfit, Modifier.weight(1f))
            else -> {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = soloFavoritos,
                        onClick = { soloFavoritos = true },
                        label = { Text("Favoritos") },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.heightIn(min = 40.dp),
                    )
                    FilterChip(
                        selected = !soloFavoritos,
                        onClick = { soloFavoritos = false },
                        label = { Text("Todos") },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.heightIn(min = 40.dp),
                    )
                }
                if (visibles.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Aún no tienes looks favoritos. Toca el corazón de un look para colgarlo aquí.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Galeria(
                        visibles,
                        Modifier.weight(1f),
                        onAbrir = { onAbrirOutfit(it.outfit) },
                        onFavorito = { vm.alternarFavorito(it.outfit) },
                        onEliminar = { vm.pedirEliminar(it.outfit) },
                        onCompartir = { item, prendas ->
                            compartir.compartir(item.outfit.name, prendas) { vm.mostrarError("No se pudo compartir el outfit") }
                        },
                    )
                }
            }
        }
    }
}

/** Rejilla vertical de 2 polaroids por fila, sujetas con clips a un cordel como en un tendedero de fotos. */
@Composable
private fun Galeria(
    outfits: List<OutfitEnLista>,
    modifier: Modifier,
    onAbrir: (OutfitEnLista) -> Unit,
    onFavorito: (OutfitEnLista) -> Unit,
    onEliminar: (OutfitEnLista) -> Unit,
    onCompartir: (OutfitEnLista, List<PrendaAvatar>) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        itemsIndexed(outfits, key = { _, it -> it.outfit.id }) { indice, item ->
            Polaroid(indice, item, onAbrir, onFavorito, onEliminar, onCompartir)
        }
    }
}

/** Clip de pinza de madera que sujeta la foto al cordel. */
@Composable
private fun Clip(modifier: Modifier = Modifier) {
    Box(
        modifier.size(14.dp, 30.dp).shadow(1.dp, RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(3.dp)),
    ) {
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(2.dp).background(Color.Black.copy(alpha = 0.25f)))
    }
}

/** Polaroid: cordel a todo el ancho de la celda, dos clips, foto con marco blanco y cartela con nombre y ocasión. */
@Composable
private fun Polaroid(
    indice: Int,
    item: OutfitEnLista,
    onAbrir: (OutfitEnLista) -> Unit,
    onFavorito: (OutfitEnLista) -> Unit,
    onEliminar: (OutfitEnLista) -> Unit,
    onCompartir: (OutfitEnLista, List<PrendaAvatar>) -> Unit,
) {
    // Cada polaroid carga solo sus miniaturas, y solo cuando la rejilla la compone (carga perezosa con caché).
    val imagenes = rememberMiniaturas(item.prendas.map { it.prenda.photoPath })
    val prendas = item.prendas.mapNotNull { p ->
        imagenes[p.prenda.photoPath]?.let {
            PrendaAvatar.de(p.prenda, it, subtipoPorDefecto(p.prenda.type), p.colocacion.ajusteGuardado())
        }
    }
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        // Cordel continuo: las celdas no tienen separación horizontal.
        Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(2.dp).background(MaterialTheme.colorScheme.outline))
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(top = 4.dp)
                .graphicsLayer { rotationZ = if (indice % 2 == 0) -1.5f else 1.5f },
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = 14.dp)
                    .shadow(3.dp, RoundedCornerShape(2.dp))
                    .background(Color.White)
                    .clickable { onAbrir(item) }
                    .padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
            ) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    AvatarConPrendas(prendas, Modifier.fillMaxHeight().padding(6.dp))
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(2.dp).size(40.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onFavorito(item) }
                            .semantics { contentDescription = if (item.outfit.favorite) "Quitar de favoritos" else "Marcar como favorito" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (item.outfit.favorite) Iconos.CorazonRelleno else Iconos.Looks,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(start = 2.dp)) {
                        Text(item.outfit.name, style = estiloCartela(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            (item.outfit.occasion?.etiqueta() ?: "Sin ocasión").uppercase() + " · " +
                                if (item.prendas.size == 1) "1 prenda" else "${item.prendas.size} prendas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // Compartir y Eliminar quedan tras el menú.
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Iconos.MasVertical, contentDescription = "Más acciones") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Compartir") },
                                enabled = prendas.isNotEmpty(),
                                onClick = { menu = false; onCompartir(item, prendas) },
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; onEliminar(item) },
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Clip()
                Clip()
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
        Text("Aún no tienes looks", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            "Combina las prendas de tu armario sobre el avatar y guarda tus looks favoritos.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onNuevoOutfit) { Text("Nuevo outfit") }
    }
}
