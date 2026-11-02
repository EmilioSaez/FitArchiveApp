package app.luxion.fitarchiveapp.foto

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

@Composable
actual fun rememberAlmacenFotos(): AlmacenFotos = remember { AlmacenFotosIos() }

@OptIn(ExperimentalForeignApi::class)
private class AlmacenFotosIos : AlmacenFotos {
    override suspend fun guardar(foto: FotoCapturada): String {
        val base = NSFileManager.defaultManager
            .URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL
        val dir = base.URLByAppendingPathComponent("prendas")!!
        NSFileManager.defaultManager.createDirectoryAtURL(dir, true, null, null)
        val ruta = dir.URLByAppendingPathComponent("prenda_${NSUUID().UUIDString}.${extensionDeImagen(foto.bytes)}")!!.path!!
        val datos = foto.bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = foto.bytes.size.toULong()) }
        datos.writeToFile(ruta, atomically = true)
        return ruta
    }

    override suspend fun borrar(ruta: String) {
        NSFileManager.defaultManager.removeItemAtPath(ruta, null)
    }
}

actual fun decodificarImagen(bytes: ByteArray): ImageBitmap? =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
