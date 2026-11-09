package app.luxion.fitarchiveapp.backup

import app.luxion.fitarchiveapp.avatar.AjustePrenda
import app.luxion.fitarchiveapp.avatar.Subtipo
import app.luxion.fitarchiveapp.model.CalendarDay
import app.luxion.fitarchiveapp.model.WearRecord
import app.luxion.fitarchiveapp.model.Fit
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Measurements
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Trip
import app.luxion.fitarchiveapp.model.Usage

/** Resultado de importar: [missingPhotos] = ids de prendas cuya foto no venía en la copia. */
data class RestoreResult(val garments: Int, val outfits: Int, val missingPhotos: List<Long>)

/**
 * Exporta e importa la copia completa. Los bytes los entrega/recibe la capa de UI
 * mediante el selector de documentos del sistema (`CreateDocument` / `OpenDocument`
 * en Android, `UIDocumentPicker` en iOS), sin permisos de almacenamiento.
 */
class BackupService(
    private val store: WardrobeBackupStore,
    private val photos: PhotoStore,
    private val now: () -> Long,
) {
    suspend fun export(): ByteArray {
        val data = store.readAll()
        val entries = mutableListOf<PhotoEntryDto>()
        val blobs = mutableListOf<ByteArray>()
        for (g in data.garments) {
            val bytes = photos.read(g.photoPath) ?: continue
            entries += PhotoEntryDto(g.id, bytes.size)
            blobs += bytes
        }
        return BackupCodec.encode(data.toManifest(now(), entries), blobs)
    }

    /**
     * Valida el archivo completo antes de tocar nada y REEMPLAZA los datos actuales
     * (la UI debe pedir confirmación). Si falla, no se pierde nada.
     */
    suspend fun import(bytes: ByteArray): RestoreResult {
        val content = BackupCodec.decode(bytes)
        val m = content.manifest
        validate(m)
        val base = m.toSnapshot()

        val previous = store.readAll().garments.map { it.photoPath }
        val blobs = m.photos.mapIndexed { i, p -> p.garmentId to content.photos[i] }.toMap()
        val written = mutableListOf<String>()
        try {
            val garments = base.garments.map { g ->
                val blob = blobs[g.id] ?: return@map g
                g.copy(photoPath = photos.save(g.id, blob).also { written += it })
            }
            store.replaceAll(base.copy(garments = garments))
        } catch (e: Throwable) {
            written.forEach { runCatching { photos.delete(it) } }
            throw e
        }
        previous.filter { it.isNotEmpty() && it !in written }.forEach { runCatching { photos.delete(it) } }
        return RestoreResult(
            garments = m.garments.size,
            outfits = m.outfits.size,
            missingPhotos = m.garments.map { it.id }.filter { it !in blobs },
        )
    }

    private fun validate(m: BackupManifest) {
        fun noDuplicates(ids: List<Any>, what: String) {
            if (ids.size != ids.toSet().size) throw BackupException.Corrupt("$what duplicados")
        }
        noDuplicates(m.garments.map { it.id }, "prendas")
        noDuplicates(m.outfits.map { it.id }, "outfits")
        noDuplicates(m.trips.map { it.id }, "viajes")
        noDuplicates(m.calendar.map { it.date }, "días")
        noDuplicates(m.photos.map { it.garmentId }, "fotos")
        val g = m.garments.map { it.id }.toSet()
        val o = m.outfits.map { it.id }.toSet()
        val t = m.trips.map { it.id }.toSet()
        fun refs(ok: Boolean, what: String) {
            if (!ok) throw BackupException.Corrupt("referencia rota en $what")
        }
        refs(m.outfitGarments.all { it.outfitId in o && it.garmentId in g }, "outfits")
        refs(m.calendar.all { it.outfitId == null || it.outfitId in o }, "calendario")
        refs(m.tripOutfits.all { it.tripId in t && it.outfitId in o }, "viajes")
        refs(m.tripGarments.all { it.tripId in t && it.garmentId in g }, "viajes")
        noDuplicates(m.wears.map { it.date to it.garmentId }, "usos")
        refs(m.wears.all { it.garmentId in g }, "usos")
        refs(m.photos.all { it.garmentId in g }, "fotos")
    }
}

private fun WardrobeSnapshot.toManifest(createdAt: Long, photos: List<PhotoEntryDto>) = BackupManifest(
    formatVersion = BackupManifest.FORMAT_VERSION,
    createdAt = createdAt,
    garments = garments.map {
        GarmentDto(
            it.id, it.type.name, it.color.name, it.season.name, it.usage.name, it.brand, it.size, it.fit.name,
            it.favorite, it.dirty, it.wearsSinceWash, it.totalWears, it.washAfterWears, it.createdAt,
            it.subtipo?.name, it.ajuste?.dx, it.ajuste?.dy, it.ajuste?.escala,
        )
    },
    outfits = outfits.map { OutfitDto(it.id, it.name, it.occasion?.name, it.favorite, it.createdAt) },
    outfitGarments = outfitGarments.map { OutfitGarmentDto(it.outfitId, it.garmentId, it.x, it.y, it.scale, it.zIndex) },
    measurements = measurements?.let {
        MeasurementsDto(it.heightCm, it.bustCm, it.waistCm, it.hipCm, it.shouldersCm, it.armCm, it.legCm, it.updatedAt, it.footCm)
    },
    calendar = calendar.map { CalendarDayDto(it.date, it.outfitId, it.note) },
    trips = trips.map { TripDto(it.id, it.name, it.destination, it.startDate, it.endDate, it.createdAt) },
    tripOutfits = tripOutfits.map { TripOutfitDto(it.tripId, it.outfitId) },
    tripGarments = tripGarments.map { TripGarmentDto(it.tripId, it.garmentId, it.packed, it.manual) },
    wears = wears.map { WearDto(it.date, it.garmentId) },
    photos = photos,
)

private inline fun <reified E : Enum<E>> parse(value: String, what: String): E =
    enumValues<E>().firstOrNull { it.name == value } ?: throw BackupException.Corrupt("$what desconocido: $value")

/** Las prendas salen con `photoPath` vacío; se rellena al guardar su foto. */
private fun BackupManifest.toSnapshot() = WardrobeSnapshot(
    garments = garments.map {
        Garment(
            id = it.id,
            photoPath = "",
            type = parse<GarmentType>(it.type, "tipo"),
            color = parse<GarmentColor>(it.color, "color"),
            season = parse<Season>(it.season, "estación"),
            usage = parse<Usage>(it.usage, "uso"),
            brand = it.brand, size = it.size, fit = parse<Fit>(it.fit, "corte"), favorite = it.favorite, dirty = it.dirty,
            wearsSinceWash = it.wearsSinceWash, totalWears = it.totalWears,
            washAfterWears = it.washAfterWears, createdAt = it.createdAt,
            subtipo = it.subtipo?.let { s -> parse<Subtipo>(s, "subtipo") },
            ajuste = if (it.ajusteDx != null && it.ajusteDy != null && it.ajusteEscala != null) {
                AjustePrenda(it.ajusteDx, it.ajusteDy, it.ajusteEscala)
            } else {
                null
            },
        )
    },
    outfits = outfits.map { Outfit(it.id, it.name, it.occasion?.let { o -> parse<Occasion>(o, "ocasión") }, it.favorite, it.createdAt) },
    outfitGarments = outfitGarments.map { OutfitGarment(it.outfitId, it.garmentId, it.x, it.y, it.scale, it.zIndex) },
    measurements = measurements?.let { Measurements(it.heightCm, it.bustCm, it.waistCm, it.hipCm, it.shouldersCm, it.armCm, it.legCm, it.updatedAt, it.footCm) },
    calendar = calendar.map { CalendarDay(it.date, it.outfitId, it.note) },
    trips = trips.map { Trip(it.id, it.name, it.destination, it.startDate, it.endDate, it.createdAt) },
    tripOutfits = tripOutfits.map { TripOutfitLink(it.tripId, it.outfitId) },
    tripGarments = tripGarments.map { TripGarmentLink(it.tripId, it.garmentId, it.packed, it.manual) },
    wears = wears.map { WearRecord(it.date, it.garmentId) },
)
