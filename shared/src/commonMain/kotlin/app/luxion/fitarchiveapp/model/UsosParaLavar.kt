package app.luxion.fitarchiveapp.model

/** Valor de [Garment.washAfterWears] que desactiva la sucia automática: la prenda solo se marca a mano. */
const val SOLO_MANUAL = 0

/** Usos tras los que una prenda de este tipo pasa a sucia por defecto (T35); null = solo manual. */
fun GarmentType.usosParaLavarPorDefecto(): Int? = when (this) {
    GarmentType.TSHIRT, GarmentType.SHIRT, GarmentType.DRESS, GarmentType.SHORTS -> 1
    GarmentType.SKIRT, GarmentType.JUMPSUIT -> 2
    GarmentType.TROUSERS, GarmentType.JEANS -> 3
    GarmentType.SWEATER -> 4
    GarmentType.JACKET -> 8
    GarmentType.COAT -> 10
    GarmentType.SHOES, GarmentType.BAG, GarmentType.ACCESSORY -> null
}

/**
 * Usos desde el último lavado tras los que esta prenda pasa a sucia; null = solo a mano.
 * El valor propio de la prenda manda; si no lo tiene (null), vale el del tipo; [SOLO_MANUAL] lo desactiva.
 */
fun Garment.usosParaLavar(): Int? = when (val propio = washAfterWears) {
    null -> type.usosParaLavarPorDefecto()
    else -> propio.takeIf { it > 0 }
}

/** Si, tras sumar un uso más, la prenda debe pasar a sucia. */
fun Garment.sucioTrasUso(): Boolean {
    val limite = usosParaLavar()
    return dirty || (limite != null && wearsSinceWash + 1 >= limite)
}
