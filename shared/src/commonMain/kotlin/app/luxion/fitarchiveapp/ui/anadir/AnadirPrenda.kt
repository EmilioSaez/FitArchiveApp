package app.luxion.fitarchiveapp.ui.anadir

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.foto.AlmacenFotos
import app.luxion.fitarchiveapp.foto.FotoCapturada
import app.luxion.fitarchiveapp.foto.decodificarImagen
import app.luxion.fitarchiveapp.foto.rememberAlmacenFotos
import app.luxion.fitarchiveapp.foto.rememberSelectorFoto
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Punto de entrada de T08: botón añadir → foto (cámara/galería) → formulario → guardar.
 * [onTerminado] se llama al guardar o al cancelar. Los enganches [quitarFondo] (T07) y
 * [detectarColor] (T09) se pasan aquí; por defecto no hacen nada.
 */
@Composable
fun AnadirPrendaFlow(
    garments: GarmentRepository,
    onTerminado: () -> Unit,
    modifier: Modifier = Modifier,
    quitarFondo: QuitarFondo = SinQuitarFondo,
    detectarColor: DetectarColor = SinDetectarColor,
    almacen: AlmacenFotos = rememberAlmacenFotos(),
) {
    var foto by remember { mutableStateOf<FotoCapturada?>(null) }
    var colorDetectado by remember { mutableStateOf<GarmentColor?>(null) }
    var procesando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val selector = rememberSelectorFoto { resultado ->
        if (resultado != null) {
            scope.launch {
                procesando = true
                val sinFondo = runCatching { quitarFondo(resultado) }.getOrDefault(resultado)
                colorDetectado = runCatching { detectarColor(sinFondo) }.getOrNull()
                foto = sinFondo
                procesando = false
            }
        }
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val actual = foto
        when {
            procesando -> CircularProgressIndicator(
                Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )
            actual == null -> ElegirFoto(
                onCamara = selector::tomarFoto,
                onGaleria = selector::elegirDeGaleria,
                onCancelar = onTerminado,
            )
            else -> FormularioPrenda(
                foto = actual,
                colorInicial = colorDetectado,
                onCambiarFoto = { foto = null; colorDetectado = null },
                onCancelar = onTerminado,
                onGuardar = { borrador ->
                    scope.launch {
                        val ruta = almacen.guardar(actual)
                        val prenda = borrador.aPrenda(ruta, Clock.System.now().toEpochMilliseconds())
                        if (prenda != null) {
                            garments.add(prenda)
                            onTerminado()
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ElegirFoto(onCamara: () -> Unit, onGaleria: () -> Unit, onCancelar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Nueva prenda", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("Empieza con una foto: se guardará solo en tu móvil.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onCamara, modifier = Modifier.fillMaxWidth()) { Text("Hacer foto") }
        OutlinedButton(onClick = onGaleria, modifier = Modifier.fillMaxWidth()) { Text("Elegir de la galería") }
        TextButton(onClick = onCancelar) { Text("Cancelar") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioPrenda(
    foto: FotoCapturada,
    colorInicial: GarmentColor?,
    onCambiarFoto: () -> Unit,
    onCancelar: () -> Unit,
    onGuardar: (BorradorPrenda) -> Unit,
) {
    var borrador by remember { mutableStateOf(BorradorPrenda(color = colorInicial)) }
    var guardando by remember { mutableStateOf(false) }
    val imagen = remember(foto) { decodificarImagen(foto.bytes) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Nueva prenda", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)

        Box(
            Modifier.fillMaxWidth().aspectRatio(4f / 5f).clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (imagen != null) {
                Image(
                    imagen,
                    contentDescription = "Foto de la prenda",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text("No se puede mostrar la foto")
            }
        }
        TextButton(onClick = onCambiarFoto, enabled = !guardando) { Text("Cambiar foto") }

        Seccion("Tipo *") {
            Opciones(GarmentType.entries, borrador.type, { it.etiqueta() }) { borrador = borrador.copy(type = it) }
        }
        Seccion("Color *") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GarmentColor.entries.forEach { c ->
                    MuestraColor(c, seleccionado = c == borrador.color) { borrador = borrador.copy(color = c) }
                }
            }
            borrador.color?.let { Text(it.etiqueta(), style = MaterialTheme.typography.bodySmall) }
        }
        Seccion("Estación *") {
            Opciones(Season.entries, borrador.season, { it.etiqueta() }) { borrador = borrador.copy(season = it) }
        }
        Seccion("Uso *") {
            Opciones(Usage.entries, borrador.usage, { it.etiqueta() }) { borrador = borrador.copy(usage = it) }
        }
        Seccion("Marca") {
            OutlinedTextField(
                value = borrador.brand,
                onValueChange = { borrador = borrador.copy(brand = it) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Opcional") },
            )
            val sugerencias = sugerenciasDeMarca(borrador.brand)
            if (sugerencias.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sugerencias.forEach { m ->
                        SuggestionChip(onClick = { borrador = borrador.copy(brand = m) }, label = { Text(m) })
                    }
                }
            }
        }
        Seccion("Talla") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TALLAS.forEach { t ->
                    val elegida = borrador.size == t
                    FilterChip(
                        selected = elegida,
                        // Tocar la talla elegida la quita: la talla es opcional.
                        onClick = { borrador = borrador.copy(size = if (elegida) null else t) },
                        label = { Text(t) },
                        colors = chipColors(),
                    )
                }
            }
        }

        HorizontalDivider()
        Button(
            onClick = { guardando = true; onGuardar(borrador) },
            enabled = borrador.completo && !guardando,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Guardar prenda") }
        if (!borrador.completo) {
            Text("Elige tipo, color, estación y uso para guardar.", style = MaterialTheme.typography.bodySmall)
        }
        TextButton(
            onClick = onCancelar,
            enabled = !guardando,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text("Cancelar") }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        contenido()
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Opciones(valores: List<T>, elegido: T?, etiqueta: (T) -> String, alElegir: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        valores.forEach { v ->
            FilterChip(
                selected = v == elegido,
                onClick = { alElegir(v) },
                label = { Text(etiqueta(v)) },
                colors = chipColors(),
            )
        }
    }
}

@Composable
private fun MuestraColor(color: GarmentColor, seleccionado: Boolean, onClick: () -> Unit) {
    val borde = if (seleccionado) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
    else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val interior = Modifier.size(34.dp).clip(CircleShape)
    Box(
        Modifier.size(44.dp).clip(CircleShape).border(borde, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (color == GarmentColor.MULTI) {
            Row(interior) {
                listOf(Color(0xFFD64545), Color(0xFFE8B931), Color(0xFF3F7FBF)).forEach {
                    Box(Modifier.weight(1f).fillMaxSize().background(it))
                }
            }
        } else {
            Box(interior.background(color.muestra()))
        }
    }
}

private fun GarmentColor.muestra(): Color = when (this) {
    GarmentColor.BLACK -> Color(0xFF1A1A1A)
    GarmentColor.WHITE -> Color(0xFFFFFFFF)
    GarmentColor.GREY -> Color(0xFF9E9E9E)
    GarmentColor.BEIGE -> Color(0xFFD9C7A8)
    GarmentColor.BROWN -> Color(0xFF7B5232)
    GarmentColor.RED -> Color(0xFFC62828)
    GarmentColor.BURGUNDY -> Color(0xFF6D1A36)
    GarmentColor.PINK -> Color(0xFFF2A7BC)
    GarmentColor.ORANGE -> Color(0xFFEF8A2B)
    GarmentColor.YELLOW -> Color(0xFFF2D13B)
    GarmentColor.GREEN -> Color(0xFF3E7D4F)
    GarmentColor.BLUE -> Color(0xFF3F7FBF)
    GarmentColor.NAVY -> Color(0xFF1C2B4F)
    GarmentColor.PURPLE -> Color(0xFF7A4CA0)
    GarmentColor.MULTI -> Color.Transparent
}
