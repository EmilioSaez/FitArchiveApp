# IA en FitArchive

Opcional y con la **clave de API de Anthropic de cada usuaria** (como EasyFood). Sin servidor propio ni coste para el proyecto.

## Arquitectura (`shared/.../ia/`)
- `ClienteClaude`: HTTP directo con Ktor a `https://api.anthropic.com` (`POST /v1/messages`, `GET /v1/models/{id}`), cabeceras `x-api-key` y `anthropic-version`. Sin plugin de logging a propósito (la clave no debe llegar a registros). Reintenta 429, 5xx y fallos de red hasta 2 veces; no reintenta 400/401/402/403/404.
- `ServicioIa`: **único punto de entrada**. Antes de cada llamada exige IA activada, clave, aviso aceptado y tope diario sin alcanzar. API para las funciones: `preguntar`, `preguntarTexto`, `preguntarJson` (salida estructurada con esquema). Nada más llama a `ClienteClaude`.
- `ErrorIa`: errores en español, sin la clave. Un 401 borra la clave y desactiva la IA.
- `AlmacenIa`: `BovedaClaveIa` (clave) y `AlmacenAjustesIa` (ajustes no sensibles: activada, modelo, avisos aceptados, tope, contadores).
- `AvisoIa`: aviso de privacidad. Cada función que envíe datos distintos declara su propio `AvisoIa` (otro `id`) y se acepta aparte.
- UI: `ui/ia/SeccionIa.kt` en Perfil (clave, activar, tope, probar conexión, borrar) y `rememberServicioIa()`.

## Almacén de la clave
- Android: AES-256-GCM con llave no exportable en AndroidKeyStore; en SharedPreferences (`fitarchive_ia_secreto`) solo el bloque cifrado. Si el Keystore falla, no se guarda (no hay degradación a texto plano). Excluido de copias con `data_extraction_rules.xml`, `backup_rules.xml` y `allowBackup="false"`.
- iOS: Keychain (`kSecClassGenericPassword`, `WhenUnlockedThisDeviceOnly`). **Pendiente de probar en iPhone real (T56)**.
- Nunca en SQLDelight, exportar/importar copia ni logs.

## Modelo y parámetros
Por defecto `claude-haiku-5-5`, `output_config.effort = "low"`; no se envían `temperature`/`top_p`/`top_k`. **Aún no se ha hecho ninguna llamada real con clave**: hay que verificarlo a mano con una clave de prueba (los tests usan MockEngine).

## Cómo añadir una función de IA
1. Recibir `ServicioIa` por parámetro (se crea en `App.kt`).
2. Comprobar `servicio.bloqueo(aviso)` para deshabilitar botones y pedir el aviso si falta.
3. Llamar a `preguntarJson`/`preguntarTexto` con su `AvisoIa`; tratar el texto de la usuaria como dato, no como instrucciones.
4. Mostrar `ErrorIa.mensaje` si falla. Sin IA, la función debe degradar al comportamiento local.

## Funciones
- **Rellenar con IA** (`ui/anadir/CatalogarConIa.kt`, botón en el formulario de alta): envía la foto y rellena tipo, subtipo, color, estación y uso. `AvisoIa` propio (`catalogar1`). La foto se reduce a lado mayor 1024 px, JPEG calidad 80 y sin EXIF con `reducirParaIa` (expect/actual en `foto/`), solo en memoria. Respuesta con esquema JSON ligado a `GarmentType`, `Subtipo`, `GarmentColor`, `Season` y `Usage`; los valores desconocidos se ignoran y el subtipo solo se acepta si vale para el tipo. Sin clave o con IA desactivada el botón sale deshabilitado con la pista. **`reducirParaIa` en iOS no está probado (sin Mac); en Android tampoco con foto real.**
