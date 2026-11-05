package app.luxion.fitarchiveapp.ui.avatar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.luxion.fitarchiveapp.avatar.AspectoAvatar
import app.luxion.fitarchiveapp.avatar.CapaAvatar
import app.luxion.fitarchiveapp.avatar.EscalaAvatar
import app.luxion.fitarchiveapp.avatar.EstiloPelo
import kotlin.math.hypot

private val Linea = Color(0xFF2F2A2A)
private val Labios = Color(0xFFA34A5E)
private const val GROSOR = 2.5f

private fun rgb(v: Int) = Color(0xFF000000.toInt() or v)

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

/** Brazo [lado] (-1 izquierda, +1 derecha): hombro → codo → muñeca → mano. */
private fun brazo(e: EscalaAvatar, lado: Int): Path {
    val c = e.cuerpo
    val hombro = Offset(200f + lado * e.u(c.hombrosCm / 2 - 2.8), e.yHombros + e.u(3.0))
    val muneca = Offset(hombro.x + lado * e.u(c.brazoCm * 0.12), hombro.y + e.u(c.brazoCm * 0.96))
    val codo = Offset(hombro.x + lado * e.u(c.brazoCm * 0.045), hombro.y + e.u(c.brazoCm * 0.5))
    val mano = Offset(muneca.x + lado * e.u(0.8), muneca.y + e.u(8.0))
    return Path().apply {
        tubo(hombro, codo, e.u(3.6), e.u(2.8))
        tubo(codo, muneca, e.u(2.8), e.u(2.0))
        tubo(muneca, mano, e.u(2.0), e.u(2.4))
    }
}

private fun DrawScope.relleno(path: Path, color: Color) {
    drawPath(path, color)
    drawPath(path, Linea, style = Stroke(GROSOR, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun cabeza(e: EscalaAvatar): Rect {
    val arriba = EscalaAvatar.CORONILLA.toFloat()
    val abajo = e.yBarbilla
    val ancho = (abajo - arriba) * 0.78f
    return Rect(200f - ancho / 2f, arriba, 200f + ancho / 2f, abajo)
}

/** Capa [capa] del avatar paramétrico, dibujada en el lienzo de 400 x 1000 unidades. */
fun DrawScope.dibujarCapaAvatar(capa: CapaAvatar, e: EscalaAvatar, aspecto: AspectoAvatar, rostro: ImageBitmap?) {
    val piel = rgb(aspecto.pielRgb)
    val pelo = rgb(aspecto.peloRgb)
    val cab = cabeza(e)
    val foto = if (aspecto.conFoto) rostro else null
    when (capa) {
        CapaAvatar.HAIR_BACK -> if (foto == null) peloDetras(aspecto.estiloPelo, cab, e, pelo)
        CapaAvatar.BODY -> {
            relleno(siluetaCuerpo(e), piel)
            relleno(Path().apply { addOval(cab) }, piel)
        }
        CapaAvatar.FACE -> if (foto != null) fotoCara(foto, cab) else rasgos(cab)
        CapaAvatar.ARMS -> {
            relleno(brazo(e, -1), piel)
            relleno(brazo(e, 1), piel)
        }
        CapaAvatar.HAIR_FRONT -> if (foto == null) peloDelante(aspecto.estiloPelo, cab, pelo)
    }
}

private fun DrawScope.rasgos(cab: Rect) {
    val cx = cab.center.x
    val ojoY = cab.top + cab.height * 0.52f
    val sep = cab.width * 0.2f
    listOf(-1, 1).forEach { l ->
        val x = cx + l * sep
        drawCircle(Linea, radius = cab.width * 0.035f, center = Offset(x, ojoY))
        val ceja = Path().apply {
            moveTo(x - cab.width * 0.09f, ojoY - cab.height * 0.1f)
            quadraticTo(x, ojoY - cab.height * 0.135f, x + cab.width * 0.09f, ojoY - cab.height * 0.1f)
        }
        drawPath(ceja, Linea, style = Stroke(GROSOR, cap = StrokeCap.Round))
    }
    val nariz = Path().apply {
        moveTo(cx, ojoY + cab.height * 0.04f)
        quadraticTo(cx - cab.width * 0.04f, ojoY + cab.height * 0.16f, cx + cab.width * 0.02f, ojoY + cab.height * 0.17f)
    }
    drawPath(nariz, Linea, style = Stroke(GROSOR * 0.8f, cap = StrokeCap.Round))
    val boca = Path().apply {
        moveTo(cx - cab.width * 0.11f, cab.top + cab.height * 0.8f)
        quadraticTo(cx, cab.top + cab.height * 0.86f, cx + cab.width * 0.11f, cab.top + cab.height * 0.8f)
    }
    drawPath(boca, Labios, style = Stroke(GROSOR * 1.6f, cap = StrokeCap.Round))
}

/** La foto, recortada en cuadrado desde el centro-arriba y ceñida al óvalo de la cabeza. */
private fun DrawScope.fotoCara(foto: ImageBitmap, cab: Rect) {
    val lado = minOf(foto.width, foto.height)
    val sx = (foto.width - lado) / 2
    val sy = ((foto.height - lado) * 0.25f).toInt()
    val ovalo = Path().apply { addOval(cab) }
    clipPath(ovalo) {
        drawImage(
            foto,
            srcOffset = IntOffset(sx, sy),
            srcSize = IntSize(lado, lado),
            dstOffset = IntOffset(cab.left.toInt(), cab.top.toInt()),
            dstSize = IntSize(cab.width.toInt() + 1, cab.width.toInt() + 1),
        )
    }
    drawPath(ovalo, Linea, style = Stroke(GROSOR))
}

private fun DrawScope.peloDetras(estilo: EstiloPelo, cab: Rect, e: EscalaAvatar, color: Color) {
    val cx = cab.center.x
    when (estilo) {
        EstiloPelo.LARGO, EstiloPelo.MEDIO -> {
            val abajo = if (estilo == EstiloPelo.LARGO) e.yPecho + e.u(4.0) else e.yBarbilla + e.u(10.0)
            val ancho = cab.width * 0.62f
            val medio = abajo * 0.7f + cab.bottom * 0.3f
            val p = Path().apply {
                moveTo(cx, cab.top - 4f)
                cubicTo(cx + ancho * 1.4f, cab.top - 4f, cx + ancho * 1.25f, medio, cx + ancho, abajo)
                lineTo(cx - ancho, abajo)
                cubicTo(cx - ancho * 1.25f, medio, cx - ancho * 1.4f, cab.top - 4f, cx, cab.top - 4f)
                close()
            }
            relleno(p, color)
        }
        EstiloPelo.RECOGIDO -> {
            val r = cab.width * 0.2f
            relleno(Path().apply { addOval(Rect(cx - r, cab.top - r * 1.5f, cx + r, cab.top + r * 0.5f)) }, color)
        }
        EstiloPelo.CORTO, EstiloPelo.SIN_PELO -> Unit
    }
}

private fun DrawScope.peloDelante(estilo: EstiloPelo, cab: Rect, color: Color) {
    if (estilo == EstiloPelo.SIN_PELO) return
    val cx = cab.center.x
    val l = cab.left
    val r = cab.right
    val alto = cab.height
    val lateral = if (estilo == EstiloPelo.CORTO) 0.38f else 0.3f
    val p = Path().apply {
        moveTo(cx, cab.top - 3f)
        cubicTo(r + 6f, cab.top - 3f, r + 4f, cab.top + alto * 0.2f, r, cab.top + alto * lateral)
        cubicTo(r - cab.width * 0.05f, cab.top + alto * 0.22f, cx + cab.width * 0.2f, cab.top + alto * 0.17f, cx - cab.width * 0.12f, cab.top + alto * 0.2f)
        cubicTo(cx - cab.width * 0.3f, cab.top + alto * 0.22f, l + cab.width * 0.05f, cab.top + alto * 0.24f, l, cab.top + alto * lateral)
        cubicTo(l - 4f, cab.top + alto * 0.2f, l - 6f, cab.top - 3f, cx, cab.top - 3f)
        close()
    }
    relleno(p, color)
}
