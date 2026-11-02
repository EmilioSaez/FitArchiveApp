// NO VALIDADO: escrito sin poder compilar ni ejecutar iOS (la maquina de desarrollo es Windows).
// Hay que compilar y probar en un Mac (iOS 17+) antes de darlo por bueno. Ver docs/tareas.md (T07).
package app.luxion.fitarchiveapp.fondo

import app.luxion.fitarchiveapp.foto.FotoCapturada
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreImage.CIContext
import platform.CoreImage.CIImage
import platform.CoreImage.createCGImage
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.Vision.VNGenerateForegroundInstanceMaskRequest
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNInstanceMaskObservation
import platform.posix.memcpy

actual fun crearQuitaFondo(): QuitaFondo = QuitaFondoVision()

/** Vision `VNGenerateForegroundInstanceMaskRequest` (iOS 17+), en el dispositivo. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class QuitaFondoVision : QuitaFondo {
    override suspend fun quitar(foto: FotoCapturada): ImagenSinFondo = withContext(Dispatchers.Default) {
        val ios17 = NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(
            cValue<NSOperatingSystemVersion> {
                majorVersion = 17
                minorVersion = 0
                patchVersion = 0
            },
        )
        if (!ios17) {
            throw QuitaFondoException(
                QuitaFondoException.Motivo.NO_DISPONIBLE,
                "Quitar el fondo requiere iOS 17 o superior",
            )
        }
        val datos = foto.bytes.usePinned {
            NSData.create(bytes = it.addressOf(0), length = foto.bytes.size.toULong())
        }
        // El handler lee la orientacion EXIF del JPEG por si solo.
        val handler = VNImageRequestHandler(data = datos, options = emptyMap<Any?, Any?>())
        val peticion = VNGenerateForegroundInstanceMaskRequest()

        val pixeles = memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            if (!handler.performRequests(listOf(peticion), error.ptr)) {
                throw QuitaFondoException(
                    QuitaFondoException.Motivo.IMAGEN_INVALIDA,
                    error.value?.localizedDescription ?: "Vision no pudo procesar la imagen",
                )
            }
            val observacion = peticion.results?.firstOrNull() as? VNInstanceMaskObservation
            if (observacion == null || observacion.allInstances.count.toInt() == 0) {
                throw QuitaFondoException(QuitaFondoException.Motivo.SIN_SUJETO, "No se detecto ninguna prenda")
            }
            val error2 = alloc<ObjCObjectVar<NSError?>>()
            observacion.generateMaskedImageOfInstances(
                observacion.allInstances,
                fromRequestHandler = handler,
                croppedToInstancesExtent = true,
                error = error2.ptr,
            ) ?: throw QuitaFondoException(
                QuitaFondoException.Motivo.NO_DISPONIBLE,
                error2.value?.localizedDescription ?: "Vision no pudo generar el recorte",
            )
        }

        val ci = CIImage(cVPixelBuffer = pixeles)
        val cg = CIContext().createCGImage(ci, fromRect = ci.extent)
            ?: throw QuitaFondoException(QuitaFondoException.Motivo.NO_DISPONIBLE, "No se pudo renderizar el recorte")
        val imagen = UIImage.imageWithCGImage(cg)
        val png = UIImagePNGRepresentation(imagen)
            ?: throw QuitaFondoException(QuitaFondoException.Motivo.NO_DISPONIBLE, "No se pudo codificar el PNG")
        ImagenSinFondo(
            png.aBytes(),
            imagen.size.useContents { width.toInt() },
            imagen.size.useContents { height.toInt() },
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.aBytes(): ByteArray = ByteArray(length.toInt()).also { destino ->
    if (destino.isNotEmpty()) destino.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
}
