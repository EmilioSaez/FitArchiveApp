package app.luxion.fitarchiveapp.foto

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun cargarImagen(ruta: String, ladoMaximo: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, medidas)
        var muestreo = 1
        while (maxOf(medidas.outWidth, medidas.outHeight) / (muestreo * 2) >= ladoMaximo) muestreo *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        BitmapFactory.decodeFile(ruta, opciones)?.asImageBitmap()
    }.getOrNull()
}
