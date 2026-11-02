package com.emilio.fitarchive.data

import com.emilio.fitarchive.model.CalendarDay
import com.emilio.fitarchive.model.Garment
import com.emilio.fitarchive.model.GarmentColor
import com.emilio.fitarchive.model.GarmentType
import com.emilio.fitarchive.model.Measurements
import com.emilio.fitarchive.model.Occasion
import com.emilio.fitarchive.model.Outfit
import com.emilio.fitarchive.model.OutfitGarment
import com.emilio.fitarchive.model.Season
import com.emilio.fitarchive.model.Trip
import com.emilio.fitarchive.model.TripGarment
import com.emilio.fitarchive.model.Usage

// Los enums se guardan como TEXT con su `name`; un valor desconocido en BD es un error de datos.
internal inline fun <reified E : Enum<E>> enumFromDb(value: String): E =
    enumValues<E>().firstOrNull { it.name == value }
        ?: error("Valor \"$value\" no válido para ${E::class.simpleName}")

internal fun Boolean.toDb(): Long = if (this) 1L else 0L

internal fun com.emilio.fitarchive.db.Garment.toModel() = Garment(
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

internal fun com.emilio.fitarchive.db.Outfit.toModel() = Outfit(
    id = id,
    name = name,
    occasion = occasion?.let { enumFromDb<Occasion>(it) },
    favorite = favorite == 1L,
    createdAt = created_at,
)

internal fun com.emilio.fitarchive.db.OutfitGarment.toModel() = OutfitGarment(
    outfitId = outfit_id,
    garmentId = garment_id,
    x = x.toFloat(),
    y = y.toFloat(),
    scale = scale.toFloat(),
    zIndex = z_index.toInt(),
)

internal fun com.emilio.fitarchive.db.Measurements.toModel() = Measurements(
    heightCm = height_cm?.toFloat(),
    bustCm = bust_cm?.toFloat(),
    waistCm = waist_cm?.toFloat(),
    hipCm = hip_cm?.toFloat(),
    shouldersCm = shoulders_cm?.toFloat(),
    armCm = arm_cm?.toFloat(),
    legCm = leg_cm?.toFloat(),
    updatedAt = updated_at,
)

internal fun com.emilio.fitarchive.db.CalendarDay.toModel() = CalendarDay(
    date = date,
    outfitId = outfit_id,
    note = note,
)

internal fun com.emilio.fitarchive.db.Trip.toModel() = Trip(
    id = id,
    name = name,
    destination = destination,
    startDate = start_date,
    endDate = end_date,
    createdAt = created_at,
)

internal fun com.emilio.fitarchive.db.TripGarment.toModel() = TripGarment(
    tripId = trip_id,
    garmentId = garment_id,
    packed = packed == 1L,
)
