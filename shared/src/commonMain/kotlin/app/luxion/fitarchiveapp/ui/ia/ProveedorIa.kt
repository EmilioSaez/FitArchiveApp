package app.luxion.fitarchiveapp.ui.ia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.luxion.fitarchiveapp.ia.ClienteClaude
import app.luxion.fitarchiveapp.ia.ServicioIa
import app.luxion.fitarchiveapp.ia.rememberAlmacenIa

/** [ServicioIa] de la app: una sola instancia por composición raíz, que comparten todas las funciones de IA. */
@Composable
fun rememberServicioIa(): ServicioIa {
    val almacen = rememberAlmacenIa()
    return remember(almacen) { ServicioIa(ClienteClaude(), almacen) }
}
