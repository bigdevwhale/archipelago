package app.mostki.ui.art

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import app.mostki.domain.Trait

/** The engraver's inks. */
object Ink {
    const val INK = 0xFF3A2D1F.toInt()
    const val MUTED = 0xFF6B5841.toInt()
    const val PAPER = 0xFFEFE3C6.toInt()
    const val LAND = 0xFFE6D5AB.toInt()
    const val LAND2 = 0xFFDCC69A.toInt()
    const val RED = 0xFF8C2F1C.toInt()
    const val GOLD = 0xFFE2A43A.toInt()
    const val PLANK = 0xFFDCC596.toInt()
    const val STONE = 0xFFCDB88C.toInt()

    fun a(c: Int, alpha: Float) = (c and 0xFFFFFF) or ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24)
}

/**
 * Little landmarks for what someone loves, drawn on their island like the towns and forests on an old
 * chart. Each fits a box about 22 units across around (0, 0), its base near y = 7.
 */
object Glyphs {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()

    private fun line(c: Canvas, vararg p: Float) {
        path.reset(); path.moveTo(p[0], p[1])
        var i = 2
        while (i < p.size) { path.lineTo(p[i], p[i + 1]); i += 2 }
        c.drawPath(path, stroke)
    }

    private fun shape(c: Canvas, close: Boolean = true, vararg p: Float) {
        path.reset(); path.moveTo(p[0], p[1])
        var i = 2
        while (i < p.size) { path.lineTo(p[i], p[i + 1]); i += 2 }
        if (close) path.close()
        c.drawPath(path, fill); c.drawPath(path, stroke)
    }

    fun draw(c: Canvas, t: Trait, x: Float, y: Float, k: Float, ink: Int = Ink.INK, paper: Int = Ink.PAPER) {
        c.save(); c.translate(x, y); c.scale(k, k)
        stroke.color = ink; stroke.strokeWidth = 1.25f; fill.color = paper
        when (t) {
            Trait.MOUNTAINS -> {
                shape(c, true, -11f, 7f, -3f, -9f, 4f, 2f, 7f, -3f, 12f, 7f)
                line(c, -3f, -9f, -5f, 7f); line(c, -7f, -1f, -9f, 6f); line(c, 7f, -3f, 6f, 7f)
                line(c, -3f, -9f, -1f, -5f, 1f, -6f)
            }
            Trait.FOREST -> {
                for ((dx, s) in listOf(-6f to .8f, 5f to 1f)) {
                    c.save(); c.translate(dx, 0f); c.scale(s, s)
                    shape(c, true, 0f, -10f, 6f, 1f, 2f, 1f, 2f, 7f, -2f, 7f, -2f, 1f, -6f, 1f)
                    line(c, -2f, -3f, 2f, -5f)
                    c.restore()
                }
            }
            Trait.COFFEE -> {
                shape(c, true, -7f, -2f, 7f, -2f, 6f, 7f, -6f, 7f)
                path.reset(); path.addArc(RectF(5f, 0f, 11f, 5f), -90f, 180f); c.drawPath(path, stroke)
                line(c, -3f, -5f, -2f, -8f, -3f, -11f); line(c, 1f, -5f, 2f, -8f, 1f, -11f)
            }
            Trait.BOOKS -> {
                shape(c, true, -9f, 1f, 9f, 1f, 9f, 7f, -9f, 7f)
                shape(c, true, -7f, -5f, 8f, -5f, 8f, 1f, -7f, 1f)
                line(c, -5f, 4f, 6f, 4f); line(c, -4f, -2f, 5f, -2f)
            }
            Trait.MUSIC -> {
                path.reset(); path.addOval(RectF(-7f, 2f, -1f, 7f), Path.Direction.CW); fill.color = ink; c.drawPath(path, fill)
                path.reset(); path.addOval(RectF(3f, 0f, 9f, 5f), Path.Direction.CW); c.drawPath(path, fill)
                fill.color = paper
                line(c, -1.5f, 4.5f, -1.5f, -9f, 8.5f, -11f, 8.5f, 2.5f); line(c, -1.5f, -5f, 8.5f, -7f)
            }
            Trait.SPORT -> {
                line(c, -4f, 8f, -4f, -11f)
                shape(c, true, -4f, -11f, 9f, -7f, -4f, -3f)
                line(c, -8f, 8f, 0f, 8f)
            }
            Trait.SEA -> {
                shape(c, true, -10f, 2f, 10f, 2f, 6f, 7f, -6f, 7f)
                shape(c, true, 0f, 0f, 0f, -12f, 8f, 0f)
                line(c, -1f, 0f, -1f, -11f); line(c, -1f, -9f, -6f, 0f)
            }
            Trait.PETS -> {
                // A sitting cat, the kind printed on old maps of ports.
                shape(c, true, -6f, 7f, -7f, 0f, -4f, -5f, -5f, -11f, -2f, -8f, 2f, -8f, 5f, -11f, 4f, -5f, 7f, 0f, 6f, 7f)
                line(c, 6f, 6f, 10f, 4f, 10f, -1f)
                fill.color = ink; c.drawCircle(-2f, -4.5f, .9f, fill); c.drawCircle(2f, -4.5f, .9f, fill); fill.color = paper
            }
            Trait.ART -> {
                line(c, -6f, 8f, 0f, -11f, 6f, 8f); line(c, 0f, -11f, 0f, 8f)
                shape(c, true, -8f, -6f, 8f, -6f, 8f, 3f, -8f, 3f)
                line(c, -5f, 0f, -2f, -3f, 1f, 0f, 5f, -4f)
            }
            Trait.GAMES -> {
                shape(c, true, -8f, -3f, 0f, -7f, 8f, -3f, 8f, 5f, 0f, 9f, -8f, 5f)
                line(c, -8f, -3f, 0f, 1f, 8f, -3f); line(c, 0f, 1f, 0f, 9f)
                fill.color = ink
                c.drawCircle(0f, -3f, 1f, fill); c.drawCircle(-4f, 2f, 1f, fill); c.drawCircle(-4f, 5.5f, 1f, fill); c.drawCircle(4f, 4f, 1f, fill)
                fill.color = paper
            }
            Trait.TRAVEL -> {
                shape(c, true, -10f, 7f, 0f, -9f, 10f, 7f)
                shape(c, true, -3f, 7f, 0f, 0f, 3f, 7f)
                line(c, 0f, -9f, 0f, -12f, 5f, -11f)
            }
            Trait.GARDEN -> {
                line(c, 0f, 8f, 0f, -2f); line(c, 0f, 4f, 5f, 1f); line(c, 0f, 3f, -4f, 0f)
                for (a in 0 until 5) {
                    c.save(); c.translate(0f, -6f); c.rotate(a * 72f)
                    path.reset(); path.addOval(RectF(-2.2f, -6.5f, 2.2f, -1f), Path.Direction.CW); c.drawPath(path, fill); c.drawPath(path, stroke)
                    c.restore()
                }
                fill.color = Ink.GOLD; c.drawCircle(0f, -6f, 1.8f, fill); fill.color = paper
            }
        }
        c.restore()
    }

    /** Your own island's house. */
    fun house(c: Canvas, x: Float, y: Float, k: Float) {
        c.save(); c.translate(x, y); c.scale(k, k)
        stroke.color = Ink.INK; stroke.strokeWidth = 1.2f; fill.color = Ink.PAPER
        shape(c, true, -7f, 7f, -7f, -1f, 0f, -8f, 7f, -1f, 7f, 7f)
        line(c, -9f, 1f, 0f, -9f, 9f, 1f); line(c, -2f, 7f, -2f, 2f, 2f, 2f, 2f, 7f)
        c.restore()
    }

    /** Your own island's lighthouse; [lit] gives it a gold lamp. */
    fun lighthouse(c: Canvas, x: Float, y: Float, k: Float, lit: Boolean) {
        c.save(); c.translate(x, y); c.scale(k, k)
        stroke.color = Ink.INK; stroke.strokeWidth = 1.2f; fill.color = Ink.PAPER
        shape(c, true, -4f, 9f, -2.5f, -6f, 2.5f, -6f, 4f, 9f)
        line(c, -3.4f, 4f, 3.4f, 4f); line(c, -3f, -1f, 3f, -1f)
        fill.color = if (lit) Ink.GOLD else Ink.PAPER
        shape(c, true, -2.5f, -6f, -2.5f, -10f, 2.5f, -10f, 2.5f, -6f)
        fill.color = Ink.PAPER
        shape(c, true, -4f, -10f, 0f, -13f, 4f, -10f)
        c.restore()
    }

    /** A paper boat, waiting to be sent. */
    fun kite(c: Canvas, x: Float, y: Float, k: Float, ink: Int = Ink.RED) {
        c.save(); c.translate(x, y); c.scale(k, k)
        stroke.color = ink; stroke.strokeWidth = 1.3f; fill.color = Ink.PAPER
        shape(c, true, -10f, 0f, 10f, 0f, 6f, 6f, -6f, 6f)
        shape(c, true, -7f, 0f, 0f, -13f, 7f, 0f)
        line(c, 0f, -13f, 0f, 0f)
        c.restore()
    }

    /** A small ferry boat, under sail. */
    fun boat(c: Canvas, x: Float, y: Float, k: Float, ink: Int) {
        c.save(); c.translate(x, y); c.scale(k, k)
        stroke.color = ink; stroke.strokeWidth = 1.3f; fill.color = Ink.PAPER
        shape(c, true, -8f, 2f, 8f, 2f, 5f, 6f, -5f, 6f)
        shape(c, true, 0f, 1f, 0f, -9f, 6f, 1f)
        c.restore()
    }
}
