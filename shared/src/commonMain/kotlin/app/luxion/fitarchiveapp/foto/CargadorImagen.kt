package app.luxion.fitarchiveapp.foto

import androidx.compose.ui.graphics.ImageBitmap

/** Lee y decodifica la foto guardada en [ruta]; null si no existe o no se puede decodificar. */
expect suspend fun cargarImagen(ruta: String): ImageBitmap?
