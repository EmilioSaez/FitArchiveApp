package app.luxion.fitarchiveapp.compartir

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberCompartirImagen(): CompartirImagen = remember { CompartirImagenIos() }

actual fun ImageBitmap.aPng(): ByteArray =
    Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes
        ?: error("No se pudo codificar el PNG")

/** Comparte con `UIActivityViewController` sobre el controlador visible. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class CompartirImagenIos : CompartirImagen {
    override suspend fun compartirPng(png: ByteArray, nombreArchivo: String) {
        val ruta = NSTemporaryDirectory() + nombreArchivo
        val datos = png.usePinned { NSData.create(bytes = it.addressOf(0), length = png.size.toULong()) }
        check(datos.writeToFile(ruta, atomically = true)) { "No se pudo escribir $ruta" }

        val hoja = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(ruta)), applicationActivities = null)
        var arriba = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (arriba?.presentedViewController != null) arriba = arriba.presentedViewController
        checkNotNull(arriba) { "No hay controlador desde el que presentar" }
        // En iPad el share sheet es un popover y necesita un ancla.
        hoja.popoverPresentationController?.sourceView = arriba.view
        arriba.presentViewController(hoja, animated = true, completion = null)
    }
}
