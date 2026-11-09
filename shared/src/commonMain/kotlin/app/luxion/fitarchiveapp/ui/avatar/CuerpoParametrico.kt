package app.luxion.fitarchiveapp.ui.avatar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.luxion.fitarchiveapp.avatar.CapaAvatar
import app.luxion.fitarchiveapp.avatar.EscalaAvatar
import kotlin.math.hypot

// Maniquí de boutique: tono neutro suave con un degradado lateral para dar volumen y contorno fino.
// Los tonos son medios para que se lea igual sobre fondo claro y oscuro.
private val Mani_Centro = Color(0xFFE2DBD4)
private val Mani_Borde = Color(0xFFC4BBB2)
private val Linea = Color(0xFFA39A92)
private const val GROSOR = 1.6f

/** Curva suave (Catmull-Rom) por [p]; [mover] empieza un trazo nuevo, si no continúa con una línea. */
private fun Path.curvaPor(p: List<Offset>, mover: Boolean) {
    if (p.isEmpty()) return
    if (mover) moveTo(p[0].x, p[0].y) else lineTo(p[0].x, p[0].y)
    for (i in 0 until p.size - 1) {
        val p0 = p.getOrElse(i - 1) { p[i] }
        val p1 = p[i]
        val p2 = p[i + 1]
        val p3 = p.getOrElse(i + 2) { p[i + 1] }
        val c1 = p1 + (p2 - p0) / 6f
        val c2 = p2 - (p3 - p1) / 6f
        cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
}

private fun Offset.espejo() = Offset(400f - x, y)

/** Silueta de cuerpo entero (cuello, torso y piernas) según las medidas. Sin cabeza ni brazos. */
internal fun siluetaCuerpo(e: EscalaAvatar): Path {
    val c = e.cuerpo
    val cuelloM = e.u(5.2)
    val hombrosM = e.u(c.hombrosCm / 2)
    val pechoM = e.u(c.pechoCm * EscalaAvatar.ANCHO_FRONTAL_PECHO / 2)
    val cinturaM = e.u(c.cinturaCm * EscalaAvatar.ANCHO_FRONTAL_CINTURA / 2)
    val caderaM = e.u(c.caderaCm * EscalaAvatar.ANCHO_FRONTAL_CADERA / 2)
    val eje = 200f
    val centroPierna = caderaM * 0.52f
    val rodillaR = maxOf(e.u(4.2), caderaM * 0.30f)
    val tobilloR = e.u(2.9)
    val muslo = caderaM * 0.30f
    val suelo = EscalaAvatar.SUELO.toFloat()
    val pie = e.u(c.pieCm * 0.22)

    // Lado izquierdo (x < 200), de arriba abajo por fuera.
    val fueraIzq = listOf(
        Offset(eje - cuelloM, e.yBarbilla - e.u(1.0)),
        Offset(eje - cuelloM * 1.08f, e.yCuello),
        Offset(eje - hombrosM, e.yHombros + e.u(1.0)),
        Offset(eje - pechoM, e.yPecho),
        Offset(eje - cinturaM, e.yCintura),
        Offset(eje - caderaM, e.yCadera),
        Offset(eje - centroPierna - muslo * 1.05f, e.yEntrepierna + e.u(8.0)),
        Offset(eje - centroPierna - rodillaR, e.yRodilla),
        Offset(eje - centroPierna * 0.9f - tobilloR, e.yTobillo),
        Offset(eje - centroPierna * 0.9f - tobilloR - pie * 0.4f, suelo - 4f),
    )
    val dentroIzq = listOf(
        Offset(eje - centroPierna * 0.9f + tobilloR + pie * 0.1f, suelo - 4f),
        Offset(eje - centroPierna * 0.9f + tobilloR, e.yTobillo),
        Offset(eje - centroPierna + rodillaR * 0.85f, e.yRodilla),
        Offset(eje - centroPierna + muslo * 0.9f, e.yEntrepierna + e.u(10.0)),
        Offset(eje, e.yEntrepierna),
    )
    return Path().apply {
        curvaPor(fueraIzq, mover = true)
        lineTo(dentroIzq.first().x, dentroIzq.first().y)
        curvaPor(dentroIzq, mover = false)
        curvaPor(dentroIzq.reversed().map { it.espejo() }, mover = false)
        lineTo(fueraIzq.last().espejo().x, fueraIzq.last().espejo().y)
        curvaPor(fueraIzq.reversed().map { it.espejo() }, mover = false)
        close()
    }
}

private fun Path.tubo(a: Offset, b: Offset, ra: Float, rb: Float) {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val l = hypot(dx, dy).coerceAtLeast(0.001f)
    val nx = -dy / l
    val ny = dx / l
    moveTo(a.x + nx * ra, a.y + ny * ra)
    lineTo(b.x + nx * rb, b.y + ny * rb)
    lineTo(b.x - nx * rb, b.y - ny * rb)
    lineTo(a.x - nx * ra, a.y - ny * ra)
    close()
    addOval(Rect(a.x - ra, a.y - ra, a.x + ra, a.y + ra))
    addOval(Rect(b.x - rb, b.y - rb, b.x + rb, b.y + rb))
}

/** Brazo [lado] (-1 izquierda, +1 derecha): hombro → codo → muñeca → punta redondeada (sin mano). */
private fun brazo(e: EscalaAvatar, lado: Int): Path {
    val c = e.cuerpo
    val hombro = Offset(200f + lado * e.u(c.hombrosCm / 2 - 2.8), e.yHombros + e.u(3.0))
    val muneca = Offset(hombro.x + lado * e.u(c.brazoCm * 0.12), hombro.y + e.u(c.brazoCm * 0.96))
    val codo = Offset(hombro.x + lado * e.u(c.brazoCm * 0.045), hombro.y + e.u(c.brazoCm * 0.5))
    val mano = Offset(muneca.x + lado * e.u(0.8), muneca.y + e.u(8.0))
    // Unión de los tramos: así el contorno no marca líneas internas en las articulaciones.
    val tramos = listOf(
        Path().apply { tubo(hombro, codo, e.u(3.6), e.u(2.8)) },
        Path().apply { tubo(codo, muneca, e.u(2.8), e.u(2.0)) },
        Path().apply { tubo(muneca, mano, e.u(2.0), e.u(2.4)) },
    )
    return tramos.reduce { a, b -> Path.combine(PathOperation.Union, a, b) }
}

/** Relleno con degradado horizontal (borde más oscuro, centro claro) a lo ancho de [path], y contorno fino. */
private fun DrawScope.relleno(path: Path) {
    val caja = path.getBounds()
    val brocha = Brush.horizontalGradient(
        0f to Mani_Borde,
        0.28f to Mani_Centro,
        0.72f to Mani_Centro,
        1f to Mani_Borde,
        startX = caja.left,
        endX = caja.right,
    )
    drawPath(path, brocha)
    drawPath(path, Linea, style = Stroke(GROSOR, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Cabeza ovalada lisa, sin rasgos: un poco más estrecha por la barbilla que por la frente. */
private fun cabeza(e: EscalaAvatar): Path {
    val arriba = EscalaAvatar.CORONILLA.toFloat()
    val abajo = e.yBarbilla
    val mitad = (abajo - arriba) * 0.72f / 2f
    val alto = abajo - arriba
    return Path().apply {
        moveTo(200f, arriba)
        cubicTo(200f + mitad * 1.05f, arriba, 200f + mitad, arriba + alto * 0.55f, 200f + mitad * 0.45f, abajo - alto * 0.08f)
        cubicTo(200f + mitad * 0.3f, abajo, 200f - mitad * 0.3f, abajo, 200f - mitad * 0.45f, abajo - alto * 0.08f)
        cubicTo(200f - mitad, arriba + alto * 0.55f, 200f - mitad * 1.05f, arriba, 200f, arriba)
        close()
    }
}

/** Capa [capa] del maniquí paramétrico, dibujada en el lienzo de 400 x 1000 unidades. */
fun DrawScope.dibujarCapaAvatar(capa: CapaAvatar, e: EscalaAvatar) {
    when (capa) {
        CapaAvatar.BODY -> {
            relleno(siluetaCuerpo(e))
            relleno(cabeza(e))
        }
        CapaAvatar.ARMS -> {
            relleno(brazo(e, -1))
            relleno(brazo(e, 1))
        }
    }
}
