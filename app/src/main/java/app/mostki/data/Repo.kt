package app.mostki.data

import app.mostki.domain.Chart
import app.mostki.domain.Contact
import app.mostki.domain.Ferry
import app.mostki.domain.Harbor
import app.mostki.domain.Island
import app.mostki.domain.Memo
import app.mostki.domain.Sortie
import app.mostki.domain.State
import app.mostki.domain.Touch
import app.mostki.domain.Trait
import app.mostki.domain.Visit
import app.mostki.domain.days
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId

/** The chart on disk, and the live picture of it in [state]. */
class Repo(db: ChartDb, private val clock: () -> Long = System::currentTimeMillis, private val zone: () -> ZoneId = ZoneId::systemDefault) {
    private val dao = db.dao()
    private val lock = Mutex()
    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state

    suspend fun refresh(): State = lock.withLock { load().also { _state.value = it } }

    private suspend fun load() = State(
        islands = dao.islands().map { it.toIsland() },
        contacts = dao.contacts().map { Contact(it.id, it.island, it.at, it.minutes, it.what, Touch.valueOf(it.kind), it.gathering) },
        ferries = dao.ferries().map { Ferry(it.id, it.a, it.b, it.at) },
        harbors = dao.harbors().map { Harbor(it.id, it.name, it.made) },
        visits = dao.visits().map { Visit(it.id, it.harbor, it.at) },
        sorties = dao.sorties().map { Sortie(it.id, it.at, it.rung, it.task, it.ok) },
        memos = dao.memos().map { Memo(it.id, it.island, it.text, it.at, it.done) },
    )

    private suspend fun current(): State = _state.value ?: load()

    private suspend fun change(block: suspend (State) -> Unit) = lock.withLock {
        block(current()); _state.value = load()
    }

    /** Draws a new island in; a first talk, if it lasted, already counts as time together. */
    suspend fun draw(name: String, detail: String, traits: List<Trait>, harbor: Long?, minutes: Int, at: Long = clock()): Long {
        var id = 0L
        change { s ->
            id = dao.insert(IslandRow(name = name.trim(), detail = detail.trim(), traits = traits.joinToString(",") { it.name }, harbor = harbor,
                angle = Chart.bearing(s, at, zone()), made = at, anchoredAt = null))
            if (minutes > 0) dao.insert(ContactRow(island = id, at = at, minutes = minutes, what = "", kind = Touch.MET.name, gathering = null))
        }
        return id
    }

    suspend fun edit(i: Island) = change { dao.update(i.toRow()) }

    suspend fun forget(id: Long) = change { dao.forget(id) }

    /** Time together. A meeting on any later day than the first is the second step, and anchors the island. */
    suspend fun meet(island: Long, minutes: Int, what: String, at: Long = clock()) = change { s ->
        dao.insert(ContactRow(island = island, at = at, minutes = minutes, what = what.trim(), kind = Touch.MET.name, gathering = null))
        val i = s.island(island) ?: return@change
        if (days(i.made, at, zone()) >= 1) dao.anchor(island, at)
    }

    /** A message sent first: the paper boat for a new island, or the way back out of the fog. */
    suspend fun wrote(island: Long, at: Long = clock()) = change {
        dao.insert(ContactRow(island = island, at = at, minutes = 0, what = "", kind = Touch.WROTE.name, gathering = null))
        dao.anchor(island, at)
    }

    suspend fun undo(contact: Long) = change { dao.deleteContact(contact) }

    /** The lighthouse: one evening for several islands, who all meet each other too. */
    suspend fun gather(islands: List<Long>, minutes: Int, what: String, at: Long = clock()) = change { s ->
        for (id in islands) {
            dao.insert(ContactRow(island = id, at = at, minutes = minutes, what = what.trim(), kind = Touch.MET.name, gathering = at))
            val i = s.island(id) ?: continue
            if (days(i.made, at, zone()) >= 1) dao.anchor(id, at)
        }
        for (a in islands.indices) for (b in a + 1 until islands.size)
            if (!s.linked(islands[a], islands[b])) dao.insert(FerryRow(a = islands[a], b = islands[b], at = at))
    }

    suspend fun ferry(a: Long, b: Long, at: Long = clock()) = change { s -> if (a != b && !s.linked(a, b)) dao.insert(FerryRow(a = a, b = b, at = at)) }

    suspend fun memo(island: Long, text: String, at: Long = clock()) = change { if (text.isNotBlank()) dao.insert(MemoRow(island = island, text = text.trim(), at = at, done = false)) }

    suspend fun tick(m: Memo) = change { dao.update(MemoRow(m.id, m.island, m.text, m.at, !m.done)) }

    suspend fun dropMemo(id: Long) = change { dao.deleteMemo(id) }

    suspend fun harbor(name: String, at: Long = clock()): Long {
        var id = 0L
        change { if (name.isNotBlank()) id = dao.insert(HarborRow(name = name.trim(), made = at)) }
        return id
    }

    suspend fun closeHarbor(id: Long) = change { dao.closeHarbor(id) }

    suspend fun visit(harbor: Long, at: Long = clock()) = change { dao.insert(VisitRow(harbor = harbor, at = at)) }

    suspend fun sortie(rung: Int, task: Int, ok: Boolean, at: Long = clock()) = change { dao.insert(SortieRow(at = at, rung = rung, task = task, ok = ok)) }

    /** A backup read back in: the chart on this phone is replaced by it whole. */
    suspend fun restore(s: State) = change { dao.clear(); put(s) }

    /** For the demo: a whole chart straight in, ids as given. */
    suspend fun seed(s: State) = change { put(s) }

    private suspend fun put(s: State) {
        for (i in s.islands) dao.insert(i.toRow())
        for (c in s.contacts) dao.insert(ContactRow(c.id, c.island, c.at, c.minutes, c.what, c.kind.name, c.gathering))
        for (f in s.ferries) dao.insert(FerryRow(f.id, f.a, f.b, f.at))
        for (h in s.harbors) dao.insert(HarborRow(h.id, h.name, h.made))
        for (v in s.visits) dao.insert(VisitRow(v.id, v.harbor, v.at))
        for (o in s.sorties) dao.insert(SortieRow(o.id, o.at, o.rung, o.task, o.ok))
        for (m in s.memos) dao.insert(MemoRow(m.id, m.island, m.text, m.at, m.done))
    }
}

private fun IslandRow.toIsland() = Island(id, name, detail,
    traits.split(',').mapNotNull { n -> Trait.entries.firstOrNull { it.name == n } }, harbor, angle, made, anchoredAt)

private fun Island.toRow() = IslandRow(id, name, detail, traits.joinToString(",") { it.name }, harbor, angle, made, anchoredAt)
