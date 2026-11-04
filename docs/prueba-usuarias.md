# Prueba con usuarias reales (T50)

Preparación de la prueba de FitArchive con usuarias reales. La prueba en sí **está pendiente**: este documento y `docs/distribucion-prueba.md` dejan todo listo para hacerla. Resultados de la prueba de humo previa en `docs/bugs-v10.md`.

## 1. Objetivos
1. Comprobar que una persona que no ha visto la app **completa sola** los flujos clave (añadir prendas, ver el outfit del día, usar calendario, viajes y tallas) sin explicaciones.
2. Detectar **errores y fricciones** en móviles y versiones de Android distintos a los de desarrollo.
3. Medir si las **sugerencias** del outfit del día resultan útiles y creíbles.
4. Validar que los **permisos** (cámara, galería, ubicación) se entienden y no generan rechazo.
5. Comprobar que la **copia de seguridad** (exportar/importar) se entiende y no pierde datos.
6. Decidir con evidencia si se puede pasar a T51 (publicación) y qué hay que arreglar antes.

Fuera de alcance: rendimiento medido con instrumentos (ver `docs/rendimiento.md`), iOS (ver `docs/ios-pendiente.md`).

## 2. Perfil de las testers
- **4–6 usuarias**, mayores de edad, que usan Android con normalidad (no desarrolladoras).
- Mezcla deliberada: 2 con armario grande (> 60 prendas) y 2 con armario pequeño; al menos 1 que viaje a menudo; al menos 1 que compre ropa online con frecuencia (tallas); gama de móviles (baja, media, alta) y Android 8.0 o superior (minSdk 26).
- Que **no** hayan participado en el desarrollo ni hayan visto la app antes.
- Se les pide un mínimo de **7 días de uso** tras una sesión guiada inicial de ~30 min.
- Consentimiento verbal: saben que es una versión de prueba, qué datos se les piden (ver 6) y que pueden abandonar cuando quieran.

## 3. Formato de la prueba
1. **Sesión inicial (30 min, presencial o por videollamada)**: instalación (ver `distribucion-prueba.md`) y guion de tareas (sección 4). La persona facilitadora observa, **no ayuda** salvo bloqueo de más de 2 minutos, y lo anota.
2. **Uso libre 7 días**: usan la app con su ropa real; anotan errores con la plantilla (sección 6).
3. **Cierre (10 min)**: cuestionario (sección 7) y comentarios abiertos.

## 4. Guion de tareas
Se lee cada tarea en voz alta, sin indicar dónde pulsar. Pestañas de la barra inferior: **Hoy · Armario · Outfits · Calendario · Viajes · Perfil**. "Éxito" = lo logra sin ayuda; "Con ayuda" = tras una pista; "Fallo" = no lo logra.

| # | Tarea | Qué se espera | Se observa |
|---|---|---|---|
| 1 | **Añadir prendas.** Añade 3 prendas de tu armario: una con foto de la cámara y dos desde la galería. | Concede el permiso de cámara solo al usarla; el fondo se recorta; elige tipo, color, estación y uso; las prendas aparecen en Armario. | ¿Entiende el permiso? ¿Entiende los campos? ¿Qué hace si falla el recorte ("Usar original")? Tiempo por prenda. |
| 2 | **Filtrar y favoritas.** Encuentra solo tus camisetas de verano y marca una como favorita. | Usa filtros combinados y el like. | ¿Encuentra los filtros? |
| 3 | **Outfit del día.** Ve a Hoy y mira qué te sugiere. Prueba una ocasión y una alternativa. Acepta uno para hoy. | Ve el outfit en el avatar, el % y el motivo; cambia con "Siguiente"/"Regenerar"; "Aceptar para hoy" lo asigna. | ¿Entiende el %? ¿El motivo es creíble? ¿Concede ubicación o elige ciudad? Sin ubicación, ¿entiende el aviso? |
| 4 | **Crear un outfit a mano.** En Outfits crea uno arrastrando prendas al avatar, ponle nombre y guárdalo. Compártelo como imagen. | Outfit en la lista; hoja de compartir del sistema con vista previa. | ¿Entiende el arrastre? ¿Encuentra compartir? |
| 5 | **Calendario.** Mira qué te pusiste hoy; asigna un outfit a un día pasado y añade una nota. | Outfit en la casilla del día; aparece en el historial de usos de la prenda. | ¿Navega entre meses? ¿Entiende el historial? |
| 6 | **Viaje.** Crea un viaje de 4 días a un destino, añade un outfit y una prenda suelta a la maleta y marca casillas. | Maleta con casillas; aviso si hay prendas sucias. | ¿Entiende las fechas y la maleta? |
| 7 | **Limpia/sucia.** Marca una prenda como sucia y cambia cada cuántos usos va a la lavadora. | Insignia de sucias en Armario; la prenda sucia no se sugiere en Hoy. | ¿Lo encuentra? ¿Espera algo distinto? |
| 8 | **Tallas.** En Perfil, rellena tus medidas. En una prenda con marca (p. ej. Zara) indica la talla y mira el ajuste. | Muestra ajustada/bien/holgada y la talla ideal. | ¿Se fía del resultado? ¿Coincide con su experiencia real? |
| 9 | **Copia de seguridad.** Exporta tu armario a un archivo, desinstala/borra datos, instala de nuevo e importa el archivo. | Todo vuelve igual (prendas, fotos, outfits, calendario, viajes, medidas). **Ver la nota de la sección 8: puede no estar disponible en la build.** | ¿Encuentra dónde? ¿Entiende que importar reemplaza todo? ¿Falta algo tras importar? |
| 10 | **Permisos.** Niega la cámara y la ubicación (ajustes del móvil) y vuelve a usar la app. | La app sigue funcionando: galería en vez de cámara, ciudad en vez de ubicación. | ¿Se queda bloqueada? ¿El mensaje explica qué hacer? |
| 11 | **Sin red.** Con datos y wifi apagados, abre Hoy. | Aviso "No pudimos consultar el tiempo…" con "Reintentar"; el resto funciona. | ¿Entiende el aviso? |
| 12 | **Reinicio.** Cierra la app del todo y ábrela. | Los datos siguen (prendas, outfit aceptado hoy). | ¿Pierde algo? |

## 5. Métricas a observar
**Cuantitativas (la facilitadora cronometra/cuenta en la sesión)**
- Tasa de éxito por tarea (éxito / con ayuda / fallo).
- Tiempo para añadir una prenda (objetivo: < 1 min con foto de galería).
- Nº de errores o cierres inesperados por persona.
- Nº de prendas añadidas en 7 días y días con uso (indica si la app se vuelve hábito).
- Nº de veces que acepta / regenera / cambia de alternativa en Hoy (proxy de utilidad de la sugerencia).
- Permisos concedidos vs. denegados (cámara, ubicación).

**Cualitativas**
- Momentos de duda, frases textuales, gestos de frustración.
- Funciones que esperaba y no encuentra.
- Textos que no entiende (p. ej. el %, "afinidad", "Tu estilo").

**Criterios de paso a T51** (propuesta): 0 cierres inesperados sin explicar, 0 pérdidas de datos, ≥ 80 % de éxito en tareas 1, 3 y 4, y satisfacción media ≥ 4/5 en el cuestionario.

> La app **no tiene analítica** (ver política de privacidad), así que todo se recoge a mano en la sesión y con la plantilla de la sección 6. No instalar herramientas de medición en la build de prueba.

## 6. Registro de errores y observaciones
Cada tester (o la facilitadora en su nombre) rellena una entrada por incidencia. Se vuelcan en `docs/bugs-v10.md` con el formato de `docs/bugs-v05.md`:

- Los **bugs** llevan numeración `B<n>` continuando la última de `bugs-v*.md`, descripción de causa y arreglo (o "pendiente").
- Las **observaciones** (no son fallos) llevan `O<n>`.

### Plantilla de incidencia (copiar y pegar)
```
Tester: T1..Tn (sin nombre real)
Fecha / hora:
Móvil y versión de Android:
Versión de la app (nombre del archivo APK):
Tipo: Bug | Observación | Sugerencia
Gravedad: Bloquea (no puedo seguir) | Molesta (hay rodeo) | Menor (estético/texto)
Pantalla:
Qué intentaba hacer:
Qué pasó:
Qué esperaba:
Pasos para repetirlo:
¿Pasa siempre o a veces?:
Captura de pantalla (sin datos personales): sí/no
```
Formato de la entrada ya consolidada en `bugs-v10.md`:
```
- **B15. <Título corto>.** <Qué pasaba y por qué>. <Cómo se corrigió o "Pendiente">. Test `<nombre>`; flujo `<maestro>`.
```

## 7. Cuestionario breve (al cierre)
Escala 1 (nada) a 5 (mucho) salvo indicación.
1. Lo fácil que me ha resultado empezar a usar la app. (1–5)
2. Lo fácil que ha sido añadir mis prendas. (1–5)
3. Las sugerencias del "outfit del día" me han parecido acertadas. (1–5)
4. Me he puesto algún outfit sugerido o montado con la app. (Sí / No / Casi)
5. Entendí qué significa el porcentaje y el motivo de cada sugerencia. (1–5)
6. Me fío de la talla que dice la app. (1–5 / No la he probado)
7. Entendí por qué la app pide cámara y ubicación y me sentí cómoda concediéndolos. (1–5)
8. Me fío de que mis fotos y datos se quedan en mi móvil. (1–5)
9. La app ha sido rápida y fluida en mi móvil. (1–5)
10. Hubo un momento en que me perdí o no supe qué hacer. ¿Cuándo? (abierta)
11. ¿Qué es lo que más te ha gustado? (abierta)
12. ¿Qué cambiarías o qué echas en falta? (abierta)
13. ¿La seguirías usando? ¿La recomendarías a una amiga? (Sí / Quizá / No, y por qué)

## 8. Estado y bloqueos conocidos antes de la prueba
Ver `docs/bugs-v10.md`. Resumen: B15 (sin interfaz de exportar/importar copia) está resuelto: Perfil tiene "Copia de seguridad" y muestra la versión de la app, así que la tarea 9 del guion ya se puede hacer. Ver ese documento para el detalle.
