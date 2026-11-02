package app.luxion.fitarchiveapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import app.luxion.fitarchiveapp.data.MeasurementsRepository

@Composable
fun MedidasScreen(repositorio: MeasurementsRepository, modifier: Modifier = Modifier) {
    val textos = remember { mutableStateMapOf<Campo, String>().apply { putAll(null.aTextos()) } }
    var cargado by remember { mutableStateOf(false) }
    LaunchedEffect(repositorio) {
        textos.putAll(repositorio.get().aTextos())
        cargado = true
    }
    val scope = rememberCoroutineScope()
    var guardado by remember { mutableStateOf(false) }
    val lecturas = Campo.entries.associateWith { leer(textos.getValue(it), it) }
    val hayErrores = lecturas.values.any { it is Lectura.Invalido }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Mis medidas", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            "Todas en centímetros. Usa una cinta métrica flexible, sin apretar. Puedes dejar vacío lo que no sepas.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Campo.entries.forEach { campo ->
            val invalido = lecturas.getValue(campo) is Lectura.Invalido
            OutlinedTextField(
                value = textos.getValue(campo),
                onValueChange = {
                    textos[campo] = it
                    guardado = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("${campo.etiqueta} (cm)") },
                singleLine = true,
                isError = invalido,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    Text(
                        if (invalido) "Introduce un valor entre ${campo.min.toInt()} y ${campo.max.toInt()} cm."
                        else campo.ayuda,
                    )
                },
            )
        }
        Button(
            onClick = {
                scope.launch {
                    repositorio.save(lecturas.aMedidas())
                    guardado = true
                }
            },
            enabled = cargado && !hayErrores,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Guardar medidas") }
        if (guardado) Text("Medidas guardadas.", color = MaterialTheme.colorScheme.primary)
    }
}
