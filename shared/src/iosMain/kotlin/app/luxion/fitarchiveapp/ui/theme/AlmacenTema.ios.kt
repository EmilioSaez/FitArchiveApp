package app.luxion.fitarchiveapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
actual fun rememberAlmacenTema(): AlmacenTema = remember { AlmacenTemaDefaults() }

private class AlmacenTemaDefaults : AlmacenTema {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun cargar() = AjustesTemaCodec.decodificar(defaults.stringForKey(CLAVE))

    override fun guardar(ajustes: AjustesTema) {
        defaults.setObject(AjustesTemaCodec.codificar(ajustes), forKey = CLAVE)
    }

    private companion object {
        const val CLAVE = "tema"
    }
}
