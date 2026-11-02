package app.luxion.fitarchiveapp.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.core.util.Consumer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

@Composable
actual fun rememberProveedorUbicacion(): ProveedorUbicacionPermiso {
    val context = LocalContext.current
    var pendiente by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    var mostrarJustificacion by remember { mutableStateOf(false) }

    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        val cb = pendiente
        pendiente = null
        cb?.invoke(concedido)
    }

    if (mostrarJustificacion) {
        fun cerrar(concedido: Boolean) {
            mostrarJustificacion = false
            val cb = pendiente
            pendiente = null
            cb?.invoke(concedido)
        }
        AlertDialog(
            onDismissRequest = { cerrar(false) },
            text = { Text(TEXTO_PERMISO_UBICACION) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarJustificacion = false
                    lanzador.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }) { Text("Continuar") }
            },
            dismissButton = { TextButton(onClick = { cerrar(false) }) { Text("Ahora no") } },
        )
    }

    return remember(context, lanzador) {
        ProveedorUbicacionAndroid(context) { cb ->
            pendiente = cb
            mostrarJustificacion = true
        }
    }
}

private class ProveedorUbicacionAndroid(
    private val context: Context,
    private val pedir: ((Boolean) -> Unit) -> Unit,
) : ProveedorUbicacionPermiso {

    override fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun solicitarPermiso(alResultado: (Boolean) -> Unit) {
        if (tienePermiso()) alResultado(true) else pedir(alResultado)
    }

    override suspend fun ubicacionAproximada(): Coordenadas? {
        if (!tienePermiso()) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return try {
            val loc = ultimaConocida(lm) ?: actual(lm)
            loc?.let { Coordenadas(it.latitude, it.longitude) }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun ultimaConocida(lm: LocationManager): Location? =
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

    private suspend fun actual(lm: LocationManager): Location? {
        val proveedor = LocationManager.NETWORK_PROVIDER
        if (!LocationManagerCompat.isLocationEnabled(lm) || !lm.isProviderEnabled(proveedor)) return null
        return withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val senal = CancellationSignal()
                cont.invokeOnCancellation { senal.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    lm,
                    proveedor,
                    senal,
                    ContextCompat.getMainExecutor(context),
                    Consumer<Location?> { if (cont.isActive) cont.resume(it) },
                )
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}

@Composable
actual fun rememberAlmacenCiudad(): AlmacenCiudad {
    val context = LocalContext.current.applicationContext
    return remember(context) { AlmacenCiudadPreferencias(context) }
}

private class AlmacenCiudadPreferencias(context: Context) : AlmacenCiudad {
    private val prefs = context.getSharedPreferences("fitarchive_ajustes", Context.MODE_PRIVATE)

    override fun cargar(): Ciudad? = CiudadCodec.decodificar(prefs.getString(CLAVE, null))

    override fun guardar(ciudad: Ciudad?) {
        prefs.edit().apply {
            if (ciudad == null) remove(CLAVE) else putString(CLAVE, CiudadCodec.codificar(ciudad))
        }.apply()
    }

    private companion object {
        const val CLAVE = "ciudad_elegida"
    }
}
