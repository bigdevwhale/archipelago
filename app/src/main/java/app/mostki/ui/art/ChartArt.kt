package app.mostki.ui.art

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import app.mostki.R
import app.mostki.domain.Bridge
import app.mostki.domain.Chart
import app.mostki.domain.Sea
import app.mostki.domain.Trait
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** One island as the chart draws it. */
class Isle(
    val id: Long,
    val name: String,
    val x: Float,
    val y: Float,
    val r: Float,
    val traits: List<Trait>,
    val sea: Sea,
    val bridge: Bridge,
    /** 0..1, how far a creaking bridge has sagged. */
    val sag: Float,
    val hint: String?,
    val warn: Boolean,
    /** Whole years known: a pennant for each at the island end of the bridge. */
    val years: Int = 0,
)

class Scene(
    val isles: List<Isle>,
    val ferries: List<Pair<Long, Long>>,
    /** The fog ring's radius. */
    val fog: Float,
    val me: String,
    val fogText: String,
    val lostText: String,
    /** A lighthouse evening today: the lamp is lit. */
    val lit: Boolean,
    val north: String,
    val selected: Long? = null,
)

/** Where the chart is looked at from: an offset in screen pixels and a zoom. */
data class View(val tx: Float = 0f, val ty: Float = 0f, val k: Float = 1f)

/** The sea chart, drawn like an engraving: hatched water, rhumb lines, inked coasts, bridges and fog. */
object ChartArt {
    var hand: Typeface = Typeface.DEFAULT
    var serif: Typeface = Typeface.SERIF
    var italic: Typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    private var fontsSet = false

    /** The app's own faces for the chart's lettering, loaded once. */
    fun fonts(ctx: Context) {
        if (fontsSet) return
        ResourcesCompat.getFont(ctx, R.font.caveat)?.let { hand = it }
        ResourcesCompat.getFont(ctx, R.font.pt_serif_bold)?.let { serif = it }
        ResourcesCompat.getFont(ctx, R.font.pt_serif_italic)?.let { italic = it }
        fontsSet = true
    }

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val bmp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path = Path()
    private val shapes = HashMap<Pair<Long, Int>, FloatArray>()
    private var puff: Bitmap? = null

    fun fit(scene: Scene, w: Float, h: Float): View {
        var x0 = -Chart.ME_R - 40; var x1 = Chart.ME_R + 40; var y0 = -Chart.ME_R - 40; var y1 = Chart.ME_R + 60
        for (i in scene.isles) if (i.sea != Sea.FOG) {
            x0 = min(x0, i.x - i.r - 50); x1 = max(x1, i.x + i.r + 50); y0 = min(y0, i.y - i.r - 30); y1 = max(y1, i.y + i.r + 60)
        }
        val k = min(w / (x1 - x0), h / (y1 - y0)).coerceIn(.35f, 1.6f)
        return View(-(x0 + x1) / 2 * k, -(y0 + y1) / 2 * k, k)
    }

    fun toWorld(sx: Float, sy: Float, w: Float, h: Float, v: View) = floatArrayOf((sx - w / 2 - v.tx) / v.k, (sy - h / 2 - v.ty) / v.k)

    /** The island under a tap, -1 for your own, null for open water. */
    fun hit(scene: Scene, sx: Float, sy: Float, w: Float, h: Float, v: View): Long? {
        val (x, y) = toWorld(sx, sy, w, h, v).let { it[0] to it[1] }
        val slack = 18f / v.k
        val best = scene.isles.minByOrNull { hypot(it.x - x, it.y - (y - 10)) - it.r }
        if (best != null && hypot(best.x - x, best.y - (y - 10)) < best.r + 26 + slack) return best.id
        if (hypot(x, y) < Chart.ME_R + slack) return -1
        return null
    }

    /** [top] is the status bar height to keep the compass under; a negative one leaves the compass out. */
    fun draw(c: Canvas, w: Float, h: Float, scene: Scene, v: View, time: Double, dp: Float, top: Float = 0f) {
        fill.shader = null
        fill.color = Ink.PAPER
        c.drawRect(0f, 0f, w, h, fill)
        c.save()
        c.translate(w / 2 + v.tx, h / 2 + v.ty)
        c.scale(v.k, v.k)
        val tl = toWorld(0f, 0f, w, h, v); val br = toWorld(w, h, w, h, v)
        waves(c, tl[0], tl[1], br[0], br[1])
        rhumbs(c, scene.fog * 1.8f)
        val byId = scene.isles.associateBy { it.id }
        for ((a, b) in scene.ferries) {
            val p = byId[a] ?: continue; val q = byId[b] ?: continue
            if (p.sea == Sea.FOG || q.sea == Sea.FOG) continue
            ferry(c, p, q)
        }
        for (i in scene.isles) if (i.sea != Sea.FOG) bridge(c, i, time)
        me(c, scene, time)
        for (i in scene.isles) if (i.sea != Sea.FOG) isle(c, i, time, scene.selected == i.id)
        fog(c, scene.fog)
        for (i in scene.isles) if (i.sea == Sea.FOG) lost(c, i, scene.lostText)
        text.typeface = italic; text.textSize = 18f; text.color = Ink.a(Ink.MUTED, .9f)
        c.drawText(scene.fogText, 0f, scene.fog * 1.16f + 40f, text)
        c.restore()
        if (top >= 0) compass(c, w - 52f * dp, top + 58f * dp, 30f * dp, scene.north)
    }

    private fun hash(a: Int, b: Int): Int {
        var x = a * 374761393 + b * 668265263
        x = (x xor (x ushr 13)) * 1274126177
        return x xor (x ushr 16)
    }

    private fun waves(c: Canvas, x0: Float, y0: Float, x1: Float, y1: Float) {
        stroke.color = Ink.a(Ink.INK, .15f); stroke.strokeWidth = 1f; stroke.pathEffect = null
        val sx = 54f; val sy = 22f
        val j0 = (y0 / sy).toInt() - 1; val j1 = (y1 / sy).toInt() + 1
        val i0 = (x0 / sx).toInt() - 1; val i1 = (x1 / sx).toInt() + 1
        if ((j1 - j0) * (i1 - i0) > 9000) return
        path.reset()
        for (j in j0..j1) for (i in i0..i1) {
            val hsh = hash(i, j)
            if ((hsh and 7) < 3) continue
            val x = i * sx + (if (j % 2 == 0) 0f else sx / 2) + ((hsh shr 4) and 15) - 7
            val y = j * sy + ((hsh shr 8) and 7) - 3
            path.moveTo(x, y)
            path.quadTo(x + 5, y - 4, x + 10, y)
            path.quadTo(x + 15, y + 4, x + 20, y)
        }
        c.drawPath(path, stroke)
    }

    private fun rhumbs(c: Canvas, r: Float) {
        stroke.color = Ink.a(Ink.INK, .09f); stroke.strokeWidth = 1f
        for (k in 0 until 16) {
            val a = k * PI / 8
            c.drawLine(0f, 0f, (cos(a) * r).toFloat(), (sin(a) * r).toFloat(), stroke)
        }
    }

    /** An island's coast: 12 points round it, wobbled by its id and smoothed. */
    private fun coast(seed: Long, r: Float): FloatArray = shapes.getOrPut(seed to r.roundToInt()) {
        val rnd = Random(seed * 9973 + 13)
        val n = 12
        FloatArray(n * 2).also { p ->
            for (i in 0 until n) {
                val a = i.toDouble() / n * 2 * PI + (rnd.nextDouble() - .5) * .35
                val rr = r * (.74 + rnd.nextDouble() * .42)
                p[i * 2] = (cos(a) * rr).toFloat(); p[i * 2 + 1] = (sin(a) * rr * .86).toFloat()
            }
        }
    }

    private fun blob(p: FloatArray, cx: Float, cy: Float, s: Float): Path {
        val n = p.size / 2
        fun px(i: Int) = cx + p[Math.floorMod(i, n) * 2] * s
        fun py(i: Int) = cy + p[Math.floorMod(i, n) * 2 + 1] * s
        path.reset()
        path.moveTo(px(0), py(0))
        for (i in 0 until n) {
            path.cubicTo(
                px(i) + (px(i + 1) - px(i - 1)) / 6, py(i) + (py(i + 1) - py(i - 1)) / 6,
                px(i + 1) - (px(i + 2) - px(i)) / 6, py(i + 1) - (py(i + 2) - py(i)) / 6,
                px(i + 1), py(i + 1),
            )
        }
        path.close()
        return path
    }

    private val dash1 = DashPathEffect(floatArrayOf(3f, 4f), 0f)
    private val dash2 = DashPathEffect(floatArrayOf(1f, 3f), 0f)
    private val dash3 = DashPathEffect(floatArrayOf(2f, 6f), 0f)
    private val dash4 = DashPathEffect(floatArrayOf(4f, 4f), 0f)

    private fun land(c: Canvas, p: FloatArray, x: Float, y: Float, selected: Boolean) {
        stroke.pathEffect = dash1; stroke.color = Ink.a(Ink.INK, .22f); stroke.strokeWidth = 1f
        c.drawPath(blob(p, x, y, 1.38f), stroke)
        stroke.pathEffect = null; stroke.color = Ink.a(Ink.INK, .38f)
        c.drawPath(blob(p, x, y, 1.17f), stroke)
        fill.color = Ink.LAND2
        c.drawPath(blob(p, x + 2.5f, y + 3f, 1f), fill)
        fill.color = if (selected) 0xFFF3E2B8.toInt() else Ink.LAND
        val coast = blob(p, x, y, 1f)
        c.drawPath(coast, fill)
        stroke.color = if (selected) Ink.RED else Ink.INK; stroke.strokeWidth = if (selected) 2.4f else 1.7f
        c.drawPath(coast, stroke)
        stroke.pathEffect = dash2; stroke.color = Ink.a(Ink.INK, .18f); stroke.strokeWidth = 1f
        c.drawPath(blob(p, x, y, .7f), stroke)
        stroke.pathEffect = null
    }

    private val OFFS = floatArrayOf(-.32f, -.2f, .3f, .04f, -.06f, .34f, .36f, -.34f)

    private fun isle(c: Canvas, i: Isle, time: Double, selected: Boolean) {
        land(c, coast(i.id, i.r), i.x, i.y, selected)
        val k = (i.r / 38f).coerceIn(.8f, 1.15f)
        i.traits.take(4).forEachIndexed { n, t -> Glyphs.draw(c, t, i.x + OFFS[n * 2] * i.r * 1.15f, i.y + OFFS[n * 2 + 1] * i.r * 1.15f, k, paper = Ink.PAPER) }
        text.typeface = hand; text.textSize = 33f; text.color = Ink.INK
        c.drawText(i.name, i.x, i.y + i.r + 28f, text)
        if (i.hint != null) {
            text.typeface = italic; text.textSize = 15f; text.color = if (i.warn) Ink.RED else Ink.MUTED
            c.drawText(i.hint, i.x, i.y + i.r + 47f, text)
        }
        if (i.sea == Sea.NEW) {
            val bob = (sin(time * 3.2 + i.id) * 2.5).toFloat()
            Glyphs.kite(c, i.x + i.r * .95f, i.y - i.r * .85f + bob, 1.1f)
        }
    }

    private fun lost(c: Canvas, i: Isle, lostText: String) {
        val p = coast(i.id, i.r)
        stroke.pathEffect = dash4; stroke.color = Ink.a(Ink.INK, .45f); stroke.strokeWidth = 1.2f
        c.drawPath(blob(p, i.x, i.y, 1f), stroke)
        stroke.pathEffect = null
        text.typeface = hand; text.textSize = 24f; text.color = Ink.a(Ink.INK, .55f)
        c.drawText(i.name + "?", i.x, i.y + 6f, text)
        text.typeface = italic; text.textSize = 11.5f; text.color = Ink.a(Ink.MUTED, .7f)
        c.drawText(lostText, i.x, i.y + 24f, text)
    }

    private fun me(c: Canvas, scene: Scene, time: Double) {
        val p = coast(0, Chart.ME_R)
        if (scene.lit) {
            c.save(); c.translate(14f, -14f); c.rotate((time * 40 % 360).toFloat())
            fill.color = Ink.a(Ink.GOLD, .18f)
            for (k in 0 until 6) {
                c.rotate(60f)
                path.reset(); path.moveTo(0f, 0f); path.lineTo(220f, -26f); path.lineTo(220f, 26f); path.close()
                c.drawPath(path, fill)
            }
            c.restore()
        }
        land(c, p, 0f, 0f, false)
        Glyphs.house(c, -16f, 8f, 1.5f)
        Glyphs.lighthouse(c, 15f, -8f, 1.6f, scene.lit)
        text.typeface = hand; text.textSize = 36f; text.color = Ink.RED
        c.drawText(scene.me, 0f, Chart.ME_R + 34f, text)
    }

    private fun quad(ax: Float, ay: Float, cx: Float, cy: Float, bx: Float, by: Float, t: Float, out: FloatArray) {
        val u = 1 - t
        out[0] = u * u * ax + 2 * u * t * cx + t * t * bx
        out[1] = u * u * ay + 2 * u * t * cy + t * t * by
        val dx = 2 * u * (cx - ax) + 2 * t * (bx - cx); val dy = 2 * u * (cy - ay) + 2 * t * (by - cy)
        val l = hypot(dx, dy).coerceAtLeast(.001f)
        out[2] = dx / l; out[3] = dy / l
    }

    private val q = FloatArray(4)

    /** From your island to theirs: rope, planks, stone arches, lanterns — sagging as it creaks. */
    private fun bridge(c: Canvas, i: Isle, time: Double) {
        val d = hypot(i.x, i.y).coerceAtLeast(1f)
        val ux = i.x / d; val uy = i.y / d
        val ax = ux * (Chart.ME_R - 6); val ay = uy * (Chart.ME_R - 10) * .9f
        val bx = i.x - ux * i.r * .78f; val by = i.y - uy * i.r * .68f
        var nx = -uy; var ny = ux
        if (ny < 0) { nx = -nx; ny = -ny }
        val creak = i.sea == Sea.CREAKING
        val sag = 10f + (if (creak) 12f + i.sag * 30f + (sin(time * 2.3 + i.id) * 3.5).toFloat() else 0f) + (if (i.sea == Sea.DRIFTING) 8f else 0f)
        val cx = (ax + bx) / 2 + nx * sag; val cy = (ay + by) / 2 + ny * sag
        val len = hypot(bx - ax, by - ay)
        val w = when (i.bridge) { Bridge.ROPE -> 3f; Bridge.PLANKS -> 5f; Bridge.STONE -> 6.5f; Bridge.ARCH -> 7f }
        val steps = max(8, (len / 4).toInt())
        val left = Path(); val right = Path()
        for (k in 0..steps) {
            quad(ax, ay, cx, cy, bx, by, k.toFloat() / steps, q)
            val lx = q[0] - q[3] * w; val ly = q[1] + q[2] * w
            val rx = q[0] + q[3] * w; val ry = q[1] - q[2] * w
            if (k == 0) { left.moveTo(lx, ly); right.moveTo(rx, ry) } else { left.lineTo(lx, ly); right.lineTo(rx, ry) }
        }
        stroke.color = Ink.INK; stroke.strokeWidth = 1.4f; stroke.pathEffect = if (i.sea == Sea.DRIFTING) dash4 else null
        fun ties(every: Float, frac: Float, sw: Float) {
            val m = max(2, (len / every).toInt())
            stroke.strokeWidth = sw
            for (k in 1 until m) {
                quad(ax, ay, cx, cy, bx, by, k.toFloat() / m, q)
                c.drawLine(q[0] - q[3] * w * frac, q[1] + q[2] * w * frac, q[0] + q[3] * w * frac, q[1] - q[2] * w * frac, stroke)
            }
            stroke.strokeWidth = 1.4f
        }
        if (i.bridge == Bridge.ROPE) {
            c.drawPath(left, stroke); c.drawPath(right, stroke)
            ties(9f, 1f, .9f)
        } else {
            val deck = Path(left)
            for (k in steps downTo 0) {
                quad(ax, ay, cx, cy, bx, by, k.toFloat() / steps, q)
                deck.lineTo(q[0] + q[3] * w, q[1] - q[2] * w)
            }
            deck.close()
            fill.color = if (i.bridge == Bridge.PLANKS) Ink.PLANK else Ink.STONE
            c.drawPath(deck, fill)
            c.drawPath(deck, stroke)
            if (i.bridge == Bridge.PLANKS) ties(5f, 1f, .8f)
            else {
                // Arches under the deck, then the joints of the stones.
                val m = max(2, (len / 22).toInt())
                stroke.strokeWidth = 1.1f
                for (k in 1 until m) {
                    quad(ax, ay, cx, cy, bx, by, k.toFloat() / m, q)
                    val px = q[0] + nx * w; val py = q[1] + ny * w
                    path.reset(); path.moveTo(px - q[2] * 9, py - q[3] * 9); path.quadTo(px + nx * 9, py + ny * 9, px + q[2] * 9, py + q[3] * 9)
                    c.drawPath(path, stroke)
                }
                stroke.color = Ink.a(Ink.INK, .6f); ties(8f, .3f, .6f); stroke.color = Ink.INK
                if (i.bridge == Bridge.ARCH) {
                    val lm = max(2, (len / 34).toInt())
                    for (k in 1 until lm) {
                        quad(ax, ay, cx, cy, bx, by, k.toFloat() / lm, q)
                        val flick = if (time == 0.0) 1f else (.7f + .3f * sin(time * 4 + k * 1.7 + i.id).toFloat())
                        val lx = q[0] + q[3] * (w + 4); val ly = q[1] - q[2] * (w + 4)
                        fill.color = Ink.a(Ink.GOLD, .28f * flick); c.drawCircle(lx, ly, 7f, fill)
                        fill.color = Ink.a(Ink.GOLD, .95f * flick); c.drawCircle(lx, ly, 3.2f, fill)
                        stroke.strokeWidth = .8f; c.drawCircle(lx, ly, 3.2f, stroke); stroke.strokeWidth = 1.4f
                    }
                }
            }
        }
        stroke.pathEffect = null
        // A pennant for every year known, planted along the rail towards their island.
        for (y in 0 until min(i.years, 12)) {
            quad(ax, ay, cx, cy, bx, by, 1f - .06f - y * .055f, q)
            val px = q[0] - q[3] * (w + 1); val py = q[1] + q[2] * (w + 1)
            stroke.strokeWidth = 1f; stroke.color = Ink.INK
            c.drawLine(px, py, px, py - 13f, stroke)
            path.reset(); path.moveTo(px, py - 13f); path.lineTo(px + 8f, py - 10.5f); path.lineTo(px, py - 8f); path.close()
            fill.color = if (y % 5 == 4) Ink.GOLD else Ink.RED
            c.drawPath(path, fill)
        }
        stroke.strokeWidth = 1.4f
    }

    private fun ferry(c: Canvas, p: Isle, r: Isle) {
        val mx = (p.x + r.x) / 2; val my = (p.y + r.y) / 2
        // Bow the route sideways, to the side away from your island, so it never runs across it.
        val len = hypot(r.x - p.x, r.y - p.y).coerceAtLeast(1f)
        var nx = -(r.y - p.y) / len; var ny = (r.x - p.x) / len
        if (nx * mx + ny * my < 0) { nx = -nx; ny = -ny }
        val bow = len * .22f + max(0f, Chart.ME_R + 90f - hypot(mx, my)) * 1.6f
        val ox = mx + nx * bow; val oy = my + ny * bow
        path.reset(); path.moveTo(p.x, p.y); path.quadTo(ox, oy, r.x, r.y)
        stroke.color = Ink.RED; stroke.strokeWidth = 1.5f; stroke.pathEffect = dash3
        c.drawPath(path, stroke)
        stroke.pathEffect = null
        quad(p.x, p.y, ox, oy, r.x, r.y, .5f, q)
        Glyphs.boat(c, q[0], q[1] - 4, 1f, Ink.RED)
    }

    private fun puff(): Bitmap = puff ?: run {
        val n = 128
        val b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(n / 2f, n / 2f, n / 2f, intArrayOf(Ink.a(Ink.PAPER, .95f), Ink.a(Ink.PAPER, .55f), Ink.a(Ink.PAPER, 0f)), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
        Canvas(b).drawCircle(n / 2f, n / 2f, n / 2f, p)
        b.also { puff = it }
    }

    private val src = Rect(0, 0, 128, 128)
    private val dst = RectF()

    /** The edge of the known: paper thickening into fog past the outermost islands. */
    private fun fog(c: Canvas, r: Float) {
        fill.shader = RadialGradient(0f, 0f, r * 2.2f, intArrayOf(0, 0, Ink.a(Ink.PAPER, .82f), Ink.PAPER), floatArrayOf(0f, .45f, .56f, .75f), Shader.TileMode.CLAMP)
        c.drawCircle(0f, 0f, r * 4f, fill)
        fill.shader = null
        val rnd = Random(7)
        val pf = puff()
        for (k in 0 until 70) {
            val a = rnd.nextDouble() * 2 * PI
            val rr = r * (1.0 + rnd.nextDouble() * .32)
            val s = 50f + rnd.nextFloat() * 70f
            val x = (cos(a) * rr).toFloat(); val y = (sin(a) * rr * 1.1).toFloat()
            dst.set(x - s, y - s * .7f, x + s, y + s * .7f)
            c.drawBitmap(pf, src, dst, bmp)
        }
    }

    private fun compass(c: Canvas, x: Float, y: Float, r: Float, north: String) {
        c.save(); c.translate(x, y)
        stroke.color = Ink.a(Ink.INK, .5f); stroke.strokeWidth = 1f
        c.drawCircle(0f, 0f, r, stroke)
        stroke.pathEffect = dash2; stroke.color = Ink.INK
        c.drawCircle(0f, 0f, r * .86f, stroke)
        stroke.pathEffect = null
        for (k in 0 until 8) {
            val a = k * PI / 4
            val l = if (k % 2 == 1) r * .6f else r
            val wd = if (k % 2 == 1) r * .11f else r * .18f
            val co = cos(a).toFloat(); val si = sin(a).toFloat()
            path.reset(); path.moveTo(0f, 0f); path.lineTo(-si * wd, co * wd); path.lineTo(co * l, si * l); path.close()
            fill.color = Ink.INK; c.drawPath(path, fill)
            path.reset(); path.moveTo(0f, 0f); path.lineTo(si * wd, -co * wd); path.lineTo(co * l, si * l); path.close()
            fill.color = Ink.PAPER; c.drawPath(path, fill); stroke.strokeWidth = .8f; c.drawPath(path, stroke)
        }
        text.typeface = serif; text.isFakeBoldText = true; text.textSize = r * .38f; text.color = Ink.RED
        c.drawText(north, 0f, -r - r * .14f, text)
        text.isFakeBoldText = false
        c.restore()
    }
}
