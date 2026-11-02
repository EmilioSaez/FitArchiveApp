package app.luxion.fitarchiveapp.backup

import kotlinx.serialization.Serializable

/**
 * Contenido JSON del archivo de copia (ver [BackupCodec] para el contenedor).
 *
 * Es independiente de los modelos de dominio a propósito: el formato en disco
 * solo cambia al subir [FORMAT_VERSION], aunque el dominio evolucione.
 * Los enums se guardan por nombre. Los ids se conservan para mantener las referencias.
 */
@Serializable
data class BackupManifest(
    val formatVersion: Int,
    val app: String = APP_NAME,
    val createdAt: Long,
    val garments: List<GarmentDto> = emptyList(),
    val outfits: List<OutfitDto> = emptyList(),
    val outfitGarments: List<OutfitGarmentDto> = emptyList(),
    val measurements: MeasurementsDto? = null,
    val calendar: List<CalendarDayDto> = emptyList(),
    val trips: List<TripDto> = emptyList(),
    val tripOutfits: List<TripOutfitDto> = emptyList(),
    val tripGarments: List<TripGarmentDto> = emptyList(),
    /** Fotos, en el mismo orden en que aparecen sus bytes tras el manifiesto. */
    val photos: List<PhotoEntryDto> = emptyList(),
) {
    companion object {
        const val APP_NAME = "FitArchive"

        /** Versión actual. Subir al cambiar el formato de forma incompatible. */
        const val FORMAT_VERSION = 1
    }
}

@Serializable
data class GarmentDto(
    val id: Long,
    val type: String,
    val color: String,
    val season: String,
    val usage: String,
    val brand: String? = null,
    val size: String? = null,
    val favorite: Boolean = false,
    val dirty: Boolean = false,
    val wearsSinceWash: Int = 0,
    val totalWears: Int = 0,
    val washAfterWears: Int? = null,
    val createdAt: Long,
)

@Serializable
data class OutfitDto(
    val id: Long,
    val name: String,
    val occasion: String? = null,
    val favorite: Boolean = false,
    val createdAt: Long,
)

@Serializable
data class OutfitGarmentDto(
    val outfitId: Long,
    val garmentId: Long,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val zIndex: Int = 0,
)

@Serializable
data class MeasurementsDto(
    val heightCm: Float? = null,
    val bustCm: Float? = null,
    val waistCm: Float? = null,
    val hipCm: Float? = null,
    val shouldersCm: Float? = null,
    val armCm: Float? = null,
    val legCm: Float? = null,
    val updatedAt: Long,
    /** Ausente en copias anteriores a T22c. */
    val footCm: Float? = null,
)

@Serializable
data class CalendarDayDto(
    val date: String,
    val outfitId: Long? = null,
    val note: String? = null,
)

@Serializable
data class TripDto(
    val id: Long,
    val name: String,
    val destination: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val createdAt: Long,
)

@Serializable
data class TripOutfitDto(val tripId: Long, val outfitId: Long)

@Serializable
data class TripGarmentDto(val tripId: Long, val garmentId: Long, val packed: Boolean = false)

/** Foto de la prenda [garmentId]; [size] es su longitud en bytes. */
@Serializable
data class PhotoEntryDto(val garmentId: Long, val size: Int)
