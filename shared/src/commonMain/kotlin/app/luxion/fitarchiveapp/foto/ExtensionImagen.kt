package app.luxion.fitarchiveapp.foto

/**
 * Extensión de archivo (sin punto) que corresponde al formato real de [bytes]:
 * "png" si empieza con la firma PNG; "jpg" en cualquier otro caso (JPEG o desconocido).
 * Las fotos ya guardadas con otra extensión se siguen leyendo igual: la lectura y la
 * decodificación no dependen del nombre del archivo.
 */
fun extensionDeImagen(bytes: ByteArray): String =
    if (bytes.size >= FIRMA_PNG.size && FIRMA_PNG.indices.all { bytes[it] == FIRMA_PNG[it] }) "png" else "jpg"

private val FIRMA_PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
