package app.luxion.fitarchiveapp.weather

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyThreeKilometers
import platform.Foundation.NSError
import platform.Foundation.NSUserDefaults
import platform.darwin.NSObject
import kotlin.coroutines.resume

// NOTA: escrito sin poder compilar en iOS (desarrollo en Windows). Validar en macOS.

@Composable
actual fun rememberProveedorUbicacion(): ProveedorUbicacionPermiso = remember { ProveedorUbicacionIos() }

@OptIn(ExperimentalForeignApi::class)
private class ProveedorUbicacionIos : ProveedorUbicacionPermiso {
    private val manager = CLLocationManager().apply { desiredAccuracy = kCLLocationAccuracyThreeKilometers }
    private val delegado = DelegadoUbicacion()

    init {
        manager.delegate = delegado
    }

    override fun tienePermiso(): Boolean {
        val estado = manager.authorizationStatus
        return estado == kCLAuthorizationStatusAuthorizedWhenInUse || estado == kCLAuthorizationStatusAuthorizedAlways
    }

    override fun solicitarPermiso(alResultado: (Boolean) -> Unit) {
        if (manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) {
            alResultado(tienePermiso())
            return
        }
        delegado.alCambiarAutorizacion = {
            // El delegado también se avisa al crearse; solo cuenta cuando ya hay decisión.
            if (manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) {
                delegado.alCambiarAutorizacion = null
                alResultado(tienePermiso())
            }
        }
        manager.requestWhenInUseAuthorization()
    }

    override suspend fun ubicacionAproximada(): Coordenadas? {
        if (!tienePermiso()) return null
        return withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                delegado.alUbicacion = { coords ->
                    delegado.alUbicacion = null
                    if (cont.isActive) cont.resume(coords)
                }
                cont.invokeOnCancellation { delegado.alUbicacion = null }
                manager.requestLocation()
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}

@OptIn(ExperimentalForeignApi::class)
private class DelegadoUbicacion : NSObject(), CLLocationManagerDelegateProtocol {
    var alCambiarAutorizacion: (() -> Unit)? = null
    var alUbicacion: ((Coordenadas?) -> Unit)? = null

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        alCambiarAutorizacion?.invoke()
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val coords = (didUpdateLocations.lastOrNull() as? CLLocation)?.coordinate?.useContents {
            Coordenadas(latitude, longitude)
        }
        alUbicacion?.invoke(coords)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        alUbicacion?.invoke(null)
    }
}

@Composable
actual fun rememberAlmacenCiudad(): AlmacenCiudad = remember { AlmacenCiudadDefaults() }

private class AlmacenCiudadDefaults : AlmacenCiudad {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun cargar(): Ciudad? = CiudadCodec.decodificar(defaults.stringForKey(CLAVE))

    override fun guardar(ciudad: Ciudad?) {
        if (ciudad == null) defaults.removeObjectForKey(CLAVE)
        else defaults.setObject(CiudadCodec.codificar(ciudad), forKey = CLAVE)
    }

    private companion object {
        const val CLAVE = "ciudad_elegida"
    }
}
