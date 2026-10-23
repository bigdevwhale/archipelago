package app.mostki.ui.atlas

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.mostki.container
import app.mostki.domain.Bridge
import app.mostki.domain.Expedition
import app.mostki.domain.Years
import app.mostki.domain.date
import app.mostki.domain.days
import app.mostki.ui.Home
import app.mostki.ui.Nav
import app.mostki.ui.components.Card
import app.mostki.ui.components.Chip
import app.mostki.ui.components.Field
import app.mostki.ui.components.Gap
import app.mostki.ui.components.Heading
import app.mostki.ui.components.Label
import app.mostki.ui.components.LineButton
import app.mostki.ui.components.Section
import app.mostki.ui.theme.C
import app.mostki.ui.theme.Shapes
import app.mostki.ui.theme.T

@Composable
fun AtlasScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val s = h.s
    val done = Expedition.entries.count { it.done(s, h.zone) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Heading(w.t("Атлас", "Atlas"), w.t("Гавани, экспедиции и годы на карте.", "Harbours, expeditions and the years on the chart."))
        Column(Modifier.padding(horizontal = 18.dp)) {
            val total = s.contacts.sumOf { it.minutes }
            Gap(6)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (b in Bridge.entries.asReversed()) {
                    val n = s.islands.count { Bridge.of(s.minutes(it.id)) == b }
                    Column(Modifier.weight(1f).border(1.dp, C.faint).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$n", style = T.Display)
                        Text(w.bond(b), style = T.Small, maxLines = 2)
                    }
                }
            }
            Gap(6)
            Text(w.t("Всего вместе: ${w.total(total)}.", "Together in all: ${w.total(total)}."), style = T.Note)

            Section(w.t("Гавани", "Harbours"), w.t("Места, куда ты возвращаешься. Дружба чаще всего рождается там, где одни и те же лица.",
                "Places you keep coming back to. Friendships are born most often where the same faces keep turning up."))
            for (hb in s.harbors) {
                val visits = s.visits.filter { it.harbor == hb.id }
                val from = s.islands.filter { it.harbor == hb.id }
                val last = visits.maxOfOrNull { it.at }
                val here = last != null && last.date(h.zone) == h.today
                var close by remember(hb.id) { mutableStateOf(false) }
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚓ ${hb.name}", style = T.Title, modifier = Modifier.weight(1f))
                        Chip(if (here) w.t("сегодня ✓", "today ✓") else w.t("я здесь", "I'm here"), here) {
                            if (!here) {
                                c.write { repo.visit(hb.id) }
                                Toast.makeText(ctx, w.t("Отмечено. Знакомые лица — начало дружбы.", "Noted. Familiar faces are where friendship starts."), Toast.LENGTH_SHORT).show()
                            }
                        }
                        Text("×", style = T.Title.copy(color = if (close) C.red else C.faint), modifier = Modifier.clickable {
                            if (!close) close = true else c.write { repo.closeHarbor(hb.id) }
                        }.padding(start = 10.dp, end = 2.dp))
                    }
                    if (close) Text(w.t("Нажми × ещё раз, чтобы закрыть гавань. Острова останутся.", "Tap × again to close the harbour. The islands stay."), style = T.Small.copy(color = C.red))
                    Text(w.t("посещений: ${visits.size}", "visits: ${visits.size}") +
                        (last?.let { w.t(" · последнее — ${w.ago(days(it, h.now, h.zone))}", " · last ${w.ago(days(it, h.now, h.zone))}") } ?: ""), style = T.Muted)
                    if (from.isNotEmpty()) Text(w.t("Острова отсюда: ", "Islands from here: ") + from.joinToString(", ") { it.name }, style = T.Muted.copy(color = C.ink))
                }
                Gap(8)
            }
            var name by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(name, { name = it }, w.t("новая гавань: клуб, секция, хор…", "new harbour: a club, a class, a choir…"), Modifier.weight(1f))
                LineButton("+", enabled = name.isNotBlank()) { val n = name; name = ""; c.write { repo.harbor(n) } }
            }

            Section(w.t("Экспедиции · $done из ${Expedition.entries.size}", "Expeditions · $done of ${Expedition.entries.size}"),
                w.t("Долгие цели — про то, из чего на самом деле складывается дружба.", "Long goals — about what friendships are actually made of."))
            for (e in Expedition.entries.sortedBy { it.done(s, h.zone) }) {
                val p = e.progress(s, h.zone)
                val ok = p >= e.goal
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(if (ok) C.light else C.paper, Shapes.Sm).border(1.dp, if (ok) C.ink else C.faint, Shapes.Sm).padding(12.dp)) {
                    Column(Modifier.padding(end = if (ok) 76.dp else 0.dp)) {
                        Text(w.expedition(e), style = T.Title.copy(color = if (ok) C.ink else C.muted))
                        Text(w.expeditionGoal(e), style = T.Note)
                        if (!ok) {
                            Gap(6)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f).height(8.dp).border(1.dp, C.ink).padding(1.dp)) {
                                    Box(Modifier.fillMaxWidth(p / e.goal.toFloat()).height(6.dp).background(C.muted))
                                }
                                Text("  $p / ${e.goal}", style = T.Small)
                            }
                        }
                    }
                    if (ok) Text(w.t("НАНЕСЕНО", "CHARTED"), style = T.Label.copy(color = C.red),
                        modifier = Modifier.align(Alignment.CenterEnd).rotate(-8f).border(1.5.dp, C.red, Shapes.Sm).padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            val years = Years.summaries(s, h.zone)
            if (years.isNotEmpty()) {
                Section(w.t("Годы", "The years"), w.t("Карта ведётся годами. Вот как они прошли.", "A chart is kept for years. Here's how they went."))
                for (y in years) {
                    YearCard(h, y)
                    Gap(10)
                }
            }
            Gap(24)
        }
    }
}

@Composable
private fun YearCard(h: Home, y: Years.Summary) {
    val w = h.w
    Card {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${y.year}", style = T.Display, modifier = Modifier.weight(1f))
            if (y.year == h.today.year) Label(w.t("идёт", "this year"))
        }
        val lines = listOfNotNull(
            if (y.drawn.isNotEmpty()) w.t("Новые острова: ", "New islands: ") + y.drawn.joinToString(", ") { it.name } else null,
            if (y.minutes > 0) w.t("${w.total(y.minutes)} вместе, ${y.meetings} ${w.plural(y.meetings, "встреча", "встречи", "встреч")} с ${y.people} ${w.plural(y.people, "человеком", "людьми", "людьми")}",
                "${w.total(y.minutes)} together, ${y.meetings} meetings with ${y.people} ${if (y.people == 1) "person" else "people"}") else null,
            if (y.raised.isNotEmpty()) w.t("Мосты выросли: ", "Bridges raised: ") + y.raised.joinToString(", ") { (i, b) -> "${i.name} — ${w.bridge(b).lowercase()}" } else null,
            if (y.most.isNotEmpty()) w.t("Больше всего времени: ", "Most time with: ") + y.most.joinToString(", ") { (i, m) -> "${i.name} (${w.total(m)})" } else null,
            listOfNotNull(
                if (y.gatherings > 0) w.t("маяков: ${y.gatherings}", "lighthouses: ${y.gatherings}") else null,
                if (y.ferries > 0) w.t("паромов: ${y.ferries}", "ferries: ${y.ferries}") else null,
                if (y.sorties > 0) w.t("вылазок: ${y.sorties}", "sorties: ${y.sorties}") else null,
                if (y.shells > 0) w.shellsN(y.shells) else null,
            ).takeIf { it.isNotEmpty() }?.joinToString(" · "),
        )
        for (l in lines) Text(l, style = T.Muted.copy(color = C.ink), modifier = Modifier.padding(top = 4.dp))
    }
}
