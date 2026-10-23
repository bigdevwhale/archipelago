package app.mostki

import app.mostki.data.Backup
import app.mostki.data.DemoSeed
import app.mostki.domain.Bridge
import app.mostki.domain.Chart
import app.mostki.domain.Contact
import app.mostki.domain.DAY
import app.mostki.domain.Expedition
import app.mostki.domain.Island
import app.mostki.domain.Ladder
import app.mostki.domain.Rules
import app.mostki.domain.Sea
import app.mostki.domain.Sortie
import app.mostki.domain.State
import app.mostki.domain.Tasks
import app.mostki.domain.Touch
import app.mostki.domain.Years
import app.mostki.notify.Nudge
import app.mostki.ui.Words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.hypot

@RunWith(RobolectricTestRunner::class)
class RulesTest {
    private val zone = ZoneId.of("Europe/Prague")
    private val now = LocalDate.of(2026, 10, 24).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private fun ago(d: Int) = now - d * DAY

    private fun isle(id: Long, made: Int, anchored: Int? = made - 1) = Island(id, "I$id", "", emptyList(), null, id.toDouble(), ago(made), anchored?.let { ago(it) })

    @Test fun bridgesFollowHall() {
        assertEquals(Bridge.ROPE, Bridge.of(49 * 60 + 59))
        assertEquals(Bridge.PLANKS, Bridge.of(50 * 60))
        assertEquals(Bridge.STONE, Bridge.of(90 * 60))
        assertEquals(Bridge.ARCH, Bridge.of(200 * 60))
    }

    @Test fun seaStates() {
        val s = State(
            islands = listOf(isle(1, 2, null), isle(2, 9, null), isle(3, 100), isle(4, 300), isle(5, 100)),
            contacts = listOf(Contact(1, 3, ago(40), 60, ""), Contact(2, 4, ago(200), 60, ""), Contact(3, 5, ago(3), 60, "")),
        )
        assertEquals(Sea.NEW, Rules.sea(s, s.islands[0], now, zone))
        assertEquals(Sea.DRIFTING, Rules.sea(s, s.islands[1], now, zone))
        assertEquals(Sea.CREAKING, Rules.sea(s, s.islands[2], now, zone))
        assertEquals(Sea.FOG, Rules.sea(s, s.islands[3], now, zone))
        assertEquals(Sea.CALM, Rules.sea(s, s.islands[4], now, zone))
    }

    @Test fun aMessageBringsAnIslandBackFromTheFog() {
        val i = isle(1, 400)
        val s = State(listOf(i), listOf(Contact(1, 1, ago(300), 60, ""), Contact(2, 1, ago(1), 0, "", Touch.WROTE)))
        assertEquals(Sea.CREAKING, Rules.sea(s, i, now, zone))
        assertEquals(1, Rules.returns(s).size)
        assertEquals(1, Expedition.OUT_OF_FOG.progress(s, zone))
    }

    @Test fun theLadderCountsTriesNotAnswers() {
        val s = State(sorties = (0 until 7).map { Sortie(it.toLong(), ago(20 - it), it / 3, 0, it % 2 == 0) })
        assertEquals(2, Ladder.rung(s))
        assertEquals(1, Ladder.tries(s))
        assertEquals(3, Ladder.shells(s))
        val top = State(sorties = (0 until 40).map { Sortie(it.toLong(), ago(50 - it), 0, 0, true) })
        assertEquals(Ladder.TOP, Ladder.rung(top))
        assertTrue(Ladder.task(top, LocalDate.of(2026, 1, 1), 3) in Tasks.open.indices)
    }

    @Test fun islandsNeverOverlap() {
        val s = State(islands = (1L..30L).map { Island(it, "I$it", "", emptyList(), null, (it % 5) * .3, ago(10), ago(9)) })
        val p = Chart.place(s, now, zone)
        for (a in s.islands) for (b in s.islands) if (a.id < b.id) {
            val d = hypot(p.getValue(a.id).x - p.getValue(b.id).x, p.getValue(a.id).y - p.getValue(b.id).y)
            assertTrue("${a.id}-${b.id} at $d", d > Chart.radius(s, a) + Chart.radius(s, b))
        }
    }

    @Test fun yearsAndAnniversaries() {
        val i = Island(1, "A", "", emptyList(), null, 0.0, LocalDate.of(2024, 10, 24).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(), ago(700))
        assertEquals(2, Years.known(i, now, zone))
        assertEquals(listOf(i to 2), Years.anniversaries(State(listOf(i)), now, zone))
        val ask = Nudge.pick(State(listOf(i)), now, zone, emptySet(), Words(true))
        assertNotNull(ask)
        assertNull(Nudge.pick(State(listOf(i)), now, zone, setOf(ask!!.key), Words(true)))
    }

    @Test fun demoYearsAddUp() {
        val s = DemoSeed.build(now, true)
        val ys = Years.summaries(s, zone)
        assertEquals(s.contacts.filter { it.kind == Touch.MET }.sumOf { it.minutes }, ys.sumOf { it.minutes })
        assertEquals(Bridge.ARCH, Bridge.of(s.minutes(3)))
        assertEquals(Sea.NEW, Rules.sea(s, s.island(4)!!, now, zone))
        assertEquals(Sea.FOG, Rules.sea(s, s.island(6)!!, now, zone))
        assertEquals(Sea.CREAKING, Rules.sea(s, s.island(2)!!, now, zone))
    }

    @Test fun backupRoundTrip() {
        val s = DemoSeed.build(now, false)
        val back = Backup.read(Backup.write(s, now))
        assertEquals(s.islands, back.islands)
        assertEquals(s.contacts, back.contacts)
        assertEquals(s.ferries, back.ferries)
        assertEquals(s.memos, back.memos)
        assertEquals(s.sorties, back.sorties)
    }
}
