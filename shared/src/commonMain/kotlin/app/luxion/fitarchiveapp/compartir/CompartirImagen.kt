package app.luxion.fitarchiveapp.compartir

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Abre el share sheet del sistema con una imagen PNG. */
interface CompartirImagen {
    /** Guarda [png] como [nombreArchivo] en caché y abre el selector de apps (WhatsApp, stories...). */
    suspend fun compartirPng(png: ByteArray, nombreArchivo: String)
}

@Composable
expect fun rememberCompartirImagen(): CompartirImagen

/** Codifica el bitmap como PNG. */
expect fun ImageBitmap.aPng(): ByteArray
