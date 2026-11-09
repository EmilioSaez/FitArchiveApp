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
import app.luxion.fitarchiveapp.data.MeasurementsRepository
import app.luxion.fitarchiveapp.model.Measurements
import app.luxion.fitarchiveapp.tallas.CargadorTallas
import app.luxion.fitarchiveapp.tallas.TablaTallas

/**
 * Provee a toda la app el [ContextoAvatar]: medidas de la usuaria y tablas de tallas.
 * Se actualiza solo al cambiar las medidas.
 */
@Composable
fun ProveedorAvatar(medidas: MeasurementsRepository, content: @Composable () -> Unit) {
    val m by remember(medidas) { medidas.observe() }.collectAsState(null as Measurements?)
    var tablas by remember { mutableStateOf<Map<String, TablaTallas>>(emptyMap()) }
    LaunchedEffect(Unit) { tablas = runCatching { CargadorTallas.cargarTodas() }.getOrDefault(emptyMap()) }
    val ctx = remember(m, tablas) { ContextoAvatar(PerfilCorporal.de(m), tablas) }
    CompositionLocalProvider(LocalAvatar provides ctx, content = content)
}
