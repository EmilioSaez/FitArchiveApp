package app.luxion.fitarchiveapp.ui.carpetas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.foto.cargarMiniatura
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.ui.armario.FotoPrenda
import app.luxion.fitarchiveapp.ui.armario.PestanaTipo
import app.luxion.fitarchiveapp.ui.theme.Iconos

/** Una carpeta: vista filtrada del armario por familia de prenda, más "Todo" y "Favoritos". */
internal sealed class Carpeta(val titulo: String) {
    abstract fun admite(prenda: Garment): Boolean

    data object Favoritos : Carpeta("Favoritos") {
        override fun admite(prenda: Garment) = prenda.favorite
    }

    data object Todo : Carpeta("Todo") {
        override fun admite(prenda: Garment) = true
    }

    data class PorTipo(val familia: PestanaTipo) : Carpeta(familia.titulo) {
        override fun admite(prenda: Garment) = prenda.type in familia.tipos
    }

    companion object {
        val todas: List<Carpeta> = listOf(Favoritos, Todo) + PestanaTipo.entries.map { PorTipo(it) }
    }
}

@Composable
fun CarpetasScreen(
    repositorio: GarmentRepository,
    onVolver: () -> Unit,
    onAbrirPrenda: (Garment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prendas by repositorio.observeAll().collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var abierta by remember { mutableStateOf<Carpeta?>(null) }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (abierta != null) abierta = null else onVolver() }) {
                Icon(Iconos.Volver, contentDescription = "Volver")
            }
            Column(Modifier.weight(1f)) {
                Text("CARPETAS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(abierta?.titulo ?: "Tu ropa, ordenada", style = MaterialTheme.typography.headlineSmall)
            }
        }
        val carpeta = abierta
        if (carpeta == null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(Carpeta.todas, key = { it.titulo }) { c ->
                    CarpetaTarjeta(c, prendas.filter { c.admite(it) }) { abierta = c }
                }
            }
        } else {
            val contenido = prendas.filter { carpeta.admite(it) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(contenido, key = { it.id }) { prenda ->
                    FotoPrenda(
                        prenda,
                        onClick = { onAbrirPrenda(prenda) },
                        onAlternarFavorita = { scope.launch { repositorio.setFavorite(prenda.id, !prenda.favorite) } },
                    )
                }
            }
        }
    }
}

/** Carpeta de 112 dp: pestaña + fondo en secondary, 3 miniaturas asomando y un frente con nombre y recuento. */
@Composable
private fun CarpetaTarjeta(carpeta: Carpeta, contenido: List<Garment>, onClick: () -> Unit) {
    val esFavoritos = carpeta == Carpeta.Favoritos
    val fondo = if (esFavoritos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val frente = if (esFavoritos) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val texto = if (esFavoritos) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Box(Modifier.fillMaxWidth().height(112.dp).clickable(onClick = onClick)) {
        // Pestaña y fondo.
        Box(Modifier.padding(start = 0.dp).size(56.dp, 16.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(fondo))
        Box(Modifier.padding(top = 12.dp).fillMaxSize().clip(RoundedCornerShape(topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)).background(fondo))
        // Miniaturas que asoman.
        Row(Modifier.padding(start = 12.dp, top = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            contenido.take(3).forEach { Miniatura(it) }
        }
        // Frente.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(62.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, topEnd = 4.dp))
                .background(frente).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (esFavoritos) {
                    Icon(Iconos.CorazonRelleno, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Box(Modifier.size(6.dp))
                }
                Text(carpeta.titulo, style = MaterialTheme.typography.titleMedium, color = texto)
            }
            Text(
                if (contenido.size == 1) "1 prenda" else "${contenido.size} prendas",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = texto,
            )
        }
    }
}

@Composable
private fun Miniatura(prenda: Garment) {
    val imagen by produceState<ImageBitmap?>(null, prenda.photoPath) { value = cargarMiniatura(prenda.photoPath) }
    Box(Modifier.size(40.dp, 52.dp).clip(RoundedCornerShape(6.dp)).background(Color.White).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))) {
        imagen?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}
