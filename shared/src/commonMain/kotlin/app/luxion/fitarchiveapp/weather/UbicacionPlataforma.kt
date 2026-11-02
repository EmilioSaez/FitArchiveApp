package app.luxion.fitarchiveapp.weather

import androidx.compose.runtime.Composable

/** Proveedor de ubicación aproximada de la plataforma (Android: coarse; iOS: "when in use"). */
@Composable
expect fun rememberProveedorUbicacion(): ProveedorUbicacionPermiso

/** Almacén de la ciudad elegida de la plataforma. */
@Composable
expect fun rememberAlmacenCiudad(): AlmacenCiudad
