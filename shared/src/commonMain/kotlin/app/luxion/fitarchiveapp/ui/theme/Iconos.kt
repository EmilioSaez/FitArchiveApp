package app.luxion.fitarchiveapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Iconos de trazo simple de 24 dp a 1,8 (diseño T52). Se tiñen con `tint` al dibujarlos. */
object Iconos {
    private fun trazo(nombre: String, vararg trazos: String): ImageVector =
        ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
            trazos.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun relleno(nombre: String, d: String): ImageVector =
        ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
        }.build()

    val Hoy = trazo("Hoy", "M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6 7 7M17 17l1.4 1.4M5.6 18.4 7 17M17 7l1.4-1.4", "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z")
    val Armario = trazo("Armario", "M12 8V6.5a2 2 0 1 1 2 2", "M3 18l9-8 9 8z")
    val Looks = trazo("Looks", "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z")
    val Calendario = trazo("Calendario", "M4 6h16v14H4z", "M4 10h16M8 3v4M16 3v4")
    val Perfil = trazo("Perfil", "M12 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8z", "M4 20a8 8 0 0 1 16 0")
    val Carpeta = trazo("Carpeta", "M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
    val Volver = trazo("Volver", "M15 5l-7 7 7 7")
    val Anadir = trazo("Anadir", "M12 5v14M5 12h14")
    val Check = trazo("Check", "M5 12.5l4.5 4.5L19 7.5")
    val Cerrar = trazo("Cerrar", "M6 6l12 12M18 6 6 18")
    val CorazonRelleno = relleno("CorazonRelleno", "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z")
    val Estrella = relleno("Estrella", "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z")
    val Ajustes = trazo("Ajustes", "M4 7h10M18 7h2M4 17h2M10 17h10", "M16 5v4M8 15v4")
    val MasVertical = relleno("MasVertical", "M12 7a1.6 1.6 0 1 0 0-3.2A1.6 1.6 0 0 0 12 7zM12 13.6a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2zM12 20.2a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2z")
    val Maleta = trazo("Maleta", "M5 8h14a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z", "M9 8V5h6v3")
}
