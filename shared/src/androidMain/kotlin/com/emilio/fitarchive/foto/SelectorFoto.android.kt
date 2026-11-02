package com.emilio.fitarchive.foto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberSelectorFoto(onResultado: (FotoCapturada?) -> Unit): SelectorFoto {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResultado)
    var uriCamara by remember { mutableStateOf<Uri?>(null) }
    var mostrarJustificacion by remember { mutableStateOf(false) }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        callback(uri?.let { context.leerFoto(it) })
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = uriCamara
        callback(if (ok && uri != null) context.leerFoto(uri) else null)
        uriCamara = null
    }
    fun lanzarCamara() {
        val uri = context.nuevoUriFoto()
        uriCamara = uri
        camara.launch(uri)
    }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) lanzarCamara() else callback(null)
    }

    if (mostrarJustificacion) {
        AlertDialog(
            onDismissRequest = {
                mostrarJustificacion = false
                callback(null)
            },
            text = { Text(TEXTO_PERMISO_CAMARA) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarJustificacion = false
                    permiso.launch(Manifest.permission.CAMERA)
                }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarJustificacion = false
                    callback(null)
                }) { Text("Ahora no") }
            },
        )
    }

    return remember(galeria, camara, permiso) {
        object : SelectorFoto {
            override fun tomarFoto() {
                val concedido = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (concedido) lanzarCamara() else mostrarJustificacion = true
            }

            override fun elegirDeGaleria() {
                galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }
}

private fun Context.nuevoUriFoto(): Uri {
    val dir = File(cacheDir, "fotos").apply { mkdirs() }
    val archivo = File.createTempFile("prenda_", ".jpg", dir)
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", archivo)
}

private fun Context.leerFoto(uri: Uri): FotoCapturada? =
    runCatching { contentResolver.openInputStream(uri)?.use { FotoCapturada(it.readBytes()) } }.getOrNull()
