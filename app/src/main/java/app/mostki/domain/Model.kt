package app.mostki.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What someone loves, drawn as a little engraved landmark on their island. */
enum class Trait { MOUNTAINS, FOREST, COFFEE, BOOKS, MUSIC, SPORT, SEA, PETS, ART, GAMES, TRAVEL, GARDEN }

/** One person on the chart. [angle] is their bearing from your island, chosen once when they're drawn in. */
data class Island(
    val id: Long,
    val name: String,
    val detail: String,
    val traits: List<Trait>,
    val harbor: Long?,
    val angle: Double,
    val made: Long,
    /** When the second step happened — a paper boat sent, or a second meeting. Null while the island can still drift off. */
    val anchoredAt: Long? = null,
)

enum class Touch {
    /** Time spent together: it lays planks. */
    MET,
    /** A message sent first: it keeps the island out of the fog, but lays nothing. */
    WROTE,
}

data class Contact(
    val id: Long,
    val island: Long,
    val at: Long,
    val minutes: Int,
    val what: String,
    val kind: Touch = Touch.MET,
    /** Set when the meeting was one evening under the lighthouse with others. */
    val gathering: Long? = null,
)

/** Two of your islands introduced to each other. */
data class Ferry(val id: Long, val a: Long, val b: Long, val at: Long)

/** A place you keep coming back to, where the same faces become people. */
data class Harbor(val id: Long, val name: String, val made: Long)

data class Visit(val id: Long, val harbor: Long, val at: Long)

/** One try on the ladder of courage. A refusal counts just the same, and brings a shell. */
data class Sortie(val id: Long, val at: Long, val rung: Int, val task: Int, val ok: Boolean)

/** Something to remember for the next meeting. */
data class Memo(val id: Long, val island: Long, val text: String, val at: Long, val done: Boolean = false)

data class State(
    val islands: List<Island> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val ferries: List<Ferry> = emptyList(),
    val harbors: List<Harbor> = emptyList(),
    val visits: List<Visit> = emptyList(),
    val sorties: List<Sortie> = emptyList(),
    val memos: List<Memo> = emptyList(),
) {
    private val byIsland: Map<Long, List<Contact>> by lazy { contacts.groupBy { it.island } }

    fun island(id: Long) = islands.firstOrNull { it.id == id }
    fun contacts(id: Long): List<Contact> = byIsland[id].orEmpty()
    fun memos(id: Long) = memos.filter { it.island == id }
    fun harbor(id: Long?) = harbors.firstOrNull { it.id == id }

    fun minutes(id: Long): Int = contacts(id).filter { it.kind == Touch.MET }.sumOf { it.minutes }

    /** The last time you were together; the drawing-in counts as the first. */
    fun lastMet(i: Island): Long = contacts(i.id).filter { it.kind == Touch.MET }.maxOfOrNull { it.at }?.coerceAtLeast(i.made) ?: i.made

    /** The last sign of life either way: a meeting or a message. */
    fun lastTouch(i: Island): Long = contacts(i.id).maxOfOrNull { it.at }?.coerceAtLeast(i.made) ?: i.made

    fun linked(a: Long, b: Long) = ferries.any { (it.a == a && it.b == b) || (it.a == b && it.b == a) }
}

const val DAY = 24 * 3600_000L

fun Long.date(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/** Whole days from [from] to [to], by the calendar. */
fun days(from: Long, to: Long, zone: ZoneId): Int = (to.date(zone).toEpochDay() - from.date(zone).toEpochDay()).toInt()
