package app.luxion.fitarchiveapp.ui.outfit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.unit.DpSize
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.luxion.fitarchiveapp.avatar.AjustePrenda
import app.luxion.fitarchiveapp.avatar.LIENZO_ALTO
import app.luxion.fitarchiveapp.avatar.Slot
import app.luxion.fitarchiveapp.avatar.slotDe
import app.luxion.fitarchiveapp.avatar.subtipoPorDefecto
import app.luxion.fitarchiveapp.compartir.rememberCompartirOutfit
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.data.OutfitRepository
import app.luxion.fitarchiveapp.foto.cargarImagen
import app.luxion.fitarchiveapp.foto.cargarMiniatura
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.ui.anadir.etiqueta
import app.luxion.fitarchiveapp.ui.avatar.AvatarConPrendas
import app.luxion.fitarchiveapp.ui.avatar.PrendaAvatar
import app.luxion.fitarchiveapp.ui.theme.Iconos
import kotlin.math.roundToInt
import kotlin.time.Clock

/** Slots cuyas prendas salen en la columna de la izquierda ("ARRIBA") y en la de la derecha ("ABAJO"). */
private val SLOTS_ARRIBA = setOf(Slot.TOP, Slot.OUTER, Slot.DRESS)
private val SLOTS_ABAJO = setOf(Slot.BOTTOM)

/** Tercera fila bajo el maniquí ("MÁS"): calzado, bolsos y accesorios. El mono va arriba, con el vestido. */
private fun esDeMas(g: Garment) = slotDe(g.type) !in SLOTS_ARRIBA && slotDe(g.type) !in SLOTS_ABAJO

/** Prenda que se está arrastrando: su foto, dónde está el dedo y el tamaño de la miniatura en píxeles. */
private class Arrastre(val prenda: Garment, val miniatura: ImageBitmap?, val origen: Offset, val tamano: Size) {
    var desplazamiento by mutableStateOf(Offset.Zero)
    val posicion get() = origen + desplazamiento
    val centro get() = posicion + Offset(tamano.width / 2f, tamano.height / 2f)
}

/**
 * Editor de outfit (T18, rediseño T52): maniquí en el centro; a la izquierda la ropa de arriba y a la derecha la de abajo,
 * cada columna con scroll normal. Se arrastra una prenda (pulsación larga) al maniquí o se toca para ponerla.
 */
@Composable
fun EditorOutfitScreen(
    outfits: OutfitRepository,
    armario: GarmentRepository,
    onTerminado: () -> Unit,
    modifier: Modifier = Modifier,
    /** Outfit guardado que se edita; null crea uno nuevo. */
    outfitId: Long? = null,
    /** Prendas con las que arranca un outfit nuevo. */
    prendasIniciales: List<Long> = emptyList(),
) {
    val scope = rememberCoroutineScope()
    val vm = remember(outfits, armario, outfitId) {
        EditorOutfitViewModel(outfits, armario, scope, outfitId, prendasIniciales) { Clock.System.now().toEpochMilliseconds() }
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

    val puestas = estado.elegidas.values.map { it.prenda }
    val arriba = estado.armario.filter { slotDe(it.type) in SLOTS_ARRIBA }
    val abajo = estado.armario.filter { slotDe(it.type) in SLOTS_ABAJO }
    val mas = estado.armario.filter(::esDeMas)
    val estacion = remember { estacionDelMes(Clock.System.todayIn(TimeZone.currentSystemDefault()).month.number) }
    val recArriba = recomendadas(arriba, puestas, estacion = estacion)
    val recAbajo = recomendadas(abajo, puestas, estacion = estacion)
    val recMas = recomendadas(mas, puestas, estacion = estacion)
    // Con el maniquí vacío la sugerencia es por dónde empezar; con prendas puestas, qué combina con ellas.
    val sugerencia: Sugerencia? = (recArriba + recAbajo).firstOrNull()?.let { rec ->
        if (puestas.isEmpty()) Sugerencia(rec, null) else puestas.firstOrNull { slotDe(it.type) != slotDe(rec.type) }?.let { Sugerencia(rec, it) }
    }
    var guardando by remember { mutableStateOf(false) }

    var arrastre by remember { mutableStateOf<Arrastre?>(null) }
    var zonaManiqui by remember { mutableStateOf(Rect.Zero) }
    var origenRaiz by remember { mutableStateOf(Offset.Zero) }
    val sobreManiqui = arrastre?.let { zonaManiqui.contains(it.centro) } == true

    Box(modifier.fillMaxSize().onGloballyPositioned { origenRaiz = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(top = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onTerminado) { Text("Cancelar") }
                Text(
                    if (outfitId == null) "Nuevo outfit" else "Editar outfit",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = { compartir.compartir(estado.nombre, prendasAvatar) { vm.mostrarError("No se pudo compartir el outfit") } },
                    enabled = puedeCompartir(estado.elegidas.size, prendasAvatar.size),
                ) { Text("Compartir") }
            }

            // Rejilla 88 | maniquí | 88 con 8 de separación.
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColumnaPrendas(
                    "ARRIBA", arriba, recArriba, arrastre,
                    onToca = vm::alternarPrenda,
                    onArrastrar = { arrastre = it },
                    onSoltar = { soltado ->
                        if (sobreManiqui) vm.alternarPrenda(soltado.prenda)
                        arrastre = null
                    },
                    origenRaiz = origenRaiz,
                )
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { zonaManiqui = it.boundsInRoot() },
                        contentAlignment = Alignment.Center,
                    ) {
                        // Arrastrar mueve y pellizcar escala la prenda del slot activo.
                        AvatarConPrendas(
                            prendas = prendasAvatar,
                            modifier = Modifier.fillMaxHeight().pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val unidades = LIENZO_ALTO / size.height
                                    vm.mover(pan.x * unidades, pan.y * unidades)
                                    if (zoom != 1f) vm.escalarPor(zoom)
                                }
                            },
                        )
                        if (arrastre != null) ZonaSoltar(sobreManiqui, textoSoltar(arrastre!!.prenda), alineacionSoltar(arrastre!!.prenda))
                    }
                    estado.elegidaActiva?.let { Ajuste(it.ajuste, vm::fijarEscala) }
                }
                ColumnaPrendas(
                    "ABAJO", abajo, recAbajo, arrastre,
                    onToca = vm::alternarPrenda,
                    onArrastrar = { arrastre = it },
                    onSoltar = { soltado ->
                        if (sobreManiqui) vm.alternarPrenda(soltado.prenda)
                        arrastre = null
                    },
                    origenRaiz = origenRaiz,
                )
            }

            if (mas.isNotEmpty()) {
                FranjaMas(
                    mas, recMas, arrastre,
                    onToca = vm::alternarPrenda,
                    onArrastrar = { arrastre = it },
                    onSoltar = { soltado ->
                        if (sobreManiqui) vm.alternarPrenda(soltado.prenda)
                        arrastre = null
                    },
                )
            }
            Pie(
                puestas = estado.elegidas,
                sugerencia = sugerencia,
                onProbar = { sugerencia?.rec?.let(vm::alternarPrenda) },
                onQuitar = vm::quitar,
                onLimpiar = vm::limpiar,
                onGuardar = { guardando = true },
                puedeGuardar = estado.elegidas.isNotEmpty() && !estado.guardando && !estado.cargando,
                error = estado.error,
            )
        }

        if (guardando) {
            HojaGuardar(
                nombre = estado.nombre,
                onNombre = vm::cambiarNombre,
                ocasion = estado.ocasion,
                onOcasion = vm::cambiarOcasion,
                puedeGuardar = estado.puedeGuardar,
                onGuardar = { vm.guardar(); guardando = false },
                onCerrar = { guardando = false },
            )
        }

        // Prenda que sigue al dedo mientras se arrastra, levantada y girada -8°.
        arrastre?.let { a ->
            val pos = a.posicion - origenRaiz
            Box(
                Modifier
                    .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                    .size(88.dp, 100.dp)
                    .rotate(-8f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                a.miniatura?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            }
        }
    }
}

private fun textoSoltar(prenda: Garment) = "Suelta para cambiar " + when (slotDe(prenda.type)) {
    in SLOTS_ABAJO -> "abajo"
    in SLOTS_ARRIBA -> "arriba"
    Slot.SHOES -> "el calzado"
    Slot.ACC_HAND -> "el bolso"
    else -> "el accesorio"
}

/** Dónde se marca la zona de soltar: en pies (calzado), junto a la mano (bolso), cuello (accesorio) o todo el maniquí. */
private fun alineacionSoltar(prenda: Garment): Alignment = when (slotDe(prenda.type)) {
    Slot.SHOES -> Alignment.BottomCenter
    Slot.ACC_HAND -> Alignment.CenterEnd
    in setOf(Slot.ACC_NECK, Slot.ACC_FACE, Slot.ACC_HEAD) -> Alignment.TopCenter
    else -> Alignment.Center
}

/** Sugerencia de la barra del pie: [rec] combina con [con]; con [con] null es por dónde empezar. */
private class Sugerencia(val rec: Garment, val con: Garment?)

/** Zona de soltar sobre el maniquí: borde discontinuo de 2 dp en primary y fondo primaryContainer al 55 %. */
@Composable
private fun ZonaSoltar(activa: Boolean, texto: String, alineacion: Alignment = Alignment.Center) {
    val primary = MaterialTheme.colorScheme.primary
    val fondo = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (activa) 0.55f else 0.25f)
    val parcial = alineacion != Alignment.Center
    Box(Modifier.fillMaxSize(), contentAlignment = alineacion) {
        Canvas(if (parcial) Modifier.fillMaxWidth().fillMaxHeight(0.22f) else Modifier.fillMaxSize()) {
            drawRoundRect(fondo, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()))
            drawRoundRect(
                primary,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))),
            )
        }
        Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center, modifier = Modifier.align(alineacion).padding(8.dp))
    }
}

/**
 * Columna lateral de 88 dp con scroll vertical normal. Arriba, el bloque "PARA TI" con las [recomendadas]
 * (borde primary y estrella); debajo, el resto de prendas.
 */
@Composable
private fun ColumnaPrendas(
    rotulo: String,
    prendas: List<Garment>,
    recomendadas: List<Garment>,
    arrastreActual: Arrastre?,
    onToca: (Garment) -> Unit,
    onArrastrar: (Arrastre?) -> Unit,
    onSoltar: (Arrastre) -> Unit,
    origenRaiz: Offset,
) {
    val resto = prendas.filter { p -> recomendadas.none { it.id == p.id } }
    Column(
        Modifier.width(88.dp).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        if (prendas.isEmpty()) {
            Text("Sin prendas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
        ) {
            if (recomendadas.isNotEmpty()) {
                item(key = "titulo-para-ti") {
                    Text("PARA TI", fontSize = 10.sp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                }
                items(recomendadas, key = { "rec-${it.id}" }) { p ->
                    PrendaColumna(p, true, arrastreActual?.prenda?.id == p.id, onToca, onArrastrar, onSoltar)
                }
                item(key = "separador") { Box(Modifier.padding(horizontal = 8.dp).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant)) }
            }
            items(resto, key = { it.id }) { p ->
                PrendaColumna(p, false, arrastreActual?.prenda?.id == p.id, onToca, onArrastrar, onSoltar)
            }
        }
    }
}

/** Foto de 88 × 100 en la columna; la que se está arrastrando deja un hueco discontinuo. */
@Composable
private fun PrendaColumna(
    prenda: Garment,
    recomendada: Boolean,
    arrastrandose: Boolean,
    onToca: (Garment) -> Unit,
    onArrastrar: (Arrastre?) -> Unit,
    onSoltar: (Arrastre) -> Unit,
    tamano: DpSize = DpSize(88.dp, 100.dp),
) {
    val imagen by produceState<ImageBitmap?>(null, prenda.photoPath) { value = cargarMiniatura(prenda.photoPath) }
    var posicion by remember { mutableStateOf(Offset.Zero) }
    val forma = RoundedCornerShape(12.dp)
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(tamano)
            .onGloballyPositioned { posicion = it.positionInRoot() }
            .pointerInput(prenda.id) {
                var actual: Arrastre? = null
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        actual = Arrastre(prenda, imagen, posicion, Size(size.width.toFloat(), size.height.toFloat())).also(onArrastrar)
                    },
                    onDrag = { cambio, delta ->
                        cambio.consume()
                        actual?.let { it.desplazamiento += delta }
                    },
                    onDragEnd = { actual?.let(onSoltar); actual = null },
                    onDragCancel = { onArrastrar(null); actual = null },
                )
            }
            .clickable { onToca(prenda) },
    ) {
        if (arrastrandose) {
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(
                    primary,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
                )
            }
            return@Box
        }
        Box(
            Modifier.fillMaxSize().clip(forma).background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(if (recomendada) 2.dp else 1.dp, if (recomendada) primary else MaterialTheme.colorScheme.outlineVariant, forma),
        ) {
            imagen?.let { Image(it, contentDescription = prenda.type.etiqueta(), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        if (recomendada) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).clip(CircleShape).background(primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Iconos.Estrella, contentDescription = "Recomendada", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp)) }
        }
    }
}

/** Escala de la prenda activa (el movimiento se hace arrastrando sobre el maniquí). */
@Composable
private fun Ajuste(ajuste: AjustePrenda, onEscala: (Float) -> Unit) {
    Slider(
        value = ajuste.escala,
        onValueChange = onEscala,
        valueRange = AjustePrenda.ESCALA_MIN..AjustePrenda.ESCALA_MAX,
        modifier = Modifier.fillMaxWidth().height(32.dp),
    )
}

/** Franja "MÁS" bajo el maniquí: calzado, bolsos y accesorios con scroll horizontal; las recomendadas llevan borde primary. */
@Composable
private fun FranjaMas(
    prendas: List<Garment>,
    recomendadas: List<Garment>,
    arrastreActual: Arrastre?,
    onToca: (Garment) -> Unit,
    onArrastrar: (Arrastre?) -> Unit,
    onSoltar: (Arrastre) -> Unit,
) {
    val orden = recomendadas + prendas.filter { p -> recomendadas.none { it.id == p.id } }
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp)
            .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(18.dp).height(64.dp), contentAlignment = Alignment.Center) {
            Text(
                "MÁS",
                Modifier.requiredWidth(40.dp).rotate(-90f),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(orden, key = { it.id }) { p ->
                PrendaColumna(
                    p, recomendadas.any { it.id == p.id }, arrastreActual?.prenda?.id == p.id,
                    onToca, onArrastrar, onSoltar, tamano = DpSize(64.dp, 64.dp),
                )
            }
        }
    }
}

/** Hoja que se abre al pulsar "Guardar outfit": nombre, ocasión y Guardar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaGuardar(
    nombre: String,
    onNombre: (String) -> Unit,
    ocasion: Occasion?,
    onOcasion: (Occasion?) -> Unit,
    puedeGuardar: Boolean,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Guardar outfit", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = nombre, onValueChange = onNombre, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Occasion.entries.forEach { o ->
                    FilterChip(selected = o == ocasion, onClick = { onOcasion(if (o == ocasion) null else o) }, label = { Text(o.etiqueta()) })
                }
            }
            Button(onClick = onGuardar, enabled = puedeGuardar, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Guardar", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Pie: sugerencia, chips con lo puesto y los botones Limpiar / Guardar outfit. */
@Composable
private fun Pie(
    puestas: Map<Slot, PrendaElegida>,
    sugerencia: Sugerencia?,
    onProbar: () -> Unit,
    onQuitar: (Slot) -> Unit,
    onLimpiar: () -> Unit,
    onGuardar: () -> Unit,
    puedeGuardar: Boolean,
    error: String?,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (sugerencia != null) {
            val rec = sugerencia.rec
            val con = sugerencia.con
            val nombreRec = "${rec.type.etiqueta().lowercase()} ${rec.color.etiqueta().lowercase()}"
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Iconos.Estrella, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(
                    if (con == null) "Empieza por tu $nombreRec"
                    else "Con tu ${con.type.etiqueta().lowercase()} ${con.color.etiqueta().lowercase()} combina $nombreRec",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Button(onClick = onProbar, modifier = Modifier.heightIn(min = 40.dp)) { Text("Probar", style = MaterialTheme.typography.labelMedium) }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        if (puestas.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                puestas.entries.sortedBy { it.key.z }.forEach { (slot, e) ->
                    InputChip(
                        selected = true,
                        onClick = { onQuitar(slot) },
                        label = { Text("${e.prenda.type.etiqueta()} ${e.prenda.color.etiqueta().lowercase()}") },
                        trailingIcon = { Icon(Iconos.Cerrar, contentDescription = "Quitar", modifier = Modifier.size(16.dp)) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onLimpiar, enabled = puestas.isNotEmpty(), modifier = Modifier.heightIn(min = 48.dp)) { Text("Limpiar") }
            Button(onClick = onGuardar, enabled = puedeGuardar, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                Text("Guardar outfit", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Compartir exige que todas las prendas elegidas tengan ya su foto cargada; si no, la imagen saldría sin ellas. */
internal fun puedeCompartir(elegidas: Int, cargadas: Int) = elegidas > 0 && cargadas == elegidas

internal fun Occasion.etiqueta() = when (this) {
    Occasion.WORK -> "Trabajo"
    Occasion.DATE -> "Cita"
    Occasion.PARTY -> "Fiesta"
    Occasion.CASUAL -> "Casual"
    Occasion.SPORT -> "Deporte"
}
