package app.luxion.fitarchiveapp.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberAlmacenTema(): AlmacenTema {
    val context = LocalContext.current.applicationContext
    return remember(context) { AlmacenTemaPreferencias(context) }
}

private class AlmacenTemaPreferencias(context: Context) : AlmacenTema {
    private val prefs = context.getSharedPreferences("fitarchive_ajustes", Context.MODE_PRIVATE)

    override fun cargar() = AjustesTemaCodec.decodificar(prefs.getString(CLAVE, null))

    override fun guardar(ajustes: AjustesTema) {
        prefs.edit().putString(CLAVE, AjustesTemaCodec.codificar(ajustes)).apply()
    }

    private companion object {
        const val CLAVE = "tema"
    }
}
