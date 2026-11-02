package app.luxion.fitarchiveapp.color

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

private const val LADO_MAXIMO = 256

actual fun decodificarImagen(bytes: ByteArray): ImageBitmap? = runCatching {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, medidas)
    var muestreo = 1
    while (maxOf(medidas.outWidth, medidas.outHeight) / (muestreo * 2) >= LADO_MAXIMO) muestreo *= 2
    val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)?.asImageBitmap()
}.getOrNull()
