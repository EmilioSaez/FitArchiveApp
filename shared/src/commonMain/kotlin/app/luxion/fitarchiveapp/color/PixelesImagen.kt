package app.luxion.fitarchiveapp.color

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import app.luxion.fitarchiveapp.foto.FotoCapturada
import app.luxion.fitarchiveapp.model.GarmentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodifica una imagen (PNG/JPEG) reduciéndola si hace falta; null si los bytes no son una imagen. */
expect fun decodificarImagen(bytes: ByteArray): ImageBitmap?

/** T09: color principal de la prenda de [foto] (idealmente ya sin fondo); null si no se puede decodificar o no hay prenda visible. */
suspend fun detectarColorDeFoto(foto: FotoCapturada): GarmentColor? = withContext(Dispatchers.Default) {
    val imagen = decodificarImagen(foto.bytes) ?: return@withContext null
    val mapa = imagen.toPixelMap()
    val pixeles = IntArray(mapa.width * mapa.height)
    // PixelMap.buffer es ARGB sin premultiplicar.
    mapa.buffer.copyInto(pixeles)
    detectarColorPrincipal(pixeles)
}
