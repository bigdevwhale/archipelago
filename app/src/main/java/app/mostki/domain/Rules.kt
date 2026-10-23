package app.mostki.domain

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The bridge to an island, by hours together. The thresholds are Jeffrey Hall's (2018): about 50 hours
 * turn an acquaintance into a casual friend, about 90 into a friend, more than 200 into a close friend.
 */
enum class Bridge(val hours: Int) {
    ROPE(0), PLANKS(50), STONE(90), ARCH(200);

    val next: Bridge? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun of(minutes: Int): Bridge = entries.last { minutes >= it.hours * 60 }
    }
}

/** How an island is doing out there. */
enum class Sea {
    CALM,
    /** Drawn in less than a week ago: the second step is still ahead. */
    NEW,
    /** The week passed without a second step; it can still be anchored, late. */
    DRIFTING,
    /** A month without meeting: the bridge sags and creaks. */
    CREAKING,
    /** Half a year without a word either way: the island is lost in the fog, until someone writes first. */
    FOG,
}

object Rules {
    const val KITE_DAYS = 7
    const val CREAK_DAYS = 28
    const val FOG_DAYS = 180

    fun sea(s: State, i: Island, now: Long, zone: ZoneId): Sea = when {
        days(s.lastTouch(i), now, zone) >= FOG_DAYS -> Sea.FOG
        i.anchoredAt == null && days(i.made, now, zone) < KITE_DAYS -> Sea.NEW
        i.anchoredAt == null -> Sea.DRIFTING
        days(s.lastMet(i), now, zone) >= CREAK_DAYS -> Sea.CREAKING
        else -> Sea.CALM
    }

    /** Days left to send the paper boat in time: 7 on the day you met, 1 on the last day. */
    fun kiteDaysLeft(i: Island, now: Long, zone: ZoneId) = KITE_DAYS - days(i.made, now, zone)

    /** Was the second step taken inside the week? */
    fun inTime(i: Island, zone: ZoneId) = i.anchoredAt != null && days(i.made, i.anchoredAt, zone) < KITE_DAYS

    /** How far a creaking bridge has sagged: 0 at a month, 1 at four months. */
    fun sag(s: State, i: Island, now: Long, zone: ZoneId): Float =
        ((days(s.lastMet(i), now, zone) - CREAK_DAYS) / 90f).coerceIn(0f, 1f)

    /** Gatherings: meetings that share a lighthouse evening, with who came. */
    fun gatherings(s: State): Map<Long, List<Contact>> = s.contacts.filter { it.gathering != null }.groupBy { it.gathering!! }

    /** A message sent to an island that had been in the fog: it came back. */
    fun returns(s: State): List<Contact> = s.contacts.filter { c ->
        c.kind == Touch.WROTE && run {
            val i = s.island(c.island) ?: return@run false
            val before = s.contacts(c.island).filter { it.at < c.at }.maxOfOrNull { it.at } ?: i.made
            (c.at - before) >= FOG_DAYS * DAY
        }
    }
}

/**
 * The ladder of courage: six rungs from a nod to an invitation, three tries each, then the open sea.
 * A try is a try whatever the answer — the ladder is about going up to people, not about how they reply.
 */
object Ladder {
    const val TRIES = 3
    val TOP = Tasks.rungs.size

    fun rung(s: State) = min(s.sorties.size / TRIES, TOP)
    fun tries(s: State) = if (rung(s) < TOP) s.sorties.size % TRIES else 0
    fun shells(s: State) = s.sorties.count { !it.ok }

    /** Today's task: the same all day until it's tried or skipped. */
    fun task(s: State, today: LocalDate, skips: Int): Int {
        val pool = Tasks.pool(rung(s))
        val k = (today.toEpochDay() * 7 + s.sorties.size * 3 + skips).toInt()
        return Math.floorMod(k, pool.size)
    }
}

/** Charts to complete: long goals that point at what actually makes friends. */
enum class Expedition(val goal: Int) {
    FIRST_ISLANDS(3), SECOND_TIME(3), PLANKS(1), HARBOR(3), REGULAR(8), FERRYMAN(2),
    LIGHTHOUSE(1), OUT_OF_FOG(1), BRAVE(10), LADDER(18), STONE(1), DOZEN(12), ARCH(1);

    fun progress(s: State, zone: ZoneId): Int = when (this) {
        FIRST_ISLANDS, DOZEN -> s.islands.size
        SECOND_TIME -> s.islands.count { Rules.inTime(it, zone) }
        PLANKS -> s.islands.count { s.minutes(it.id) >= Bridge.PLANKS.hours * 60 }
        STONE -> s.islands.count { s.minutes(it.id) >= Bridge.STONE.hours * 60 }
        ARCH -> s.islands.count { s.minutes(it.id) >= Bridge.ARCH.hours * 60 }
        HARBOR -> s.islands.mapNotNull { it.harbor }.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
        REGULAR -> s.visits.groupingBy { it.harbor }.eachCount().values.maxOrNull() ?: 0
        FERRYMAN -> s.ferries.size
        LIGHTHOUSE -> Rules.gatherings(s).values.count { it.size >= 3 }
        OUT_OF_FOG -> Rules.returns(s).size
        BRAVE -> Ladder.shells(s)
        LADDER -> s.sorties.size
    }.coerceAtMost(goal)

    fun done(s: State, zone: ZoneId) = progress(s, zone) >= goal
}

/** Where the islands lie: closer the more hours you share, spread round the compass, never on top of each other. */
object Chart {
    data class Pt(val x: Float, val y: Float)

    const val ME_R = 56f

    fun radius(s: State, i: Island): Float = 30f + min(i.traits.size, 4) * 3f + min(s.minutes(i.id) / 60f, 200f) / 200f * 8f

    /** The ring the fog begins at, which grows with the archipelago. */
    fun span(n: Int) = 230f * max(1f, sqrt(n / 14f))

    fun fogRing(n: Int) = 235f + span(n) + 70f

    fun place(s: State, now: Long, zone: ZoneId): Map<Long, Pt> {
        val n = s.islands.size
        val span = span(n)
        // Even the closest friend sits far enough off for a long arch with its lanterns.
        val near = 235f
        val r = HashMap<Long, Float>()
        val pts = s.islands.map { i ->
            val sea = Rules.sea(s, i, now, zone)
            val h = min(s.minutes(i.id) / 60f, 220f)
            var d = near + (1 - h / 220f) * span
            if (sea == Sea.DRIFTING) d += 50f
            if (sea == Sea.FOG) d = fogRing(n) + 160f
            r[i.id] = radius(s, i)
            i.id to floatArrayOf((cos(i.angle) * d).toFloat(), (sin(i.angle) * d * 1.15).toFloat())
        }
        // Push overlapping islands apart, labels included, keeping them clear of your own.
        repeat(240) {
            for (a in pts.indices) for (b in a + 1 until pts.size) {
                val p = pts[a].second; val q = pts[b].second
                var dx = q[0] - p[0]; var dy = q[1] - p[1]
                // Two islands on the very same spot: part them along a bearing of their own.
                if (abs(dx) + abs(dy) < .5f) { dx = cos(b * 2.4).toFloat(); dy = sin(b * 2.4).toFloat() }
                val d = hypot(dx, dy).coerceAtLeast(.01f)
                val want = r.getValue(pts[a].first) + r.getValue(pts[b].first) + 46f
                if (d < want) {
                    val push = (want - d) / 2 / d
                    p[0] -= dx * push; p[1] -= dy * push
                    q[0] += dx * push; q[1] += dy * push
                }
            }
            for ((id, p) in pts) {
                val d = hypot(p[0], p[1])
                val min = ME_R + r.getValue(id) + 60f
                if (d < min) { p[0] *= min / d; p[1] *= min / d }
            }
        }
        return pts.associate { (id, p) -> id to Pt(p[0], p[1]) }
    }

    /** The fog begins past the farthest island still in sight, never on top of it. */
    fun fog(s: State, pts: Map<Long, Pt>, now: Long, zone: ZoneId): Float {
        var far = fogRing(s.islands.size)
        for (i in s.islands) if (Rules.sea(s, i, now, zone) != Sea.FOG) {
            val p = pts[i.id] ?: continue
            far = max(far, hypot(p.x, p.y / 1.1f) + radius(s, i) + 70f)
        }
        return far
    }

    /** A bearing for a new island: the middle of the widest empty sector. */
    fun bearing(s: State, now: Long, zone: ZoneId): Double {
        val used = s.islands.filter { Rules.sea(s, it, now, zone) != Sea.FOG }.map { it.angle }
        if (used.isEmpty()) return -PI / 2.6
        var best = 0.0; var bestGap = -1.0
        for (k in 0 until 72) {
            val a = -PI + k * PI / 36
            val gap = used.minOf { u -> abs(atan2(sin(a - u), cos(a - u))) }
            if (gap > bestGap + 1e-9) { bestGap = gap; best = a }
        }
        return best
    }
}

/** The long view: the chart is meant to be kept for years. */
object Years {
    /** Whole years since the island was drawn in. */
    fun known(i: Island, now: Long, zone: ZoneId): Int {
        val a = i.made.date(zone); val b = now.date(zone)
        var y = b.year - a.year
        if (b.dayOfYear < a.withYear(b.year).dayOfYear) y--
        return y.coerceAtLeast(0)
    }

    /** Islands whose day of meeting falls today, a year or more ago. */
    fun anniversaries(s: State, now: Long, zone: ZoneId): List<Pair<Island, Int>> {
        val d = now.date(zone)
        return s.islands.mapNotNull { i ->
            val m = i.made.date(zone)
            val y = d.year - m.year
            if (y >= 1 && m.month == d.month && m.dayOfMonth == d.dayOfMonth) i to y else null
        }
    }

    class Summary(
        val year: Int,
        val drawn: List<Island>,
        val minutes: Int,
        val meetings: Int,
        val people: Int,
        val ferries: Int,
        val gatherings: Int,
        val shells: Int,
        val sorties: Int,
        /** Bridges that reached a new kind that year, with the kind. */
        val raised: List<Pair<Island, Bridge>>,
        /** Who you spent most time with. */
        val most: List<Pair<Island, Int>>,
    )

    /** Each year on the chart, newest first. */
    fun summaries(s: State, zone: ZoneId): List<Summary> {
        val years = (s.islands.map { it.made } + s.contacts.map { it.at } + s.sorties.map { it.at }).map { it.date(zone).year }.distinct().sortedDescending()
        return years.map { y -> summary(s, y, zone) }
    }

    fun summary(s: State, y: Int, zone: ZoneId): Summary {
        fun inY(t: Long) = t.date(zone).year == y
        val met = s.contacts.filter { it.kind == Touch.MET && inY(it.at) }
        val raised = s.islands.mapNotNull { i ->
            var before = 0
            var got: Bridge? = null
            for (c in s.contacts(i.id).filter { it.kind == Touch.MET }.sortedBy { it.at }) {
                val was = Bridge.of(before); before += c.minutes
                val now = Bridge.of(before)
                if (now != was && inY(c.at)) got = now
            }
            got?.let { i to it }
        }
        return Summary(
            year = y,
            drawn = s.islands.filter { inY(it.made) },
            minutes = met.sumOf { it.minutes },
            meetings = met.map { it.gathering ?: -it.id }.distinct().size,
            people = met.map { it.island }.distinct().size,
            ferries = s.ferries.count { inY(it.at) },
            gatherings = Rules.gatherings(s).count { (_, cs) -> inY(cs.first().at) },
            shells = s.sorties.count { !it.ok && inY(it.at) },
            sorties = s.sorties.count { inY(it.at) },
            raised = raised,
            most = met.groupBy { it.island }.mapNotNull { (id, cs) -> s.island(id)?.let { it to cs.sumOf { c -> c.minutes } } }.sortedByDescending { it.second }.take(3),
        )
    }
}
