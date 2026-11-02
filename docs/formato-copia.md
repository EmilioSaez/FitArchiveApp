# Formato de la copia (`.fitarchive`)

Un solo archivo binario, creado y leído con el selector de documentos del sistema (`CreateDocument` / `OpenDocument`; sin permisos de almacenamiento). Código: `shared/.../backup/`.

| Bytes | Contenido |
|---|---|
| 0–7 | Magia `FITARCH` + `0x01` |
| 8–11 | Versión del formato (u32 big-endian) |
| 12–15 | Longitud N del manifiesto (u32 big-endian) |
| 16… | Manifiesto JSON UTF-8 (N bytes): prendas, outfits, posiciones, medidas, calendario, historial de usos, viajes y listas de maleta, e índice de fotos |
| tras el manifiesto | Bytes de cada foto, en el orden de `photos` (cada entrada indica `garmentId` y `size`) |

Reglas:
- **Versionado**: `FORMAT_VERSION = 1`. Una copia con versión mayor se rechaza (`UnsupportedVersion`). Los campos desconocidos se ignoran; un cambio incompatible sube la versión.
- Los ids se conservan para mantener las referencias; los enums se guardan por nombre.
- Importar **reemplaza** todo el armario actual (la UI debe pedir confirmación). Se valida el archivo entero antes de tocar nada; si falla, no cambia nada.
- Las rutas de foto no se guardan (dependen del dispositivo): al importar se regeneran. Una prenda sin foto en la copia queda con `photoPath` vacío y se informa en `RestoreResult.missingPhotos`.
- La BD y el almacenamiento de fotos se acceden por los puertos `WardrobeBackupStore` (a implementar en T04) y `PhotoStore` (por plataforma).
