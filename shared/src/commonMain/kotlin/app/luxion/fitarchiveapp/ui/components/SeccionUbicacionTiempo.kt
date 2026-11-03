package app.luxion.fitarchiveapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.weather.Ciudad
import app.luxion.fitarchiveapp.weather.ProveedorUbicacionPermiso
import app.luxion.fitarchiveapp.weather.SeleccionCiudad
import app.luxion.fitarchiveapp.weather.etiqueta
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private sealed interface Busqueda {
    data object Inactiva : Busqueda
    data object Buscando : Busqueda
    data object Error : Busqueda
    data class Resultados(val ciudades: List<Ciudad>) : Busqueda
}

/**
 * Sección "Ubicación y tiempo": permiso de ubicación aproximada y elección manual de ciudad
 * (que queda guardada para la próxima vez). Reutilizable en cualquier pantalla.
 */
@Composable
fun SeccionUbicacionTiempo(
    seleccion: SeleccionCiudad,
    ubicacion: ProveedorUbicacionPermiso,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var ciudad by remember { mutableStateOf(seleccion.ciudadGuardada()) }
    var permiso by remember { mutableStateOf(ubicacion.tienePermiso()) }
    var texto by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf<Busqueda>(Busqueda.Inactiva) }
    var job by remember { mutableStateOf<Job?>(null) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloSeccion("Ubicación y tiempo")
        Text(
            "Usamos el tiempo de hoy para sugerirte outfits. Puedes usar la ubicación aproximada del móvil " +
                "o elegir una ciudad.",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (permiso) {
            Text("Ubicación del móvil: activada.", style = MaterialTheme.typography.bodyMedium)
        } else {
            OutlinedButton(
                onClick = { ubicacion.solicitarPermiso { permiso = it } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Usar mi ubicación aproximada") }
        }

        Text(
            ciudad?.let { "Ciudad elegida: ${it.etiqueta()}" } ?: "Sin ciudad elegida.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (permiso && ciudad != null) {
            Text(
                "Mientras la ubicación del móvil esté activa, tiene prioridad; la ciudad se usa si no está disponible.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar ciudad") },
            singleLine = true,
        )
        Button(
            onClick = {
                job?.cancel()
                busqueda = Busqueda.Buscando
                job = scope.launch {
                    busqueda = seleccion.buscar(texto).fold(
                        onSuccess = { Busqueda.Resultados(it) },
                        onFailure = { Busqueda.Error },
                    )
                }
            },
            enabled = texto.trim().length >= SeleccionCiudad.MIN_CARACTERES && busqueda != Busqueda.Buscando,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Buscar") }

        when (val b = busqueda) {
            Busqueda.Inactiva -> Unit
            Busqueda.Buscando -> Text("Buscando…", style = MaterialTheme.typography.bodyMedium)
            Busqueda.Error -> Text(
                "No se pudo buscar. Comprueba tu conexión e inténtalo de nuevo.",
                color = MaterialTheme.colorScheme.error,
            )
            is Busqueda.Resultados ->
                if (b.ciudades.isEmpty()) {
                    Text("No encontramos ninguna ciudad con ese nombre.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    b.ciudades.forEach { c ->
                        TextButton(
                            onClick = {
                                seleccion.elegir(c)
                                ciudad = c
                                busqueda = Busqueda.Inactiva
                                texto = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(c.etiqueta()) }
                    }
                }
        }

        if (ciudad != null) {
            TextButton(onClick = {
                seleccion.quitar()
                ciudad = null
            }) { Text("Quitar ciudad") }
        }
    }
}
