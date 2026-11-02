package app.luxion.fitarchiveapp.model

data class Garment(
    val id: Long,
    val photoPath: String,
    val type: GarmentType,
    val color: GarmentColor,
    val season: Season,
    val usage: Usage,
    val brand: String?,
    val size: String?,
    val favorite: Boolean,
    val dirty: Boolean,
    val wearsSinceWash: Int,
    val totalWears: Int,
    /** Usos tras los que la prenda pasa sola a sucia; null = solo manual. */
    val washAfterWears: Int?,
    val createdAt: Long,
    /** Corte (T29): corrige la talla recomendada. */
    val fit: Fit = Fit.NORMAL,
)

data class Outfit(
    val id: Long,
    val name: String,
    val occasion: Occasion?,
    val favorite: Boolean,
    val createdAt: Long,
)

data class OutfitGarment(
    val outfitId: Long,
    val garmentId: Long,
    val x: Float,
    val y: Float,
    val scale: Float,
    val zIndex: Int,
)

/** Medidas en cm. */
data class Measurements(
    val heightCm: Float?,
    val bustCm: Float?,
    val waistCm: Float?,
    val hipCm: Float?,
    val shouldersCm: Float?,
    val armCm: Float?,
    val legCm: Float?,
    val updatedAt: Long,
)

/** [date] en ISO-8601 (yyyy-MM-dd). */
data class CalendarDay(
    val date: String,
    val outfitId: Long?,
    val note: String?,
)

data class Trip(
    val id: Long,
    val name: String,
    val destination: String?,
    val startDate: String?,
    val endDate: String?,
    val createdAt: Long,
)

/** Prenda en la lista de la maleta; [packed] indica que ya está guardada. */
data class TripGarment(
    val tripId: Long,
    val garmentId: Long,
    val packed: Boolean,
)
