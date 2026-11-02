package app.luxion.fitarchiveapp.ui.detalle

import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.foto.AlmacenFotos
import app.luxion.fitarchiveapp.model.Garment

/**
 * Elimina la prenda (y, en cascada, sus apariciones en outfits y maletas) y después su foto.
 * Se borra primero la fila: si fallara el borrado del archivo solo queda un archivo huérfano,
 * nunca una prenda sin foto.
 */
suspend fun eliminarPrenda(garments: GarmentRepository, almacen: AlmacenFotos, prenda: Garment) {
    garments.delete(prenda.id)
    runCatching { almacen.borrar(prenda.photoPath) }
}
