package app.luxion.fitarchiveapp.ia

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Archivos de preferencias de la IA; `res/xml` los excluye de las copias y transferencias del sistema. */
internal const val PREFS_IA_SECRETO = "fitarchive_ia_secreto"
internal const val PREFS_IA_AJUSTES = "fitarchive_ia_ajustes"

@Composable
actual fun rememberAlmacenIa(): AlmacenIa {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        AlmacenIa(BovedaClaveKeystore(context), AlmacenAjustesIaPreferencias(context))
    }
}

private class AlmacenAjustesIaPreferencias(context: Context) : AlmacenAjustesIa {
    private val prefs = context.getSharedPreferences(PREFS_IA_AJUSTES, Context.MODE_PRIVATE)

    override fun cargar() = AjustesIaCodec.decodificar(prefs.getString(CLAVE, null))

    override fun guardar(ajustes: AjustesIa) {
        prefs.edit().putString(CLAVE, AjustesIaCodec.codificar(ajustes)).apply()
    }

    private companion object {
        const val CLAVE = "ajustes"
    }
}

/**
 * Guarda la clave cifrada con AES-256-GCM; la llave vive en el AndroidKeyStore y no es exportable. En las
 * preferencias solo queda el bloque cifrado (IV + texto cifrado, en Base64). Si el Keystore falla, [escribir]
 * lanza [ErrorIa.Almacen] en lugar de guardar la clave sin cifrar, y [leer] devuelve null.
 */
internal class BovedaClaveKeystore(context: Context) : BovedaClaveIa {
    private val prefs = context.getSharedPreferences(PREFS_IA_SECRETO, Context.MODE_PRIVATE)

    override fun leer(): String? {
        val bloque = prefs.getString(CLAVE, null) ?: return null
        return try {
            val bytes = Base64.decode(bloque, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMACION)
            cipher.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(BITS_ETIQUETA, bytes, 0, LONGITUD_IV))
            String(cipher.doFinal(bytes, LONGITUD_IV, bytes.size - LONGITUD_IV), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    override fun escribir(clave: String) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMACION)
            cipher.init(Cipher.ENCRYPT_MODE, llave())
            val bloque = cipher.iv + cipher.doFinal(clave.toByteArray(Charsets.UTF_8))
            // commit() (síncrono): si no se puede persistir, que falle aquí y no en silencio.
            if (!prefs.edit().putString(CLAVE, Base64.encodeToString(bloque, Base64.NO_WRAP)).commit()) {
                throw ErrorIa.Almacen()
            }
        } catch (e: ErrorIa) {
            throw e
        } catch (e: Exception) {
            throw ErrorIa.Almacen(e)
        }
    }

    override fun borrar() {
        prefs.edit().remove(CLAVE).commit()
        runCatching { KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(ALIAS) }
    }

    private fun llave(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generador.generateKey()
    }

    private companion object {
        const val CLAVE = "clave_api"
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "fitarchive_ia_clave_api"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val LONGITUD_IV = 12
        const val BITS_ETIQUETA = 128
    }
}
