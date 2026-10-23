package app.mostki.ui.art

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

/** The shells of courage: one for every "no" heard. Every tenth is a nautilus. */
object Shells {
    const val KINDS = 5

    fun kind(n: Int) = if ((n + 1) % 10 == 0) 4 else (n * 3 + 1) % 4

    private val colors = intArrayOf(0xFFE8C9A8.toInt(), 0xFFEFD9C0.toInt(), 0xFFDCB48E.toInt(), 0xFF8E9AA6.toInt(), 0xFFF1E2CC.toInt())
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND; color = Ink.INK }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    /** Draws shell [kind] centred at (x, y), [s] units across ≈ 28·s. */
    fun draw(c: Canvas, kind: Int, x: Float, y: Float, s: Float, tilt: Float = 0f) {
        c.save(); c.translate(x, y); c.scale(s, s); c.rotate(tilt)
        fill.color = colors[kind]; stroke.strokeWidth = 1.1f
        when (kind) {
            0 -> { // scallop
                path.reset(); path.moveTo(0f, 11f); path.cubicTo(-15f, 2f, -12f, -12f, 0f, -12f); path.cubicTo(12f, -12f, 15f, 2f, 0f, 11f); path.close()
                c.drawPath(path, fill); c.drawPath(path, stroke)
                stroke.strokeWidth = .7f
                for (k in -2..2) c.drawLine(0f, 10f, k * 4.5f, -11f + kotlin.math.abs(k) * 1.5f, stroke)
                stroke.strokeWidth = 1.1f
                path.reset(); path.moveTo(-4f, 10f); path.lineTo(-6f, 14f); path.lineTo(6f, 14f); path.lineTo(4f, 10f)
                c.drawPath(path, fill); c.drawPath(path, stroke)
            }
            1 -> { // cowrie
                val r = RectF(-8f, -12f, 8f, 12f)
                c.drawOval(r, fill); c.drawOval(r, stroke)
                path.reset(); path.moveTo(0f, -10f); path.cubicTo(-2f, -4f, 2f, 4f, 0f, 10f); c.drawPath(path, stroke)
                stroke.strokeWidth = .7f
                for (k in -3..3) { c.drawLine(-2f, k * 2.6f, -4f, k * 2.6f, stroke); c.drawLine(2f, k * 2.6f, 4f, k * 2.6f, stroke) }
                fill.color = Ink.a(Ink.MUTED, .35f)
                for ((dx, dy) in listOf(-5f to -6f, 5f to -3f, -4f to 5f, 5f to 7f)) c.drawCircle(dx, dy, 1.1f, fill)
            }
            2 -> { // whelk
                path.reset(); path.moveTo(0f, -13f); path.lineTo(7f, -3f); path.cubicTo(10f, 4f, 5f, 10f, 1f, 13f); path.lineTo(-1f, 9f)
                path.cubicTo(-8f, 6f, -9f, -2f, -5f, -5f); path.close()
                c.drawPath(path, fill); c.drawPath(path, stroke)
                stroke.strokeWidth = .8f
                c.drawLine(-3f, -8f, 4f, -6f, stroke); c.drawLine(-6f, -3f, 7f, 0f, stroke); c.drawLine(-6f, 3f, 7f, 5f, stroke)
            }
            3 -> { // mussel
                path.reset(); path.moveTo(-2f, -13f); path.cubicTo(8f, -8f, 9f, 6f, 3f, 13f); path.cubicTo(-4f, 10f, -9f, -4f, -2f, -13f); path.close()
                c.drawPath(path, fill); c.drawPath(path, stroke)
                stroke.strokeWidth = .7f
                for (k in 1..3) { path.reset(); path.moveTo(-2f, -13f + k); path.cubicTo(6f - k * 2, -6f, 6f - k * 2, 6f, 2f, 12f - k * 2); c.drawPath(path, stroke) }
            }
            else -> { // nautilus
                c.drawCircle(0f, 0f, 12f, fill); c.drawCircle(0f, 0f, 12f, stroke)
                stroke.strokeWidth = .8f
                path.reset()
                var a = 0.0; var r = 11.0
                path.moveTo(11f, 0f)
                while (r > 1.2) { a += .25; r *= .955; path.lineTo((cos(a) * r).toFloat(), (sin(a) * r).toFloat()) }
                c.drawPath(path, stroke)
                stroke.color = Ink.RED
                for (k in 0 until 7) { val aa = k * .8; c.drawLine((cos(aa) * 12).toFloat(), (sin(aa) * 12).toFloat(), (cos(aa) * 7).toFloat(), (sin(aa) * 7).toFloat(), stroke) }
                stroke.color = Ink.INK
            }
        }
        c.restore()
    }
}
