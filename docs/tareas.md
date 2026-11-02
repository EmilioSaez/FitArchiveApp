# FitArchive: subtareas

Cada tarea = un worktree (`shogun new tNN-nombre`) y una sesión (`shogun open <id> --herdr`). Lo que no depende de nada pendiente puede ir en paralelo. Estado: `[ ]` pendiente, `[x]` hecho.

Oleadas: 1) base T01–T04 + contenido T23–T26 + avatar T16 (sin código). 2) v0.1 en paralelo. 3) resto según dependencias.

## Base
- [x] T01 Framework (Kotlin Multiplatform) y proyecto — Base — PR #1
- [x] T02 Navegación y tema visual base (colores, tipografía editorial) — UI — T01
- [x] T03 Modelo de datos y BD local: prenda, outfit, medidas, día de calendario, viaje — Datos — T01
- [ ] T04 Capa de acceso a datos con tests — Datos — T03

## v0.1 Armario básico
- [ ] T05 Pantalla de carga con puertas del armario — UI — T02
- [x] T06 Hacer foto o elegir de galería — UI — T02
- [ ] T07 Quitar fondo en el móvil — Lógica — T06
- [ ] T08 Formulario añadir prenda (tipo, color, estación, uso, marca, talla) — UI — T04, T06
- [ ] T09 Detectar color principal — Lógica — T07
- [ ] T10 Armario con pestañas por tipo — UI — T04
- [ ] T11 Pestañas por color y estación — UI — T10
- [ ] T12 Filtro multicriterio — UI — T10
- [ ] T13 Detalle de prenda: editar y eliminar con confirmación — UI — T08
- [ ] T14 Like y pestaña de favoritas — UI — T10
- [ ] T15 Pruebas v0.1 — QA — T05–T14

## v0.2 Outfits
- [x] T16 Diseñar avatar 2D con capas — Diseño — —
- [ ] T17 Prendas sobre el avatar por capas — Lógica — T07, T16
- [ ] T18 Editor de outfit — UI — T17
- [ ] T19 Lista de outfits guardados — UI — T18
- [ ] T20 Exportar outfit como imagen y compartir — Lógica — T18
- [ ] T21 Pruebas v0.2 — QA — T16–T20

## v0.3 Tallas
- [x] T22 Pantalla de medidas con guía — UI — T04
- [ ] T23 Tablas Zara, Pull&Bear, Bershka, Stradivarius, Massimo Dutti — Contenido — —
- [~] T24 Tablas Mango y H&M — Contenido — — (parcial: H&M camiseta y pantalón hecho; Mango pendiente de guía oficial)
- [x] T25 Tablas Brownie, Scalpers, Hollister, Brandy Melville — Contenido — —
- [x] T26 Tabla genérica europea — Contenido — —
- [ ] T27 Formato común de tablas y carga — Datos — T03, T23–T26
- [ ] T28 Algoritmo de ajuste y talla ideal — Lógica — T22, T27
- [ ] T29 Etiqueta de corte (slim/normal/oversize) — Lógica — T28
- [ ] T30 Resultado de talla en el detalle — UI — T13, T28
- [ ] T31 Pruebas del algoritmo con casos reales — QA — T28–T30

## v0.4 Organización
- [ ] T32 Calendario: asignar outfits a días — UI — T19
- [ ] T33 Historial y contador de usos — Lógica — T32
- [ ] T34 Limpia/sucia — UI — T13
- [ ] T35 Sucia automática tras N usos — Lógica — T33, T34
- [ ] T36 Clasificación por uso en pestañas y filtro — UI — T12
- [ ] T37 Maleta de viaje — UI — T19, T32
- [ ] T38 Pruebas v0.4 — QA — T32–T37

## v0.5 Sugerencias
- [x] T39 Servicio del tiempo (ubicación o ciudad) — Lógica — T02
- [ ] T40 Reglas de combinación de colores — Lógica — T03
- [ ] T41 Motor de sugerencias — Lógica — T35, T36, T39, T40
- [ ] T42 Aprender estilo de likes y outfits guardados — Lógica — T14, T41
- [ ] T43 Pantalla "outfit del día" — UI — T17, T41
- [ ] T44 Pruebas v0.5 — QA — T39–T43

## v1.0 Lanzamiento
- [x] T45 Exportar/importar copia — Datos — T03
- [ ] T46 Pulir estética de revista — Diseño — T44
- [ ] T47 Rendimiento con armarios grandes — Lógica — T44
- [x] T48 Icono y fichas de tiendas — Contenido — —
- [x] T49 Política de privacidad (cámara, ubicación) — Contenido — —
- [ ] T50 Prueba con usuarias reales y errores — QA — T45–T47
- [ ] T51 Publicar en Google Play y App Store — Base — T48–T50
