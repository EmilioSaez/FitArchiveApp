package app.luxion.fitarchiveapp.ui.avatar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.luxion.fitarchiveapp.avatar.PerfilCorporal
import app.luxion.fitarchiveapp.data.AvatarRepository
import app.luxion.fitarchiveapp.data.MeasurementsRepository
import app.luxion.fitarchiveapp.foto.cargarImagen
import app.luxion.fitarchiveapp.model.Measurements
import app.luxion.fitarchiveapp.tallas.CargadorTallas
import androidx.compose.ui.graphics.ImageBitmap
import app.luxion.fitarchiveapp.tallas.TablaTallas

/**
 * Provee a toda la app el [ContextoAvatar]: medidas de la usuaria, aspecto, foto de la cara y tablas de tallas.
 * Se actualiza solo al cambiar las medidas o el aspecto.
 */
@Composable
fun ProveedorAvatar(medidas: MeasurementsRepository, avatar: AvatarRepository, content: @Composable () -> Unit) {
    val m by remember(medidas) { medidas.observe() }.collectAsState(null as Measurements?)
    val aspecto by remember(avatar) { avatar.observe() }.collectAsState(app.luxion.fitarchiveapp.avatar.AspectoAvatar())
    var tablas by remember { mutableStateOf<Map<String, TablaTallas>>(emptyMap()) }
    var rostro by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) { tablas = runCatching { CargadorTallas.cargarTodas() }.getOrDefault(emptyMap()) }
    LaunchedEffect(aspecto.fotoRostro) { rostro = aspecto.fotoRostro?.let { cargarImagen(it) } }
    val ctx = remember(m, aspecto, rostro, tablas) { ContextoAvatar(PerfilCorporal.de(m), aspecto, rostro, tablas) }
    CompositionLocalProvider(LocalAvatar provides ctx, content = content)
}
