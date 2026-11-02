package app.luxion.fitarchiveapp.ui.outfit

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.avatar.AjustePrenda
import app.luxion.fitarchiveapp.avatar.LIENZO_ALTO
import app.luxion.fitarchiveapp.avatar.Slot
import app.luxion.fitarchiveapp.avatar.subtipoPorDefecto
import app.luxion.fitarchiveapp.compartir.rememberCompartirOutfit
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.foto.cargarImagen
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.ui.armario.FotoPrenda
import app.luxion.fitarchiveapp.ui.avatar.AvatarConPrendas
import app.luxion.fitarchiveapp.ui.avatar.PrendaAvatar
import kotlin.time.Clock

/** Editor de outfit (T18): elige prendas por slot, las ajusta sobre el avatar y guarda el outfit. */
@Composable
fun EditorOutfitScreen(
    outfits: OutfitRepository,
    armario: GarmentRepository,
    onTerminado: () -> Unit,
    modifier: Modifier = Modifier,
    /** Outfit guardado que se edita; null crea uno nuevo. */
    outfitId: Long? = null,
) {
    val scope = rememberCoroutineScope()
    val vm = remember(outfits, armario, outfitId) {
        EditorOutfitViewModel(outfits, armario, scope, outfitId) { Clock.System.now().toEpochMilliseconds() }
    }
    val estado by vm.estado.collectAsState()
    LaunchedEffect(estado.idGuardado) { if (estado.idGuardado != null) onTerminado() }

    val imagenes = remember { mutableStateMapOf<String, ImageBitmap>() }
    val rutas = estado.elegidas.values.map { it.prenda.photoPath }
    LaunchedEffect(rutas) {
        rutas.filter { it !in imagenes }.forEach { ruta -> cargarImagen(ruta)?.let { imagenes[ruta] = it } }
    }
    val prendasAvatar = estado.elegidas.values.mapNotNull { e ->
        imagenes[e.prenda.photoPath]?.let { PrendaAvatar(it, subtipoPorDefecto(e.prenda.type), e.ajuste) }
    }

    val compartir = rememberCompartirOutfit()

    Column(modifier.fillMaxSize().padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onTerminado) { Text("Cancelar") }
            Text(
                if (outfitId == null) "Nuevo outfit" else "Editar outfit",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Button(onClick = vm::guardar, enabled = estado.puedeGuardar) { Text("Guardar") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Arrastrar mueve y pellizcar escala la prenda del slot activo.
            AvatarConPrendas(
                prendas = prendasAvatar,
                modifier = Modifier.height(300.dp).pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val unidades = LIENZO_ALTO / size.height
                        vm.mover(pan.x * unidades, pan.y * unidades)
                        if (zoom != 1f) vm.escalarPor(zoom)
                    }
                },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = estado.nombre,
                    onValueChange = vm::cambiarNombre,
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Ocasiones(estado.ocasion, vm::cambiarOcasion)
                OutlinedButton(
                    onClick = { compartir.compartir(estado.nombre, prendasAvatar) { vm.mostrarError("No se pudo compartir el outfit") } },
                    enabled = prendasAvatar.isNotEmpty(),
                ) { Text("Compartir") }
                estado.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Ajuste(estado.elegidaActiva?.ajuste, vm::fijarEscala, vm::restablecerAjuste, vm::quitarActiva)
        ScrollableTabRow(
            selectedTabIndex = SLOTS_EDITOR.indexOf(estado.slotActivo).coerceAtLeast(0),
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SLOTS_EDITOR.forEach { slot ->
                Tab(
                    selected = slot == estado.slotActivo,
                    onClick = { vm.seleccionarSlot(slot) },
                    text = { Text(slot.titulo() + if (slot in estado.elegidas) " ✓" else "") },
                )
            }
        }
        val candidatas = estado.candidatas
        if (candidatas.isEmpty()) {
            Text(
                "No tienes prendas para ${estado.slotActivo.titulo().lowercase()}",
                Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(candidatas, key = { it.id }) { prenda ->
                    FotoPrenda(prenda, onClick = { vm.alternarPrenda(prenda) }, onAlternarFavorita = null)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Ocasiones(elegida: Occasion?, onElegir: (Occasion?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Occasion.entries.forEach { o ->
            FilterChip(
                selected = o == elegida,
                onClick = { onElegir(if (o == elegida) null else o) },
                label = { Text(o.etiqueta()) },
            )
        }
    }
}

/** Escala, restablecer y quitar de la prenda del slot activo; sin prenda activa muestra una pista. */
@Composable
private fun Ajuste(ajuste: AjustePrenda?, onEscala: (Float) -> Unit, onRestablecer: () -> Unit, onQuitar: () -> Unit) {
    if (ajuste == null) {
        Text(
            "Elige una prenda; arrástrala sobre el avatar para moverla y pellizca para cambiar su tamaño.",
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Tamaño", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = ajuste.escala,
            onValueChange = onEscala,
            valueRange = AjustePrenda.ESCALA_MIN..AjustePrenda.ESCALA_MAX,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        OutlinedButton(onClick = onRestablecer) { Text("Restablecer") }
        TextButton(onClick = onQuitar) { Text("Quitar") }
    }
}

private fun Slot.titulo() = when (this) {
    Slot.TOP -> "Arriba"
    Slot.OUTER -> "Abrigo"
    Slot.BOTTOM -> "Abajo"
    Slot.DRESS -> "Vestido"
    Slot.SHOES -> "Calzado"
    Slot.ACC_NECK -> "Accesorio"
    Slot.ACC_HAND -> "Bolso"
    else -> name
}

internal fun Occasion.etiqueta() = when (this) {
    Occasion.WORK -> "Trabajo"
    Occasion.DATE -> "Cita"
    Occasion.PARTY -> "Fiesta"
    Occasion.CASUAL -> "Casual"
    Occasion.SPORT -> "Deporte"
}
