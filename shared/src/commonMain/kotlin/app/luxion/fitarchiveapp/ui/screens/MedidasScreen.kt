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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.data.MedidasRepository
import app.luxion.fitarchiveapp.model.Measurements

private enum class Campo(val etiqueta: String, val ayuda: String, val min: Float, val max: Float) {
    Altura("Altura", "De pie y descalza, de espaldas a la pared: mide del suelo a la coronilla.", 100f, 230f),
    Pecho("Pecho", "Pasa la cinta por la parte más ancha del pecho, bajo las axilas, sin apretar.", 50f, 200f),
    Cintura("Cintura", "Mide la parte más estrecha del torso, a la altura del ombligo, sin meter tripa.", 40f, 200f),
    Cadera("Cadera", "Rodea la parte más ancha de caderas y glúteos, con los pies juntos.", 50f, 220f),
    Hombros("Hombros", "Mide de un extremo de los hombros al otro, por la espalda, de hueso a hueso.", 25f, 80f),
    Brazos("Brazos", "Del hueso del hombro a la muñeca, con el brazo relajado y ligeramente doblado.", 40f, 100f),
    Piernas("Piernas", "Del interior de la entrepierna al suelo, de pie y descalza (largo de pierna).", 50f, 130f),
}

/** Texto -> cm. Vacío = sin dato (null). Acepta coma decimal. */
private sealed interface Lectura {
    data class Valor(val cm: Float?) : Lectura
    data object Invalido : Lectura
}

private fun leer(texto: String, campo: Campo): Lectura {
    val t = texto.trim().replace(',', '.')
    if (t.isEmpty()) return Lectura.Valor(null)
    val v = t.toFloatOrNull() ?: return Lectura.Invalido
    return if (v in campo.min..campo.max) Lectura.Valor(v) else Lectura.Invalido
}

private fun Float?.comoTexto(): String = when {
    this == null -> ""
    this == toInt().toFloat() -> toInt().toString()
    else -> toString()
}

@Composable
fun MedidasScreen(repositorio: MedidasRepository, modifier: Modifier = Modifier) {
    val inicial = remember { repositorio.cargar() }
    val textos = remember {
        mutableStateMapOf(
            Campo.Altura to inicial?.heightCm.comoTexto(),
            Campo.Pecho to inicial?.bustCm.comoTexto(),
            Campo.Cintura to inicial?.waistCm.comoTexto(),
            Campo.Cadera to inicial?.hipCm.comoTexto(),
            Campo.Hombros to inicial?.shouldersCm.comoTexto(),
            Campo.Brazos to inicial?.armCm.comoTexto(),
            Campo.Piernas to inicial?.legCm.comoTexto(),
        )
    }
    var guardado by remember { mutableStateOf(false) }
    val lecturas = Campo.entries.associateWith { leer(textos.getValue(it), it) }
    val hayErrores = lecturas.values.any { it is Lectura.Invalido }

    fun valor(c: Campo): Float? = (lecturas.getValue(c) as? Lectura.Valor)?.cm

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
                repositorio.guardar(
                    Measurements(
                        heightCm = valor(Campo.Altura),
                        bustCm = valor(Campo.Pecho),
                        waistCm = valor(Campo.Cintura),
                        hipCm = valor(Campo.Cadera),
                        shouldersCm = valor(Campo.Hombros),
                        armCm = valor(Campo.Brazos),
                        legCm = valor(Campo.Piernas),
                        updatedAt = 0L, // lo fija el repositorio al persistir
                    ),
                )
                guardado = true
            },
            enabled = !hayErrores,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Guardar medidas") }
        if (guardado) Text("Medidas guardadas.", color = MaterialTheme.colorScheme.primary)
    }
}
