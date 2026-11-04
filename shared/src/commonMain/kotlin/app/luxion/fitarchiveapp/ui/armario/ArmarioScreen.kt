package app.luxion.fitarchiveapp.ui.armario

import app.luxion.fitarchiveapp.ui.components.Titular
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import app.luxion.fitarchiveapp.ui.theme.Iconos
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import app.luxion.fitarchiveapp.foto.cargarMiniatura
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
    onCrearOutfit: (List<Long>) -> Unit = {},
    onAbrirCarpetas: () -> Unit = {},
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
        onCrearOutfit = onCrearOutfit,
        onAbrirCarpetas = onAbrirCarpetas,
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
    onCrearOutfit: (List<Long>) -> Unit = {},
    onAbrirCarpetas: () -> Unit = {},
) {
    // Selección por pulsación larga: ids de las prendas elegidas para montar un outfit.
    var elegidas by remember { mutableStateOf(emptySet<Long>()) }
    var hojaFiltro by remember { mutableStateOf(false) }
    if (hojaFiltro) {
        HojaFiltro(estado.filtro, onAplicar = onAplicarFiltro, onCerrar = { hojaFiltro = false })
    }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val resumen = when {
                estado.totalArmario == 0 -> null
                estado.hayFiltro -> "${estado.total} de ${estado.totalArmario} prendas"
                estado.total == 1 -> "1 prenda"
                else -> "${estado.total} prendas"
            }
            Titular("Armario", Modifier.weight(1f), subtitulo = resumen)
            if (!estado.armarioVacio) {
                IconButton(onClick = onAbrirCarpetas) { Icon(Iconos.Carpeta, contentDescription = "Carpetas") }
                // Las perchas ya son los filtros; la hoja de filtros avanzados queda tras un icono de ajustes.
                IconButton(onClick = { hojaFiltro = true }) {
                    Icon(
                        Iconos.Ajustes,
                        contentDescription = "Filtros avanzados",
                        tint = if (estado.filtro.numeroCriterios() > 0) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    )
                }
                IconButton(onClick = onAnadirPrenda) { Icon(Iconos.Anadir, contentDescription = "Añadir prenda") }
            }
        }
        if (estado.armarioVacio) {
            EstadoVacio(onAnadirPrenda, Modifier.weight(1f))
            return@Column
        }
        RielPerchas(estado.filtro, onAplicarFiltro, Modifier.padding(top = 8.dp))
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
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Text(
                                if (n > 0) "${pestana.titulo} · $n" else pestana.titulo,
                                fontWeight = if (i == estado.pestanaActual) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
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
            Box(Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = if (elegidas.isEmpty()) 16.dp else 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(estado.prendas, key = { it.id }) { prenda ->
                        FotoPrenda(
                            prenda,
                            onClick = {
                                if (elegidas.isEmpty()) onAbrirPrenda(prenda)
                                else elegidas = if (prenda.id in elegidas) elegidas - prenda.id else elegidas + prenda.id
                            },
                            onAlternarFavorita = { onAlternarFavorita(prenda) },
                            seleccionada = prenda.id in elegidas,
                            onLongClick = { elegidas = elegidas + prenda.id },
                        )
                    }
                }
                if (elegidas.isNotEmpty()) {
                    BarraSeleccion(
                        elegidas.size,
                        onCrear = { onCrearOutfit(elegidas.toList()); elegidas = emptySet() },
                        onCancelar = { elegidas = emptySet() },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

/** Barra flotante negra con el recuento y la acción "Crear outfit". */
@Composable
private fun BarraSeleccion(cuantas: Int, onCrear: () -> Unit, onCancelar: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = 20.dp, vertical = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.onSurface).padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancelar) { Icon(Iconos.Cerrar, contentDescription = "Cancelar selección", tint = MaterialTheme.colorScheme.surface) }
        Text(
            if (cuantas == 1) "1 prenda elegida" else "$cuantas prendas elegidas",
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surface,
            style = MaterialTheme.typography.labelMedium,
        )
        TextButton(onClick = onCrear) { Text("Crear outfit", color = MaterialTheme.colorScheme.surface, style = MaterialTheme.typography.labelLarge) }
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
    // Sin estado intermedio: la hoja se abre entera y los botones fijos quedan a la vista.
    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Los criterios se desplazan; los botones quedan fijos abajo, siempre a la vista.
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FotoPrenda(
    prenda: Garment,
    onClick: () -> Unit,
    onAlternarFavorita: (() -> Unit)?,
    modifier: Modifier = Modifier,
    seleccionada: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val imagen by produceState<ImageBitmap?>(null, prenda.photoPath) { value = cargarMiniatura(prenda.photoPath) }
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(forma)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma)
            .then(if (seleccionada) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, forma) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
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
        if (prenda.dirty) {
            Text(
                "Sucia",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        if (seleccionada) {
            Box(
                Modifier.align(Alignment.TopStart).padding(6.dp).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Iconos.Check, contentDescription = "Elegida", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) }
        }
        if (onAlternarFavorita != null) {
            // Disco blanco al 92 % de 28 dp con el corazón en primary; el área táctil crece a 48 dp.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .clickable(onClick = onAlternarFavorita)
                    .semantics {
                        contentDescription = if (prenda.favorite) "Quitar de favoritas" else "Marcar como favorita"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (prenda.favorite) Iconos.CorazonRelleno else Iconos.Looks,
                        contentDescription = null,
                        tint = if (prenda.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
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
