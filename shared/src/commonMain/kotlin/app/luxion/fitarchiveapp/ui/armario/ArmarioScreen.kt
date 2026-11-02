package app.luxion.fitarchiveapp.ui.armario

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.foto.cargarImagen
import app.luxion.fitarchiveapp.model.Garment

/**
 * Pantalla del armario con pestañas.
 *
 * [onAnadirPrenda] es el único punto de entrada para crear prendas (lo engancha T08).
 * [onAbrirPrenda] queda listo para el detalle (T13).
 */
@Composable
fun ArmarioScreen(
    repositorio: GarmentRepository,
    onAnadirPrenda: () -> Unit,
    modifier: Modifier = Modifier,
    onAbrirPrenda: (Garment) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(repositorio) { ArmarioViewModel(repositorio, scope) }
    val estado by viewModel.estado.collectAsState()
    ArmarioContenido(
        estado = estado,
        onSeleccionarPestana = viewModel::seleccionarPestana,
        onAnadirPrenda = onAnadirPrenda,
        onAbrirPrenda = onAbrirPrenda,
        modifier = modifier,
    )
}

@Composable
internal fun ArmarioContenido(
    estado: ArmarioUiState,
    onSeleccionarPestana: (Int) -> Unit,
    onAnadirPrenda: () -> Unit,
    onAbrirPrenda: (Garment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Armario", style = MaterialTheme.typography.headlineLarge)
                if (estado.total > 0) {
                    Text(
                        if (estado.total == 1) "1 prenda" else "${estado.total} prendas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!estado.armarioVacio) TextButton(onClick = onAnadirPrenda) { Text("+ Añadir") }
        }
        if (estado.armarioVacio) {
            EstadoVacio(onAnadirPrenda, Modifier.weight(1f))
            return@Column
        }
        if (estado.pestanas.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = estado.pestanaActual,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                estado.pestanas.forEachIndexed { i, pestana ->
                    val n = estado.conteos.getOrElse(i) { 0 }
                    Tab(
                        selected = i == estado.pestanaActual,
                        onClick = { onSeleccionarPestana(i) },
                        text = { Text(if (n > 0) "${pestana.titulo} · $n" else pestana.titulo) },
                    )
                }
            }
        }
        if (estado.prendas.isEmpty()) {
            val titulo = estado.pestanas.getOrNull(estado.pestanaActual)?.titulo.orEmpty().lowercase()
            Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Aún no tienes prendas en $titulo",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(estado.prendas, key = { it.id }) { prenda ->
                    FotoPrenda(prenda, onClick = { onAbrirPrenda(prenda) })
                }
            }
        }
    }
}

@Composable
private fun FotoPrenda(prenda: Garment, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val imagen by produceState<ImageBitmap?>(null, prenda.photoPath) { value = cargarImagen(prenda.photoPath) }
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        imagen?.let {
            Image(
                bitmap = it,
                contentDescription = prenda.brand,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun EstadoVacio(onAnadirPrenda: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Tu armario está vacío",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            "Fotografía tu primera prenda y empieza a construir tu armario digital.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        )
        Button(onClick = onAnadirPrenda) { Text("Añadir mi primera prenda") }
    }
}
