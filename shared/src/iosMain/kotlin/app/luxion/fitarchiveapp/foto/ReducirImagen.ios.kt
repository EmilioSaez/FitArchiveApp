package app.luxion.fitarchiveapp.foto

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.posix.memcpy

// No se puede compilar ni probar en iPhone desde Windows (docs/ios-pendiente.md).
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun reducirParaIa(bytes: ByteArray): ByteArray? = withContext(Dispatchers.Default) {
    runCatching {
        val datos = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        val imagen = UIImage(data = datos)
        // `size` ya tiene la orientación aplicada y, al decodificar de datos, 1 punto = 1 píxel.
        val (ancho0, alto0) = imagen.size.useContents { width.toInt() to height.toInt() }
        if (ancho0 <= 0 || alto0 <= 0) return@runCatching null
        val (ancho, alto) = dimensionesReducidas(ancho0, alto0)

        val formato = UIGraphicsImageRendererFormat.defaultFormat().apply {
            scale = 1.0
            opaque = true // fondo opaco (blanco): JPEG no tiene transparencia.
        }
        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(ancho.toDouble(), alto.toDouble()), format = formato)
        // JPEG sin EXIF: el renderer genera los píxeles desde cero y dibujar aplica la orientación.
        val jpeg = renderer.JPEGDataWithCompressionQuality(CALIDAD_JPEG_IA / 100.0) { contexto ->
            platform.UIKit.UIColor.whiteColor.setFill()
            platform.UIKit.UIRectFill(CGRectMake(0.0, 0.0, ancho.toDouble(), alto.toDouble()))
            imagen.drawInRect(CGRectMake(0.0, 0.0, ancho.toDouble(), alto.toDouble()))
        }
        val resultado = ByteArray(jpeg.length.toInt())
        if (resultado.isNotEmpty()) resultado.usePinned { memcpy(it.addressOf(0), jpeg.bytes, jpeg.length) }
        resultado
    }.getOrNull()
}
