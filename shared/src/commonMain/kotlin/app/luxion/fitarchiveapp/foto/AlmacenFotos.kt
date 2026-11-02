package app.luxion.fitarchiveapp.foto

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Guarda las fotos de las prendas en el almacenamiento privado de la app. */
interface AlmacenFotos {
    /** Escribe la imagen y devuelve la ruta que se guarda en `Garment.photoPath`. */
    suspend fun guardar(foto: FotoCapturada): String

    /** Borra el archivo de [ruta]; no falla si ya no existe. */
    suspend fun borrar(ruta: String)
}

@Composable
expect fun rememberAlmacenFotos(): AlmacenFotos

/** Decodifica una imagen codificada (JPEG/PNG) para mostrarla; null si no es válida. */
expect fun decodificarImagen(bytes: ByteArray): ImageBitmap?
