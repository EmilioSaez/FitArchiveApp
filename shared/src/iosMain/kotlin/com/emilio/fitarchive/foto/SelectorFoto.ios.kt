package com.emilio.fitarchive.foto

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

@Composable
actual fun rememberSelectorFoto(onResultado: (FotoCapturada?) -> Unit): SelectorFoto {
    val callback = rememberUpdatedState(onResultado)
    return remember { SelectorFotoIos { callback.value(it) } }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class SelectorFotoIos(private val alResultado: (FotoCapturada?) -> Unit) : SelectorFoto {
    // Los delegados son débiles en UIKit: hay que retenerlos mientras el selector está abierto.
    private var delegadoCamara: DelegadoCamara? = null
    private var delegadoGaleria: DelegadoGaleria? = null

    override fun tomarFoto() {
        val camara = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        if (!UIImagePickerController.isSourceTypeAvailable(camara)) {
            alResultado(null)
            return
        }
        val delegado = DelegadoCamara {
            delegadoCamara = null
            alResultado(it)
        }
        delegadoCamara = delegado
        val picker = UIImagePickerController().apply {
            sourceType = camara
            this.delegate = delegado
        }
        presentar(picker)
    }

    override fun elegirDeGaleria() {
        val config = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter
            selectionLimit = 1
        }
        val delegado = DelegadoGaleria {
            delegadoGaleria = null
            alResultado(it)
        }
        delegadoGaleria = delegado
        val picker = PHPickerViewController(config).apply { this.delegate = delegado }
        presentar(picker)
    }

    private fun presentar(vc: UIViewController) {
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        top?.presentViewController(vc, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.aFoto(): FotoCapturada? =
    UIImageJPEGRepresentation(this, 0.9)?.let { FotoCapturada(it.aBytes()) }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.aBytes(): ByteArray = ByteArray(length.toInt()).also { destino ->
    if (destino.isNotEmpty()) destino.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
}

private class DelegadoCamara(
    private val alTerminar: (FotoCapturada?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val imagen = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, null)
        alTerminar(imagen?.aFoto())
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        alTerminar(null)
    }
}

private class DelegadoGaleria(
    private val alTerminar: (FotoCapturada?) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)
        val proveedor = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
        if (proveedor == null || !proveedor.canLoadObjectOfClass(UIImage)) {
            alTerminar(null)
            return
        }
        proveedor.loadObjectOfClass(UIImage) { objeto, _ ->
            val foto = (objeto as? UIImage)?.aFoto()
            dispatch_async(dispatch_get_main_queue()) { alTerminar(foto) }
        }
    }
}
