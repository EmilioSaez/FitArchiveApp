package app.luxion.fitarchiveapp.ui.copia

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberSelectorCopia(): SelectorCopia {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val selector = remember(context, scope) { SelectorCopiaAndroid(context, scope) }
    selector.lanzadorCrear = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
        selector::alCrear,
    )
    selector.lanzadorAbrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), selector::alAbrir)
    return selector
}

private class SelectorCopiaAndroid(
    private val context: Context,
    private val scope: CoroutineScope,
) : SelectorCopia {
    lateinit var lanzadorCrear: ActivityResultLauncher<String>
    lateinit var lanzadorAbrir: ActivityResultLauncher<Array<String>>
    private var bytesPendientes: ByteArray? = null
    private var alGuardar: ((ResultadoSelector<Unit>) -> Unit)? = null
    private var alLeer: ((ResultadoSelector<ByteArray>) -> Unit)? = null

    override fun guardar(nombre: String, bytes: ByteArray, alTerminar: (ResultadoSelector<Unit>) -> Unit) {
        bytesPendientes = bytes
        alGuardar = alTerminar
        lanzadorCrear.launch(nombre)
    }

    override fun abrir(alTerminar: (ResultadoSelector<ByteArray>) -> Unit) {
        alLeer = alTerminar
        // Sin filtro por tipo: el proveedor del archivo no siempre conoce la extensión .fitarchive.
        lanzadorAbrir.launch(arrayOf("*/*"))
    }

    fun alCrear(uri: Uri?) {
        val bytes = bytesPendientes
        val cb = alGuardar
        bytesPendientes = null
        alGuardar = null
        if (cb == null) return
        if (uri == null || bytes == null) return cb(ResultadoSelector.Cancelado)
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    // "wt": si el archivo ya existía, se trunca para no dejar bytes sobrantes.
                    val salida = context.contentResolver.openOutputStream(uri, "wt") ?: error("sin flujo de salida")
                    salida.use { it.write(bytes) }
                }.isSuccess
            }
            cb(if (ok) ResultadoSelector.Ok(Unit) else ResultadoSelector.Error)
        }
    }

    fun alAbrir(uri: Uri?) {
        val cb = alLeer
        alLeer = null
        if (cb == null) return
        if (uri == null) return cb(ResultadoSelector.Cancelado)
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            }
            cb(if (bytes != null) ResultadoSelector.Ok(bytes) else ResultadoSelector.Error)
        }
    }
}
