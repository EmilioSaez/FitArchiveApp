package app.luxion.fitarchiveapp.ui.screens

import app.luxion.fitarchiveapp.model.Measurements

internal enum class Campo(val etiqueta: String, val ayuda: String, val min: Float, val max: Float) {
    Altura("Altura", "De pie y descalza, de espaldas a la pared: mide del suelo a la coronilla.", 100f, 230f),
    Pecho("Pecho", "Pasa la cinta por la parte más ancha del pecho, bajo las axilas, sin apretar.", 50f, 200f),
    Cintura("Cintura", "Mide la parte más estrecha del torso, a la altura del ombligo, sin meter tripa.", 40f, 200f),
    Cadera("Cadera", "Rodea la parte más ancha de caderas y glúteos, con los pies juntos.", 50f, 220f),
    Hombros("Hombros", "Mide de un extremo de los hombros al otro, por la espalda, de hueso a hueso.", 25f, 80f),
    Brazos("Brazos", "Del hueso del hombro a la muñeca, con el brazo relajado y ligeramente doblado.", 40f, 100f),
    Piernas("Piernas", "Del interior de la entrepierna al suelo, de pie y descalza (largo de pierna).", 50f, 130f),
}

/** Texto -> cm. Vacío = sin dato (null). Acepta coma decimal. */
internal sealed interface Lectura {
    data class Valor(val cm: Float?) : Lectura
    data object Invalido : Lectura
}

internal fun leer(texto: String, campo: Campo): Lectura {
    val t = texto.trim().replace(',', '.')
    if (t.isEmpty()) return Lectura.Valor(null)
    val v = t.toFloatOrNull() ?: return Lectura.Invalido
    return if (v in campo.min..campo.max) Lectura.Valor(v) else Lectura.Invalido
}

internal fun Float?.comoTexto(): String = when {
    this == null -> ""
    this == toInt().toFloat() -> toInt().toString()
    else -> toString()
}

/** Textos iniciales del formulario a partir de las medidas guardadas (vacíos si no hay). */
internal fun Measurements?.aTextos(): Map<Campo, String> = mapOf(
    Campo.Altura to this?.heightCm.comoTexto(),
    Campo.Pecho to this?.bustCm.comoTexto(),
    Campo.Cintura to this?.waistCm.comoTexto(),
    Campo.Cadera to this?.hipCm.comoTexto(),
    Campo.Hombros to this?.shouldersCm.comoTexto(),
    Campo.Brazos to this?.armCm.comoTexto(),
    Campo.Piernas to this?.legCm.comoTexto(),
)

/** Medidas a guardar a partir de las lecturas; los campos inválidos cuentan como vacíos. `updatedAt` lo fija el repositorio. */
internal fun Map<Campo, Lectura>.aMedidas(): Measurements {
    fun v(c: Campo): Float? = (this[c] as? Lectura.Valor)?.cm
    return Measurements(
        heightCm = v(Campo.Altura),
        bustCm = v(Campo.Pecho),
        waistCm = v(Campo.Cintura),
        hipCm = v(Campo.Cadera),
        shouldersCm = v(Campo.Hombros),
        armCm = v(Campo.Brazos),
        legCm = v(Campo.Piernas),
        updatedAt = 0L,
    )
}
