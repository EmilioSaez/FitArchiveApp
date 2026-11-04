package app.luxion.fitarchiveapp.ui.theme

import androidx.compose.runtime.Composable

/**
 * `true` si el usuario pidió reducir el movimiento: portada ya abierta, perchas sin balanceo y maleta sin escalonado.
 * Android: la escala de duración de animaciones es 0. iOS: "Reducir movimiento" de Accesibilidad.
 */
@Composable
expect fun rememberReducirMovimiento(): Boolean
