package app.luxion.fitarchiveapp.avatar

/** Peinado del avatar. */
enum class EstiloPelo(val etiqueta: String) {
    LARGO("Largo"),
    MEDIO("Media melena"),
    CORTO("Corto"),
    RECOGIDO("Recogido"),
    SIN_PELO("Sin pelo"),
}

/**
 * Cómo se ve el avatar (independiente de las medidas). Colores como RGB `0xRRGGBB`.
 * Si [usarFoto] y hay [fotoRostro] (ruta de una foto guardada), la cara del avatar es esa foto.
 */
data class AspectoAvatar(
    val pielRgb: Int = PIEL_POR_DEFECTO,
    val peloRgb: Int = PELO_POR_DEFECTO,
    val estiloPelo: EstiloPelo = EstiloPelo.LARGO,
    val fotoRostro: String? = null,
    val usarFoto: Boolean = false,
) {
    val conFoto: Boolean get() = usarFoto && fotoRostro != null

    companion object {
        const val PIEL_POR_DEFECTO = 0xEBD8CB
        const val PELO_POR_DEFECTO = 0x5A4336

        /** Tonos de piel de claro a oscuro. */
        val PIELES = listOf(0xF8E5D8, 0xEBD8CB, 0xF0C9A8, 0xD9A67F, 0xB97D56, 0x8D5A3B, 0x6A4129, 0x4A2C1D)

        val PELOS = listOf(0x1E1A18, 0x5A4336, 0x8B5E3C, 0xB7824A, 0xE0C089, 0xB0361F, 0x8E8E92, 0xD9A3B8)
    }
}
