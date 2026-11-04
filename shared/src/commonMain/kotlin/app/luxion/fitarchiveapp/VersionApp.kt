package app.luxion.fitarchiveapp

import androidx.compose.runtime.Composable

/** Versión de la app para mostrar en Perfil y citar en los informes de error, p. ej. `1.0.0 (1)`. */
@Composable
expect fun rememberVersionApp(): String
