package app.mostki.ui.sortie

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.mostki.container
import app.mostki.domain.Ladder
import app.mostki.domain.Rules
import app.mostki.domain.Sea
import app.mostki.domain.Tasks
import app.mostki.ui.Home
import app.mostki.ui.Nav
import app.mostki.ui.Overlay
import app.mostki.ui.art.Shells
import app.mostki.ui.components.Card
import app.mostki.ui.components.Gap
import app.mostki.ui.components.Heading
import app.mostki.ui.components.Label
import app.mostki.ui.components.LineButton
import app.mostki.ui.components.NativeCanvas
import app.mostki.ui.components.RedButton
import app.mostki.ui.components.Section
import app.mostki.ui.theme.C
import app.mostki.ui.theme.T

@Composable
fun SortieScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val s = h.s
    val rung = Ladder.rung(s)
    val tries = Ladder.tries(s)
    val task = Ladder.task(s, h.today, h.settings.skips)
    val r = Tasks.rungs.getOrNull(rung)
    val today = s.sorties.count { java.time.Instant.ofEpochMilli(it.at).atZone(h.zone).toLocalDate() == h.today }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Heading(w.t("Вылазка", "Sortie"), w.t("Один маленький шаг к людям.", "One small step towards people."))
        Column(Modifier.padding(horizontal = 18.dp)) {
            Gap(8)
            Card {
                Label(if (r != null) w.t("Ступень «${r.name.ru}» · попытка ${tries + 1} из ${Ladder.TRIES}", "Rung \"${r.name.en}\" · try ${tries + 1} of ${Ladder.TRIES}")
                else w.t("Открытое море", "The open sea"))
                Gap(6)
                Text(Tasks.pool(rung)[task](h.ru), style = T.Task)
                Gap(6)
                Text(r?.why?.invoke(h.ru) ?: w.t("Лестница пройдена. Теперь — то, на чём держится дружба: звать, знакомить, возвращаться.",
                    "The ladder is climbed. Now for what friendships rest on: asking, introducing, coming back."), style = T.Note)
                Gap(6)
                Text(w.t("Засчитывается попытка, а не ответ. Отказ — тоже смелость.", "The try counts, not the answer. A \"no\" is courage too."), style = T.Small)
                Gap(12)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RedButton(w.t("Получилось", "It worked"), Modifier.weight(1f)) {
                        c.write { repo.sortie(rung, task, true) }
                        if (rung >= 2) {
                            Toast.makeText(ctx, w.t("Если человек запомнился — нанеси его остров.", "If they stuck in your mind — chart their island."), Toast.LENGTH_LONG).show()
                            nav.open(Overlay.New)
                        } else Toast.makeText(ctx, w.t("Получилось! Мир стал чуть ближе.", "Done! The world is a little closer."), Toast.LENGTH_SHORT).show()
                    }
                    LineButton(w.t("Отказали 🐚", "Said no 🐚"), Modifier.weight(1f)) {
                        c.write { repo.sortie(rung, task, false) }
                        Toast.makeText(ctx, w.t("Ракушка смелости. Риск — это и есть тренировка.", "A shell of courage. You took the risk — that's the whole exercise."), Toast.LENGTH_LONG).show()
                    }
                }
                Gap(8)
                LineButton(w.t("Другое задание", "Another task"), Modifier.fillMaxWidth(), dashed = true) { c.write { settings.update { it.copy(skips = it.skips + 1) } } }
                if (today > 0) {
                    Gap(6)
                    Text(w.t("Сегодня уже ${today}. Можно ещё — или отдохнуть до завтра.", "$today today already. Another one — or rest till tomorrow."), style = T.Small)
                }
            }

            val boats = s.islands.filter { Rules.sea(s, it, h.now, h.zone).let { x -> x == Sea.NEW || x == Sea.DRIFTING } }
            for (i in boats) {
                Gap(10)
                Card(Modifier.clickable { nav.open(Overlay.Isle(i.id)) }) {
                    Text(w.t("⛵ Второй раз: ${i.name}", "⛵ The second time: ${i.name}"), style = T.Title.copy(color = C.red))
                    val left = Rules.kiteDaysLeft(i, h.now, h.zone)
                    Text(if (left > 0) w.t("Отправь кораблик — осталось ${w.daysN(left)}.", "Send the paper boat — ${w.daysN(left)} left.")
                    else w.t("Остров дрейфует. Ещё не поздно написать.", "The island is drifting. It's not too late to write."), style = T.Note)
                }
            }

            Section(w.t("Лестница смелости", "The ladder of courage"), w.t("Три попытки на ступень — и выше.", "Three tries a rung, then up."))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (rung >= Ladder.TOP) Rung(w.t("Открытое море", "The open sea"), done = false, cur = true, pips = "")
                for (k in Tasks.rungs.indices.reversed()) {
                    val n = Tasks.rungs[k].name(h.ru)
                    Rung(n, done = k < rung, cur = k == rung, pips = when {
                        k < rung -> "●●●"
                        k == rung -> "●".repeat(tries) + "○".repeat(Ladder.TRIES - tries)
                        else -> ""
                    })
                }
            }

            val shells = Ladder.shells(s)
            Section(w.t("Ракушки смелости · $shells", "Shells of courage · $shells"),
                w.t("Каждый отказ приносит ракушку. Их не тратят — только собирают. Каждая десятая — наутилус.",
                    "Every \"no\" brings a shell. They aren't spent, only kept. Every tenth is a nautilus."))
            if (shells == 0) Text(w.t("Пока ни одной — настоящий риск ещё впереди.", "None yet — so no real risks taken yet."), style = T.Small)
            else {
                val rows = (shells + 7) / 8
                NativeCanvas(Modifier.fillMaxWidth().height((rows * 44).dp)) { cv, cw, ch ->
                    val cell = cw / 8; val row = ch / rows
                    for (n in 0 until shells) {
                        val x = (n % 8) * cell + cell / 2; val y = (n / 8) * row + row / 2
                        Shells.draw(cv, Shells.kind(n), x, y, minOf(cell, row) / 34f, ((n * 37) % 50 - 25).toFloat())
                    }
                }
                Text(w.t("Последняя — ${w.shell(Shells.kind(shells - 1)).lowercase()}.", "The latest is a ${w.shell(Shells.kind(shells - 1)).lowercase()}."), style = T.Small)
            }
            Gap(24)
        }
    }
}

@Composable
private fun Rung(name: String, done: Boolean, cur: Boolean, pips: String) {
    Row(Modifier.fillMaxWidth().background(if (cur) C.light else C.paper.copy(alpha = 0f)), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(36.dp).background(if (cur) C.red else if (done) C.ink else C.faint))
        Text((if (done) "✓  " else if (cur) "➤  " else "·  ") + name, style = T.Body.copy(color = if (cur) C.red else if (done) C.ink else C.faint),
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        Text(pips, style = T.Muted.copy(color = if (cur) C.red else C.ink), modifier = Modifier.padding(end = 12.dp))
    }
}
