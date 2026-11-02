package app.luxion.fitarchiveapp.data

import app.luxion.fitarchiveapp.model.CalendarDay
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Measurements
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Trip
import app.luxion.fitarchiveapp.model.TripGarment
import app.luxion.fitarchiveapp.model.Usage

// Los enums se guardan como TEXT con su `name`; un valor desconocido en BD es un error de datos.
internal inline fun <reified E : Enum<E>> enumFromDb(value: String): E =
    enumValues<E>().firstOrNull { it.name == value }
        ?: error("Valor \"$value\" no válido para ${E::class.simpleName}")

internal fun Boolean.toDb(): Long = if (this) 1L else 0L

internal fun app.luxion.fitarchiveapp.db.Garment.toModel() = Garment(
    id = id,
    photoPath = photo_path,
    type = enumFromDb<GarmentType>(type),
    color = enumFromDb<GarmentColor>(color),
    season = enumFromDb<Season>(season),
    usage = enumFromDb<Usage>(usage),
    brand = brand,
    size = size,
    favorite = favorite == 1L,
    dirty = dirty == 1L,
    wearsSinceWash = wears_since_wash.toInt(),
    totalWears = total_wears.toInt(),
    washAfterWears = wash_after_wears?.toInt(),
    createdAt = created_at,
)

internal fun app.luxion.fitarchiveapp.db.Outfit.toModel() = Outfit(
    id = id,
    name = name,
    occasion = occasion?.let { enumFromDb<Occasion>(it) },
    favorite = favorite == 1L,
    createdAt = created_at,
)

internal fun app.luxion.fitarchiveapp.db.OutfitGarment.toModel() = OutfitGarment(
    outfitId = outfit_id,
    garmentId = garment_id,
    x = x.toFloat(),
    y = y.toFloat(),
    scale = scale.toFloat(),
    zIndex = z_index.toInt(),
)

internal fun app.luxion.fitarchiveapp.db.Measurements.toModel() = Measurements(
    heightCm = height_cm?.toFloat(),
    bustCm = bust_cm?.toFloat(),
    waistCm = waist_cm?.toFloat(),
    hipCm = hip_cm?.toFloat(),
    shouldersCm = shoulders_cm?.toFloat(),
    armCm = arm_cm?.toFloat(),
    legCm = leg_cm?.toFloat(),
    updatedAt = updated_at,
)

internal fun app.luxion.fitarchiveapp.db.CalendarDay.toModel() = CalendarDay(
    date = date,
    outfitId = outfit_id,
    note = note,
)

internal fun app.luxion.fitarchiveapp.db.Trip.toModel() = Trip(
    id = id,
    name = name,
    destination = destination,
    startDate = start_date,
    endDate = end_date,
    createdAt = created_at,
)

internal fun app.luxion.fitarchiveapp.db.TripGarment.toModel() = TripGarment(
    tripId = trip_id,
    garmentId = garment_id,
    packed = packed == 1L,
)
