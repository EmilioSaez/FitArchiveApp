package app.luxion.fitarchiveapp.backup

import app.luxion.fitarchiveapp.model.CalendarDay
import app.luxion.fitarchiveapp.model.WearRecord
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.Measurements
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import app.luxion.fitarchiveapp.model.Trip

data class TripOutfitLink(val tripId: Long, val outfitId: Long)

data class TripGarmentLink(val tripId: Long, val garmentId: Long, val packed: Boolean, val manual: Boolean = false)

/** Todos los datos de la usuaria, tal como están en la BD de T03. */
data class WardrobeSnapshot(
    val garments: List<Garment> = emptyList(),
    val outfits: List<Outfit> = emptyList(),
    val outfitGarments: List<OutfitGarment> = emptyList(),
    val measurements: Measurements? = null,
    val calendar: List<CalendarDay> = emptyList(),
    val trips: List<Trip> = emptyList(),
    val tripOutfits: List<TripOutfitLink> = emptyList(),
    val tripGarments: List<TripGarmentLink> = emptyList(),
    val wears: List<WearRecord> = emptyList(),
)

/**
 * Puerto hacia la BD. Lo implementa la capa de datos (T04) sobre SQLDelight;
 * esta tarea solo define el contrato para no duplicar su trabajo.
 */
interface WardrobeBackupStore {
    suspend fun readAll(): WardrobeSnapshot

    /**
     * Sustituye TODO el contenido por [snapshot], conservando los ids,
     * en una única transacción (si falla, no cambia nada).
     */
    suspend fun replaceAll(snapshot: WardrobeSnapshot)
}

/** Puerto hacia las fotos guardadas en el almacenamiento privado de la app (por plataforma). */
interface PhotoStore {
    /** Bytes de la foto, o null si el archivo ya no existe. */
    suspend fun read(path: String): ByteArray?

    /** Guarda la foto de la prenda [garmentId] y devuelve la ruta a guardar en `Garment.photoPath`. */
    suspend fun save(garmentId: Long, bytes: ByteArray): String

    suspend fun delete(path: String)
}
