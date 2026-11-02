# store/

Recursos para Google Play y App Store (T48). Listos para integrar; no tocan `androidApp`/`iosApp`.

- `icon/`: SVG fuente. `icon.svg` (compuesto, fondo blanco), `foreground.svg` + `bg.svg` (capas del adaptive icon), `monochrome.svg` (icono temático Android 13+). Burdeos `#6D1A2B`, serif editorial. Letra convertida a trazos, sin depender de fuentes.
- `android/play-store-icon-512.png`: icono de ficha de Google Play.
- `android/res/mipmap-*/`: `ic_launcher.png` (48dp, heredado), `ic_launcher_foreground.png` y `ic_launcher_monochrome.png` (108dp, adaptive). Fondo adaptive: color `#FFFFFF`. El motivo cabe en la zona segura (66dp centrales).
- `ios/AppIcon-1024.png`: icono único 1024×1024 RGB sin canal alfa, para el asset catalog.
- `ficha-es.md`, `ficha-en.md`: textos de las fichas.

Integración pendiente: crear `mipmap-anydpi-v26/ic_launcher.xml` (adaptive-icon con background `@color/ic_launcher_background`, foreground y monochrome) y copiar el PNG iOS a `AppIcon.appiconset`.
