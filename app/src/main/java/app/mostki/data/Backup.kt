package app.mostki.data

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
import org.json.JSONArray
import org.json.JSONObject

/**
 * The whole chart as one JSON file, so years of it survive a new phone. Plain field names, a version
 * number, nothing that can't be read by eye.
 */
object Backup {
    const val VERSION = 1

    fun write(s: State, at: Long): String {
        val o = JSONObject()
        o.put("app", "mostki"); o.put("version", VERSION); o.put("at", at)
        o.put("islands", JSONArray(s.islands.map { i ->
            JSONObject().put("id", i.id).put("name", i.name).put("detail", i.detail).put("traits", JSONArray(i.traits.map { it.name }))
                .put("harbor", i.harbor ?: JSONObject.NULL).put("angle", i.angle).put("made", i.made).put("anchoredAt", i.anchoredAt ?: JSONObject.NULL)
        }))
        o.put("contacts", JSONArray(s.contacts.map { c ->
            JSONObject().put("id", c.id).put("island", c.island).put("at", c.at).put("minutes", c.minutes).put("what", c.what)
                .put("kind", c.kind.name).put("gathering", c.gathering ?: JSONObject.NULL)
        }))
        o.put("ferries", JSONArray(s.ferries.map { JSONObject().put("id", it.id).put("a", it.a).put("b", it.b).put("at", it.at) }))
        o.put("harbors", JSONArray(s.harbors.map { JSONObject().put("id", it.id).put("name", it.name).put("made", it.made) }))
        o.put("visits", JSONArray(s.visits.map { JSONObject().put("id", it.id).put("harbor", it.harbor).put("at", it.at) }))
        o.put("sorties", JSONArray(s.sorties.map { JSONObject().put("id", it.id).put("at", it.at).put("rung", it.rung).put("task", it.task).put("ok", it.ok) }))
        o.put("memos", JSONArray(s.memos.map { JSONObject().put("id", it.id).put("island", it.island).put("text", it.text).put("at", it.at).put("done", it.done) }))
        return o.toString(1)
    }

    /** Reads a backup; throws on anything that isn't one. */
    fun read(text: String): State {
        val o = JSONObject(text)
        require(o.optString("app") == "mostki") { "not a Mostki backup" }
        require(o.optInt("version") in 1..VERSION) { "backup from a newer version" }
        fun <T> list(k: String, f: (JSONObject) -> T): List<T> = o.optJSONArray(k)?.let { a -> (0 until a.length()).map { f(a.getJSONObject(it)) } }.orEmpty()
        fun JSONObject.longOrNull(k: String) = if (isNull(k)) null else optLong(k)
        return State(
            islands = list("islands") { j ->
                val tr = j.optJSONArray("traits")
                Island(j.getLong("id"), j.getString("name"), j.optString("detail"),
                    (0 until (tr?.length() ?: 0)).mapNotNull { n -> Trait.entries.firstOrNull { it.name == tr!!.getString(n) } },
                    j.longOrNull("harbor"), j.getDouble("angle"), j.getLong("made"), j.longOrNull("anchoredAt"))
            },
            contacts = list("contacts") { j ->
                Contact(j.getLong("id"), j.getLong("island"), j.getLong("at"), j.getInt("minutes"), j.optString("what"),
                    Touch.entries.firstOrNull { it.name == j.optString("kind") } ?: Touch.MET, j.longOrNull("gathering"))
            },
            ferries = list("ferries") { Ferry(it.getLong("id"), it.getLong("a"), it.getLong("b"), it.getLong("at")) },
            harbors = list("harbors") { Harbor(it.getLong("id"), it.getString("name"), it.getLong("made")) },
            visits = list("visits") { Visit(it.getLong("id"), it.getLong("harbor"), it.getLong("at")) },
            sorties = list("sorties") { Sortie(it.getLong("id"), it.getLong("at"), it.getInt("rung"), it.getInt("task"), it.getBoolean("ok")) },
            memos = list("memos") { Memo(it.getLong("id"), it.getLong("island"), it.getString("text"), it.getLong("at"), it.optBoolean("done")) },
        )
    }
}
