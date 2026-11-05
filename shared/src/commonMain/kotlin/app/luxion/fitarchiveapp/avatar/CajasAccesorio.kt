package app.luxion.fitarchiveapp.avatar

/**
 * Caja de un accesorio adaptada al cuerpo. Las cajas base de [Subtipo] están pensadas para la cabeza del avatar
 * original (98 unidades de alto); la cabeza paramétrica mide [FACTOR_CABEZA] veces más.
 */
const val FACTOR_CABEZA = 1.28f

fun cajaAccesorio(subtipo: Subtipo, escala: EscalaAvatar): Caja {
    val c = subtipo.caja
    val coronilla = EscalaAvatar.CORONILLA.toFloat()
    fun cabeza() = Caja(
        200f + (c.x - 200f) * FACTOR_CABEZA,
        coronilla + (c.y - coronilla) * FACTOR_CABEZA,
        c.w * FACTOR_CABEZA,
        c.h * FACTOR_CABEZA,
    )
    val cuerpo = escala.cuerpo
    return when (subtipo) {
        Subtipo.ACC_SOMBRERO_GORRO, Subtipo.ACC_DIADEMA, Subtipo.ACC_GAFAS, Subtipo.ACC_PENDIENTES -> cabeza()
        Subtipo.ACC_COLLAR, Subtipo.ACC_PANUELO -> c.copy(y = escala.yCuello - escala.u(1.5))
        Subtipo.ACC_CINTURON -> {
            val ancho = escala.u(cuerpo.cinturaCm * EscalaAvatar.ANCHO_FRONTAL_CINTURA + 4.0)
            Caja(200f - ancho / 2f, escala.yCintura - c.h / 2f, ancho, c.h)
        }
        Subtipo.ACC_BOLSO_MANO_DER -> {
            val mano = 200f + escala.u(cuerpo.hombrosCm / 2 - 2.5 + cuerpo.brazoCm * 0.12)
            Caja(mano - c.w / 2f, escala.yHombros + escala.u(cuerpo.brazoCm * 0.9), c.w, c.h)
        }
        Subtipo.ACC_BOLSO_HOMBRO_IZQ -> {
            val hombro = 200f - escala.u(cuerpo.hombrosCm / 2 + 4.0)
            Caja(hombro - c.w / 2f, escala.yHombros, c.w, c.h)
        }
        else -> c
    }
}
