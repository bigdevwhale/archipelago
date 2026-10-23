package app.mostki.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.mostki.domain.Rules
import app.mostki.domain.State
import app.mostki.domain.Tasks
import app.mostki.domain.Touch
import app.mostki.domain.date
import app.mostki.ui.Home
import app.mostki.ui.Nav
import app.mostki.ui.Overlay
import app.mostki.ui.Words
import app.mostki.ui.components.Heading
import app.mostki.ui.theme.C
import app.mostki.ui.theme.T

class Entry(val at: Long, val text: String, val island: Long? = null)

/** Everything that happened on the chart, as lines in a ship's log. */
fun entries(s: State, w: Words): List<Entry> {
    val out = ArrayList<Entry>()
    fun name(id: Long) = s.island(id)?.name ?: "?"
    for (i in s.islands) out += Entry(i.made, w.t("Новый остров: ${i.name}", "New island: ${i.name}") + if (i.detail.isNotBlank()) " — «${i.detail}»" else "", i.id)
    val returns = Rules.returns(s).map { it.id }.toSet()
    val firstWrote = s.contacts.filter { it.kind == Touch.WROTE }.groupBy { it.island }.mapValues { (_, l) -> l.minOf { it.at } }
    for (c in s.contacts) when {
        c.gathering != null -> Unit
        c.kind == Touch.WROTE -> out += Entry(c.at, when {
            c.id in returns -> w.t("Из тумана: ${name(c.island)} — первое сообщение за полгода", "Out of the fog: ${name(c.island)} — the first word in half a year")
            firstWrote[c.island] == c.at && s.island(c.island)?.anchoredAt == c.at -> w.t("⛵ Кораблик: ${name(c.island)}. Остров на якоре", "⛵ Paper boat: ${name(c.island)}. The island is anchored")
            else -> w.t("Сообщение: ${name(c.island)}", "A message: ${name(c.island)}")
        }, c.island)
        else -> out += Entry(c.at, listOf(name(c.island), w.dur(c.minutes), c.what).filter { it.isNotBlank() }.joinToString(" · "), c.island)
    }
    for ((_, cs) in Rules.gatherings(s)) {
        val f = cs.first()
        out += Entry(f.at, w.t("✦ Маяк: ", "✦ Lighthouse: ") + cs.joinToString(", ") { name(it.island) } + " · ${w.dur(f.minutes)}" + if (f.what.isNotBlank()) " · ${f.what}" else "")
    }
    val gatherAt = Rules.gatherings(s).keys
    for (f in s.ferries) if (f.at !in gatherAt) out += Entry(f.at, w.t("Паром: ${name(f.a)} — ${name(f.b)}", "Ferry: ${name(f.a)} — ${name(f.b)}"))
    for (o in s.sorties) {
        val r = Tasks.rungs.getOrNull(o.rung)?.name?.invoke(w.ru) ?: w.t("Открытое море", "Open sea")
        out += Entry(o.at, w.t("Вылазка «$r»: ", "Sortie \"$r\": ") + (if (o.ok) w.t("получилось", "it worked") else w.t("отказ — ракушка", "a no — a shell")) +
            Tasks.pool(o.rung).getOrNull(o.task)?.let { " (${it(w.ru).replaceFirstChar { ch -> ch.lowercase() }})" }.orEmpty())
    }
    for (v in s.visits) out += Entry(v.at, "⚓ " + (s.harbor(v.harbor)?.name ?: "?"))
    return out.sortedByDescending { it.at }
}

@Composable
fun JournalScreen(h: Home, nav: Nav) {
    val w = h.w
    val list = remember(h.s, w.ru) { entries(h.s, w) }
    val groups = remember(list, h.today) { list.groupBy { it.at.date(h.zone) }.entries.toList() }
    Column(Modifier.fillMaxSize()) {
        Heading(w.t("Судовой журнал", "Ship's log"), w.t("С кем и когда — день за днём, год за годом.", "Who you spoke with and when — day by day, year by year."))
        if (list.isEmpty()) Text(w.t("Журнал пуст. Первая запись появится после первой вылазки или первого острова.", "The log is empty. The first line comes with your first sortie or island."),
            style = T.Note, modifier = Modifier.padding(18.dp))
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            for ((d, es) in groups) {
                item {
                    Text(w.date(d, h.today), style = T.HandBody.copy(color = C.red), modifier = Modifier.padding(top = 14.dp, bottom = 2.dp))
                }
                items(es) { e ->
                    Column(Modifier.fillMaxWidth().then(if (e.island != null && h.s.island(e.island) != null) Modifier.clickable { nav.open(Overlay.Isle(e.island)) } else Modifier)) {
                        Text(e.text, style = T.Body.copy(fontSize = T.Muted.fontSize, lineHeight = T.Muted.lineHeight), modifier = Modifier.padding(vertical = 6.dp))
                        HorizontalDivider(color = C.faint.copy(alpha = .5f))
                    }
                }
            }
            item { Text("", modifier = Modifier.padding(20.dp)) }
        }
    }
}
