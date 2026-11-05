package app.luxion.fitarchiveapp.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun IconosBarrasSistema(oscuro: Boolean) {
    val vista = LocalView.current
    if (vista.isInEditMode) return
    SideEffect {
        val ventana = (vista.context as? Activity)?.window ?: return@SideEffect
        val controlador = WindowCompat.getInsetsController(ventana, vista)
        // Iconos claros sobre fondo oscuro y al revés.
        controlador.isAppearanceLightStatusBars = !oscuro
        controlador.isAppearanceLightNavigationBars = !oscuro
    }
}
