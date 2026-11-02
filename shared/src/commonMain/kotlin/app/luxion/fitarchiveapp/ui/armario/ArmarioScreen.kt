package app.luxion.fitarchiveapp.ui.armario

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.data.GarmentFilter
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.foto.cargarImagen
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.ui.anadir.etiqueta

/**
 * Pantalla del armario con pestañas (tipo / color / estación) y filtro multicriterio.
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
        onSeleccionarAgrupacion = viewModel::seleccionarAgrupacion,
        onAplicarFiltro = viewModel::aplicarFiltro,
        onAnadirPrenda = onAnadirPrenda,
        onAbrirPrenda = onAbrirPrenda,
        onAlternarFavorita = viewModel::alternarFavorita,
        modifier = modifier,
    )
}

@Composable
internal fun ArmarioContenido(
    estado: ArmarioUiState,
    onSeleccionarPestana: (Int) -> Unit,
    onAnadirPrenda: () -> Unit,
    onAbrirPrenda: (Garment) -> Unit,
    onAlternarFavorita: (Garment) -> Unit,
    modifier: Modifier = Modifier,
    onSeleccionarAgrupacion: (Agrupacion) -> Unit = {},
    onAplicarFiltro: (GarmentFilter) -> Unit = {},
) {
    var hojaFiltro by remember { mutableStateOf(false) }
    if (hojaFiltro) {
        HojaFiltro(estado.filtro, onAplicar = onAplicarFiltro, onCerrar = { hojaFiltro = false })
    }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Armario", style = MaterialTheme.typography.headlineLarge)
                if (estado.totalArmario > 0) {
                    val resumen = when {
                        estado.hayFiltro -> "${estado.total} de ${estado.totalArmario} prendas"
                        estado.total == 1 -> "1 prenda"
                        else -> "${estado.total} prendas"
                    }
                    Text(
                        resumen,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!estado.armarioVacio) {
                TextButton(onClick = { hojaFiltro = true }) {
                    val n = estado.filtro.numeroCriterios()
                    Text(if (n > 0) "Filtrar ($n)" else "Filtrar")
                }
                TextButton(onClick = onAnadirPrenda) { Text("+ Añadir") }
            }
        }
        if (estado.armarioVacio) {
            EstadoVacio(onAnadirPrenda, Modifier.weight(1f))
            return@Column
        }
        SelectorAgrupacion(estado.agrupacion, onSeleccionarAgrupacion)
        if (estado.hayFiltro) FiltrosActivos(estado.filtro, onAplicarFiltro)
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
        if (estado.filtroSinResultados) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Ninguna prenda coincide con el filtro",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { onAplicarFiltro(GarmentFilter()) }) { Text("Quitar filtros") }
            }
        } else if (estado.prendas.isEmpty()) {
            val titulo = estado.pestanas.getOrNull(estado.pestanaActual)?.titulo.orEmpty().lowercase()
            Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (estado.hayFiltro) "Sin resultados en $titulo con este filtro" else "Aún no tienes prendas en $titulo",
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
                    FotoPrenda(prenda, onClick = { onAbrirPrenda(prenda) }, onAlternarFavorita = { onAlternarFavorita(prenda) })
                }
            }
        }
    }
}

@Composable
private fun SelectorAgrupacion(actual: Agrupacion, onSeleccionar: (Agrupacion) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Ver por", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Agrupacion.todas.forEach { a ->
            FilterChip(selected = a == actual, onClick = { onSeleccionar(a) }, label = { Text(a.titulo) })
        }
    }
}

/** Chips de los criterios activos; tocar uno lo quita. */
@Composable
private fun FiltrosActivos(filtro: GarmentFilter, onCambiar: (GarmentFilter) -> Unit) {
    @Composable
    fun Activo(texto: String, quitar: () -> Unit) =
        InputChip(selected = true, onClick = quitar, label = { Text(texto) }, trailingIcon = { Text("×") })

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        filtro.type?.let { Activo(it.etiqueta()) { onCambiar(filtro.copy(type = null)) } }
        filtro.color?.let { Activo(it.etiqueta()) { onCambiar(filtro.copy(color = null)) } }
        filtro.season?.let { Activo(it.etiqueta()) { onCambiar(filtro.copy(season = null)) } }
        filtro.usage?.let { Activo(it.etiqueta()) { onCambiar(filtro.copy(usage = null)) } }
        if (filtro.favorite == true) Activo("Favoritas") { onCambiar(filtro.copy(favorite = null)) }
        TextButton(onClick = { onCambiar(GarmentFilter()) }) { Text("Limpiar") }
    }
}

/** Hoja inferior para combinar tipo, color, estación, uso y favoritas. Se aplica al pulsar "Aplicar". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaFiltro(inicial: GarmentFilter, onAplicar: (GarmentFilter) -> Unit, onCerrar: () -> Unit) {
    var filtro by remember { mutableStateOf(inicial) }
    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Filtrar", style = MaterialTheme.typography.headlineMedium)
            Grupo("Tipo", GarmentType.entries, filtro.type, { it.etiqueta() }) { filtro = filtro.copy(type = it) }
            Grupo("Color", GarmentColor.entries, filtro.color, { it.etiqueta() }) { filtro = filtro.copy(color = it) }
            Grupo("Estación", Season.entries, filtro.season, { it.etiqueta() }) { filtro = filtro.copy(season = it) }
            Grupo("Uso", Usage.entries, filtro.usage, { it.etiqueta() }) { filtro = filtro.copy(usage = it) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Solo favoritas", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = filtro.favorite == true,
                    onCheckedChange = { filtro = filtro.copy(favorite = if (it) true else null) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { filtro = GarmentFilter() }, Modifier.weight(1f)) { Text("Limpiar") }
                Button(onClick = { onAplicar(filtro); onCerrar() }, Modifier.weight(1f)) { Text("Aplicar") }
            }
        }
    }
}

/** Selección única opcional: tocar el elegido lo desmarca. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Grupo(titulo: String, valores: List<T>, elegido: T?, etiqueta: (T) -> String, alCambiar: (T?) -> Unit) {
    Text(titulo, style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        valores.forEach { v ->
            FilterChip(
                selected = v == elegido,
                onClick = { alCambiar(if (v == elegido) null else v) },
                label = { Text(etiqueta(v)) },
            )
        }
    }
}

@Composable
internal fun FotoPrenda(
    prenda: Garment,
    onClick: () -> Unit,
    onAlternarFavorita: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
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
        if (onAlternarFavorita != null) TextButton(
            onClick = onAlternarFavorita,
            modifier = Modifier.align(Alignment.TopEnd).semantics {
                contentDescription = if (prenda.favorite) "Quitar de favoritas" else "Marcar como favorita"
            },
        ) {
            Text(
                if (prenda.favorite) "♥" else "♡",
                style = MaterialTheme.typography.titleLarge,
                color = if (prenda.favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
