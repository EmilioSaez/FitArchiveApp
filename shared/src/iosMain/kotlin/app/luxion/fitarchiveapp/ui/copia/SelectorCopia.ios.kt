package app.luxion.fitarchiveapp.ui.copia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun rememberSelectorCopia(): SelectorCopia = remember { SelectorCopiaIos() }

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class SelectorCopiaIos : SelectorCopia {
    // Los delegados son débiles en UIKit: hay que retenerlos mientras el selector está abierto.
    private var delegado: DelegadoDocumento? = null

    override fun guardar(nombre: String, bytes: ByteArray, alTerminar: (ResultadoSelector<Unit>) -> Unit) {
        // Se escribe un archivo temporal y el selector lo copia donde elija la usuaria.
        val ruta = NSTemporaryDirectory() + nombre
        val escrito = NSFileManager.defaultManager.createFileAtPath(ruta, contents = bytes.aNSData(), attributes = null)
        if (!escrito) return alTerminar(ResultadoSelector.Error)
        val d = DelegadoDocumento { urls ->
            delegado = null
            NSFileManager.defaultManager.removeItemAtPath(ruta, error = null)
            alTerminar(if (urls == null) ResultadoSelector.Cancelado else ResultadoSelector.Ok(Unit))
        }
        delegado = d
        val picker = UIDocumentPickerViewController(forExportingURLs = listOf(NSURL.fileURLWithPath(ruta)), asCopy = true)
        picker.delegate = d
        presentar(picker)
    }

    override fun abrir(alTerminar: (ResultadoSelector<ByteArray>) -> Unit) {
        val d = DelegadoDocumento { urls ->
            delegado = null
            val url = urls?.firstOrNull()
            val datos = url?.let { NSData.dataWithContentsOfURL(it) }
            alTerminar(
                when {
                    urls == null -> ResultadoSelector.Cancelado
                    datos == null -> ResultadoSelector.Error
                    else -> ResultadoSelector.Ok(datos.aBytes())
                },
            )
        }
        delegado = d
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeData), asCopy = true)
        picker.delegate = d
        presentar(picker)
    }

    private fun presentar(vc: UIViewController) {
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        top?.presentViewController(vc, animated = true, completion = null)
    }
}

/** [alTerminar] recibe las URLs elegidas, o null si se canceló. */
private class DelegadoDocumento(
    private val alTerminar: (List<NSURL>?) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        alTerminar(didPickDocumentsAtURLs.filterIsInstance<NSURL>())
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        alTerminar(null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.aBytes(): ByteArray = ByteArray(length.toInt()).also { destino ->
    if (destino.isNotEmpty()) destino.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.aNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
