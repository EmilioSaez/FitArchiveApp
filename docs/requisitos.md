# FitArchive: requisitos

App móvil gratuita (Android + iPhone) para mujeres: convierte su armario en un armario digital y le propone outfits con la ropa que tiene. Todo se guarda en el móvil, sin cuenta, con opción de exportar copia.

## Decisiones
| Aspecto | Decisión |
|---|---|
| Plataforma | Android y iPhone (Kotlin Multiplatform + Compose Multiplatform) |
| Cuenta | Sin registro, todo local |
| Negocio | Gratis, sin monetizar |
| Estética | Revista de moda: blanco, serif editorial para títulos, burdeos como acento, fotos grandes |
| Pantalla de carga | Animación de las puertas de un armario abriéndose |
| Plazo | Sin prisa, largo plazo |
| Nombre | FitArchive |

## Armario
- Foto de cada prenda con fondo quitado automáticamente; eliminar prendas.
- Pestañas por tipo (camisetas, pantalones, vestidos, calzado, accesorios…), color y estación (invierno, verano, entretiempo).
- Clasificación por uso: trabajo, salir, casual…
- Filtro que combina criterios.
- Like en favoritas; marcar limpia/sucia, o indicar cada cuántos usos va a la lavadora y pasa a sucia sola.

## Outfits
- Montaje a mano arrastrando prendas sobre un avatar 2D (lienzo tipo collage).
- Sugerencias automáticas según tiempo, ocasión (trabajo, cita, fiesta, casual, deporte), colores que combinan y estilo aprendido (likes y outfits guardados).
- Solo prendas limpias.

## Avatar 2D y tallas (sin IA ni APIs de pago)
- Avatar ilustrado con prendas superpuestas por capas.
- La usuaria guarda sus medidas una vez (altura, pecho, cintura, cadera, hombros, brazos, piernas…).
- Al registrar la prenda indica marca, tipo y talla; la app busca esa talla en la tabla de la marca, compara y muestra etiqueta (ajustada/bien/holgada) y talla ideal.
- Marcas: Inditex (Zara, Pull&Bear, Bershka, Stradivarius, Massimo Dutti), Mango, H&M, Brownie, Scalpers, Hollister, Brandy Melville. Otras: tabla genérica europea.
- Tablas pasadas a mano por marca y tipo de prenda; revisarlas de vez en cuando.
- Etiqueta de corte (slim/normal/oversize) para corregir la predicción.

## Extras
- Calendario de looks (qué se llevó cada día).
- Maleta de viaje: outfits y lista de ropa.
- Compartir outfit como imagen (WhatsApp / stories); sin perfiles ni comunidad.
- Exportar/importar copia del armario en un archivo.
- Tiempo: ubicación del móvil o ciudad elegida si no da permiso.

## Descartado / futuro
Descartado: catálogo de tiendas e IA de prueba virtual. Opcional más adelante: importar prendas desde tiendas online, prueba virtual con IA.

## Diseño
Pantallas en Claude Design ("Armario inteligente: pantallas", 13 pantallas): carga, armario, filtro, añadir prenda, detalle, outfit del día, editor con avatar, mis outfits, compartir, calendario, maleta, medidas, ajustes. Navegación inferior: Hoy, Armario, Outfits, Calendario, Perfil.

## Roadmap
| Versión | Objetivo | Incluye |
|---|---|---|
| 0.1 Armario básico | Ropa digitalizada | Añadir con foto y fondo quitado, eliminar, pestañas tipo/color/estación, likes, filtro, pantalla de carga, guardado local |
| 0.2 Outfits | Montar y ver looks | Avatar 2D, montar a mano, guardar, compartir imagen |
| 0.3 Tallas | Saber cómo queda | Medidas, tablas por marca, genérica, etiqueta de ajuste, talla ideal |
| 0.4 Organización | Planificar | Calendario, limpia/sucia, clasificación por uso, maleta |
| 0.5 Sugerencias | Propuestas automáticas | Ocasión, colores, tiempo, aprendizaje de estilo |
| 1.0 Lanzamiento | Publicar | Copia, estética pulida, icono/fichas, Google Play y App Store |
