package app.luxion.fitarchiveapp.ui.detalle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.data.MeasurementsRepository
import app.luxion.fitarchiveapp.data.UsoRepository
import app.luxion.fitarchiveapp.foto.AlmacenFotos
import app.luxion.fitarchiveapp.foto.cargarImagen
import app.luxion.fitarchiveapp.foto.rememberAlmacenFotos
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.SOLO_MANUAL
import app.luxion.fitarchiveapp.model.usosParaLavar
import app.luxion.fitarchiveapp.ui.anadir.BorradorPrenda
import app.luxion.fitarchiveapp.ui.anadir.FormularioPrenda
import app.luxion.fitarchiveapp.ui.anadir.etiqueta
import kotlinx.coroutines.launch

/**
 * Detalle de una prenda (T13): foto grande, datos, editar y eliminar con confirmación.
 * [onVolver] se llama al pulsar atrás y también tras eliminar la prenda.
 */
@Composable
fun DetallePrendaScreen(
    garments: GarmentRepository,
    measurements: MeasurementsRepository,
    usos: UsoRepository,
    prendaId: Long,
    onVolver: () -> Unit,
    onAbrirPerfil: () -> Unit,
    modifier: Modifier = Modifier,
    almacen: AlmacenFotos = rememberAlmacenFotos(),
) {
    var editando by remember { mutableStateOf(false) }
    var confirmando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val estado by remember(garments, prendaId) { garments.observe(prendaId) }.collectAsState(initial = null)
    // Se recuerda la última prenda vista para no parpadear mientras llega la primera emisión
    // ni al borrar (la fila desaparece antes de que se cierre la pantalla).
    var prenda by remember { mutableStateOf<Garment?>(null) }
    LaunchedEffect(estado) { estado?.let { prenda = it } }
    val actual = estado ?: prenda ?: return

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.systemBars)) {
        if (editando) {
            val imagen by produceState<ImageBitmap?>(null, actual.photoPath) { value = cargarImagen(actual.photoPath) }
            FormularioPrenda(
                titulo = "Editar prenda",
                imagen = imagen,
                borradorInicial = BorradorPrenda.desde(actual),
                textoGuardar = "Guardar cambios",
                onCambiarFoto = null,
                onCancelar = { editando = false },
                onGuardar = { borrador ->
                    scope.launch {
                        borrador.aplicarA(actual)?.let { garments.update(it) }
                        editando = false
                    }
                },
            )
        } else {
            DetalleContenido(
                prenda = actual,
                onVolver = onVolver,
                onEditar = { editando = true },
                onEliminar = { confirmando = true },
                onAlternarLimpieza = { scope.launch { garments.setDirty(actual.id, !actual.dirty) } },
                onCambiarUsosParaLavar = { n -> scope.launch { garments.update(actual.copy(washAfterWears = n)) } },
                measurements = measurements,
                usos = usos,
                onAbrirPerfil = onAbrirPerfil,
            )
        }
    }

    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text("¿Eliminar esta prenda?") },
            text = {
                Text("Se borrará también su foto y desaparecerá de tus outfits y maletas. No se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmando = false
                    scope.launch {
                        eliminarPrenda(garments, almacen, actual)
                        onVolver()
                    }
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmando = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun DetalleContenido(
    prenda: Garment,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onAlternarLimpieza: () -> Unit,
    onCambiarUsosParaLavar: (Int) -> Unit,
    measurements: MeasurementsRepository,
    usos: UsoRepository,
    onAbrirPerfil: () -> Unit,
) {
    val imagen by produceState<ImageBitmap?>(null, prenda.photoPath) { value = cargarImagen(prenda.photoPath) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TextButton(onClick = onVolver, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) { Text("← Armario") }

        // Foto grande a sangre, estilo revista.
        Box(
            Modifier.fillMaxWidth().aspectRatio(4f / 5f).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            imagen?.let {
                Image(
                    bitmap = it,
                    contentDescription = prenda.brand ?: prenda.type.etiqueta(),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    (prenda.brand ?: "Sin marca").uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    prenda.type.etiqueta(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (prenda.favorite) {
                    Text("♥ Favorita", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider()
            Dato("Color", prenda.color.etiqueta())
            Dato("Estación", prenda.season.etiqueta())
            Dato("Uso", prenda.usage.etiqueta())
            Dato("Marca", prenda.brand ?: "—")
            Dato("Talla", prenda.size ?: "—")

            HorizontalDivider()
            HistorialUsos(prenda, usos)
            HorizontalDivider()

            SeccionTalla(prenda, measurements, onAbrirPerfil)
            HuecoEstadoLimpieza(prenda, onAlternarLimpieza, onCambiarUsosParaLavar)

            HorizontalDivider()
            OutlinedButton(onClick = onEditar, modifier = Modifier.fillMaxWidth()) { Text("Editar") }
            TextButton(onClick = onEliminar, modifier = Modifier.fillMaxWidth()) {
                Text("Eliminar prenda", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Estado limpia/sucia (T34) con un control para alternarlo. Al marcar limpia se reinicia el contador de usos. */
@Composable
private fun HuecoEstadoLimpieza(prenda: Garment, onAlternar: () -> Unit, onCambiarUsos: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Dato("Estado", if (prenda.dirty) "Sucia" else "Limpia")
        if (prenda.dirty) {
            Button(onClick = onAlternar, modifier = Modifier.fillMaxWidth()) { Text("Marcar como limpia") }
        } else {
            OutlinedButton(onClick = onAlternar, modifier = Modifier.fillMaxWidth()) { Text("Marcar como sucia") }
        }
        UsosParaLavar(prenda.usosParaLavar() ?: SOLO_MANUAL, onCambiarUsos)
    }
}

/** Ajuste (T35) de los usos tras los que la prenda pasa sola a sucia; 0 = solo a mano. */
@Composable
private fun UsosParaLavar(usos: Int, onCambiar: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            when (usos) {
                SOLO_MANUAL -> "Sucia: solo a mano"
                1 -> "Sucia tras 1 uso"
                else -> "Sucia tras $usos usos"
            },
            Modifier.weight(1f),
        )
        TextButton(onClick = { onCambiar(usos - 1) }, enabled = usos > SOLO_MANUAL) { Text("−") }
        TextButton(onClick = { onCambiar(usos + 1) }, enabled = usos < MAX_USOS_PARA_LAVAR) { Text("+") }
    }
}

private const val MAX_USOS_PARA_LAVAR = 30

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
