package app.luxion.fitarchiveapp.ia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@Composable
actual fun rememberAlmacenIa(): AlmacenIa = remember { AlmacenIa(BovedaClaveKeychain(), AlmacenAjustesIaDefaults()) }

private class AlmacenAjustesIaDefaults : AlmacenAjustesIa {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun cargar() = AjustesIaCodec.decodificar(defaults.stringForKey(CLAVE))

    override fun guardar(ajustes: AjustesIa) {
        defaults.setObject(AjustesIaCodec.codificar(ajustes), forKey = CLAVE)
    }

    private companion object {
        const val CLAVE = "ia_ajustes"
    }
}

/**
 * Guarda la clave en el Keychain (`kSecClassGenericPassword`). `...ThisDeviceOnly` impide que viaje en copias de
 * seguridad o a otro dispositivo, y solo se puede leer con el móvil desbloqueado.
 *
 * Pendiente de comprobar en un iPhone real (T56): en Windows solo se puede verificar al compilar en CI de macOS.
 */
@OptIn(ExperimentalForeignApi::class)
internal class BovedaClaveKeychain : BovedaClaveIa {

    override fun leer(): String? = memScoped {
        val consulta = consulta(kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne)
        val resultado = alloc<CFTypeRefVar>()
        val estado = SecItemCopyMatching(consulta, resultado.ptr)
        CFRelease(consulta)
        if (estado != errSecSuccess) return null
        val datos = CFBridgingRelease(resultado.value) as? NSData ?: return null
        NSString.create(data = datos, encoding = NSUTF8StringEncoding)?.toString()
    }

    override fun escribir(clave: String) {
        val datos = (clave as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            ?: throw ErrorIa.Almacen()
        borrar() // Evita el error de duplicado al reemplazar una clave existente.
        val retenido = CFBridgingRetain(datos)
        val consulta = consulta(
            kSecValueData to retenido,
            kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
        )
        val estado = SecItemAdd(consulta, null)
        CFRelease(consulta)
        CFRelease(retenido)
        if (estado != errSecSuccess) throw ErrorIa.Almacen()
    }

    override fun borrar() {
        val consulta = consulta()
        SecItemDelete(consulta)
        CFRelease(consulta)
    }

    /** Diccionario de Keychain con la clase, el servicio y la cuenta de la clave, más los [extra]. */
    private fun consulta(vararg extra: Pair<Any?, Any?>): CFMutableDictionaryRef? {
        val base = listOf<Pair<Any?, Any?>>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICIO,
            kSecAttrAccount to CUENTA,
        ) + extra
        val d = CFDictionaryCreateMutable(null, base.size.convert(), kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        base.forEach { (k, v) -> CFDictionaryAddValue(d, k.puenteCf(), v.puenteCf()) }
        return d
    }

    /** Los valores que ya son referencias CF (constantes `kSec…`) se usan tal cual; el resto se puentea. */
    private fun Any?.puenteCf() = when (this) {
        is String -> CFBridgingRetain(this)
        else -> this as? kotlinx.cinterop.CPointer<*> ?: CFBridgingRetain(this)
    }

    private companion object {
        const val SERVICIO = "app.luxion.fitarchiveapp.ia"
        const val CUENTA = "clave_api"
    }
}
