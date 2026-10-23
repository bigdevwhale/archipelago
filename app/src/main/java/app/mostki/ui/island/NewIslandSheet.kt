package app.mostki.ui.island

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.mostki.container
import app.mostki.domain.Rules
import app.mostki.domain.Sea
import app.mostki.domain.Trait
import app.mostki.ui.Home
import app.mostki.ui.Nav
import app.mostki.ui.components.Chip
import app.mostki.ui.components.Field
import app.mostki.ui.components.Flow
import app.mostki.ui.components.Gap
import app.mostki.ui.components.Label
import app.mostki.ui.components.LineButton
import app.mostki.ui.components.RedButton
import app.mostki.ui.components.Section
import app.mostki.ui.components.Sheet
import app.mostki.ui.theme.T

/** Chart someone new: a name, one detail, what they love. No photo — the island grows from the detail. */
@Composable
fun NewIslandSheet(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    var name by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var traits by remember { mutableStateOf(listOf<Trait>()) }
    var harbor by remember { mutableStateOf<Long?>(null) }
    var newHarbor by remember { mutableStateOf("") }
    var talk by remember { mutableIntStateOf(30) }

    Sheet(w.t("Нанести остров", "Chart an island"), w.t("Закрыть", "Close"), nav.close) {
        Text(w.t("Фото не нужно. Запиши одну деталь, которая запомнилась, — из неё и вырастет остров.",
            "No photo. Write down one detail that stuck — the island grows from that."), style = T.Note)
        Gap(12)
        Label(w.t("Имя", "Name"))
        Field(name, { name = it }, w.t("Как зовут", "Their name"))
        Gap(10)
        Label(w.t("Одна деталь", "One detail"))
        Field(detail, { detail = it }, w.t("например: держит улей на крыше", "e.g. keeps bees on the roof"))
        Section(w.t("Что любит", "What they love"))
        TraitPicker(w, traits) { traits = it }
        Section(w.t("Где познакомились", "Where you met"), w.t("Гавань — место, куда ты возвращаешься.", "A harbour is a place you keep coming back to."))
        Flow {
            Chip(w.t("случайно, не в гавани", "by chance, not at a harbour"), harbor == null) { harbor = null }
            for (hb in h.s.harbors) Chip(hb.name, harbor == hb.id) { harbor = hb.id }
        }
        Gap(8)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(newHarbor, { newHarbor = it }, w.t("новая гавань: клуб, секция, двор…", "new harbour: club, class, the yard…"), Modifier.weight(1f))
            LineButton("+", enabled = newHarbor.isNotBlank()) {
                val n = newHarbor; newHarbor = ""
                c.write { harbor = repo.harbor(n) }
            }
        }
        Section(w.t("Сколько проговорили", "How long you talked"))
        Flow {
            for (m in listOf(0, 10, 30, 60, 120)) Chip(if (m == 0) w.t("пара фраз", "a few words") else w.dur(m), talk == m) { talk = m }
        }
        Gap(20)
        RedButton(w.t("Нанести на карту", "Put it on the chart"), Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
            val n = name; val d = detail; val t = traits; val hb = harbor; val m = talk
            c.write { repo.draw(n, d, t, hb, m) }
            Toast.makeText(ctx, w.t("Из тумана показался остров «$n». Через неделю — второй раз!", "An island called $n rises from the fog. Within a week — the second time!"), Toast.LENGTH_LONG).show()
            nav.close()
        }
    }
}

/** The lighthouse: have people over. Everyone who comes gets the hours, and meets everyone else. */
@Composable
fun LighthouseSheet(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val open = h.s.islands.filter { Rules.sea(h.s, it, h.now, h.zone) != Sea.FOG }
    var guests by remember { mutableStateOf(open.filter { Rules.sea(h.s, it, h.now, h.zone) == Sea.CREAKING }.map { it.id }.toSet()) }
    var dur by remember { mutableIntStateOf(180) }
    var what by remember { mutableStateOf("") }
    Sheet(w.t("✦ Зажечь маяк", "✦ Light the lighthouse"), w.t("Закрыть", "Close"), nav.close) {
        Text(w.t("Собери людей у себя: ужин, настолки, пикник. Каждому — часы к мосту, а тем, кто ещё не знаком, — паром друг к другу.",
            "Have people over: dinner, board games, a picnic. Everyone gets the hours on their bridge, and those who hadn't met get a ferry to each other."), style = T.Note)
        Section(w.t("Кто пришёл", "Who came"), w.t("Скрипящие мосты отмечены заранее.", "Creaking bridges are ticked already."))
        Flow { for (i in open) Chip(i.name, i.id in guests) { guests = if (i.id in guests) guests - i.id else guests + i.id } }
        Section(w.t("Сколько длилось", "How long it lasted"))
        Flow { for (d in listOf(120, 180, 300, 480)) Chip(if (d == 480) w.t("весь день", "all day") else w.dur(d), dur == d) { dur = d } }
        Gap(8)
        Field(what, { what = it }, w.t("что за вечер", "what kind of evening"))
        Gap(20)
        RedButton(w.t("Зажечь", "Light it"), Modifier.fillMaxWidth(), enabled = guests.size >= 2) {
            val g = open.map { it.id }.filter { it in guests }; val t = what; val d = dur
            c.write { repo.gather(g, d, t) }
            Toast.makeText(ctx, w.t("Маяк горит. Острова видят друг друга.", "The lighthouse is lit. The islands can see each other."), Toast.LENGTH_LONG).show()
            nav.close()
        }
        if (guests.size < 2) Text(w.t("Маяк — для двоих и больше.", "The lighthouse is for two or more."), style = T.Small)
    }
}
