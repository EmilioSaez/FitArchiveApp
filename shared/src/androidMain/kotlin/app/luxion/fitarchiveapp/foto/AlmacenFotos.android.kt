package app.luxion.fitarchiveapp.foto

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
actual fun rememberAlmacenFotos(): AlmacenFotos {
    val context = LocalContext.current.applicationContext
    return remember(context) { AlmacenFotosAndroid(context) }
}

private class AlmacenFotosAndroid(private val context: Context) : AlmacenFotos {
    override suspend fun guardar(foto: FotoCapturada): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "prendas").apply { mkdirs() }
        val archivo = File(dir, "prenda_${UUID.randomUUID()}.jpg")
        archivo.writeBytes(foto.bytes)
        archivo.absolutePath
    }
}

actual fun decodificarImagen(bytes: ByteArray): ImageBitmap? =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
