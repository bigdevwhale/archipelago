package app.mostki.ui.island

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.mostki.container
import app.mostki.domain.Bridge
import app.mostki.domain.Rules
import app.mostki.domain.Sea
import app.mostki.domain.Touch
import app.mostki.domain.Trait
import app.mostki.domain.Years
import app.mostki.domain.date
import app.mostki.domain.days
import app.mostki.ui.Home
import app.mostki.ui.Nav
import app.mostki.ui.Words
import app.mostki.ui.components.Card
import app.mostki.ui.components.Chip
import app.mostki.ui.components.Field
import app.mostki.ui.components.Flow
import app.mostki.ui.components.Gap
import app.mostki.ui.components.HoursBar
import app.mostki.ui.components.Label
import app.mostki.ui.components.LineButton
import app.mostki.ui.components.RedButton
import app.mostki.ui.components.Section
import app.mostki.ui.components.Sheet
import app.mostki.ui.theme.C
import app.mostki.ui.theme.Shapes
import app.mostki.ui.theme.T

val DURATIONS = listOf(30, 60, 120, 180, 300, 480)

@Composable
fun IslandSheet(h: Home, id: Long, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val s = h.s
    val i = s.island(id) ?: run { LaunchedEffect(id) { nav.close() }; return }
    val sea = Rules.sea(s, i, h.now, h.zone)
    val m = s.minutes(id)
    val b = Bridge.of(m)
    var editing by remember(id) { mutableStateOf(false) }

    Sheet(null, w.t("Закрыть", "Close"), nav.close) {
        Text(i.name, style = T.Name)
        if (i.detail.isNotBlank()) Text("«${i.detail}»", style = T.Note)
        val harbor = s.harbor(i.harbor)
        val known = Years.known(i, h.now, h.zone)
        Text(listOfNotNull(
            harbor?.let { w.t("гавань: ${it.name}", "harbour: ${it.name}") },
            w.t("на карте с ", "charted ") + w.date(i.made.date(h.zone), h.today).lowercase().let { if (known >= 1) "$it · ${w.t("$known ${w.plural(known, "год", "года", "лет")}", if (known == 1) "a year" else "$known years")}" else it },
        ).joinToString(" · "), style = T.Small)
        if (i.traits.isNotEmpty()) {
            Gap(8)
            Flow { for (t in i.traits) Text(w.trait(t), style = T.Small.copy(color = C.ink), modifier = Modifier.border(1.dp, C.faint, Shapes.Pill).padding(horizontal = 10.dp, vertical = 3.dp)) }
        }

        if (sea == Sea.FOG) {
            Gap(14)
            Card {
                Text(w.t("Остров ушёл в туман", "The island is lost in the fog"), style = T.Title)
                Text(w.t("Вы не виделись ${w.ago(days(s.lastTouch(i), h.now, h.zone))}. Мост смыло, но остров никуда не делся — обычно хватает одного сообщения.",
                    "Last word ${w.ago(days(s.lastTouch(i), h.now, h.zone))}. The bridge washed away, but the island is still there — one message is usually enough."), style = T.Note)
                Drafts(w, w.fogDrafts())
                RedButton(w.t("Сообщение отправлено", "Message sent"), Modifier.fillMaxWidth()) {
                    c.write { repo.wrote(id) }
                    Toast.makeText(ctx, w.t("${i.name} выходит из тумана. Теперь — встретиться.", "${i.name} comes out of the fog. Now — meet."), Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Section("${w.bridge(b)} · ${w.bond(b)}")
            HoursBar(m, Bridge.entries.drop(1).map { it.hours }, Bridge.entries.drop(1).map { w.hours(it.hours) }, Bridge.entries.drop(1).map { w.bond(it) })
            val next = b.next
            Text(w.t("${w.total(m)} вместе", "${w.total(m)} together") +
                (next?.let { w.t(" · ещё ${w.hours((it.hours * 60 - m + 59) / 60)} до ${w.bridgeTo(it)}", " · ${w.hours((it.hours * 60 - m + 59) / 60)} more to ${w.bridgeTo(it)}") }
                    ?: w.t(" · дальше — только беречь", " · from here on, just look after it")), style = T.Muted)
            val last = days(s.lastMet(i), h.now, h.zone)
            Text(w.t("Последняя встреча: ${w.ago(last)}", "Last met: ${w.ago(last)}"), style = T.Muted.copy(color = if (sea == Sea.CREAKING) C.red else C.muted))

            when (sea) {
                Sea.NEW, Sea.DRIFTING -> {
                    Gap(14)
                    Card {
                        Text(w.t("⛵ Второй раз", "⛵ The second time"), style = T.Title.copy(color = C.red))
                        val left = Rules.kiteDaysLeft(i, h.now, h.zone)
                        Text(if (sea == Sea.NEW) w.t("Познакомиться — полдела, труднее второй шаг. Отправь кораблик — ещё ${w.daysN(left)}, и остров встанет на якорь.",
                            "Meeting is half of it; the second step is harder. Send a paper boat — ${w.daysN(left)} left — and the island drops anchor.")
                        else w.t("Неделя прошла, и остров дрейфует. Ещё не поздно: напиши — и он встанет на якорь.",
                            "The week went by and the island is drifting. It's not too late: write, and it drops anchor."), style = T.Note)
                        Drafts(w, w.kiteDrafts(harbor?.name))
                        RedButton(w.t("Кораблик отправлен", "Boat sent"), Modifier.fillMaxWidth()) {
                            c.write { repo.wrote(id) }
                            Toast.makeText(ctx, w.t("${i.name}: остров встал на якорь ⚓", "${i.name}: the island drops anchor ⚓"), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Sea.CREAKING -> {
                    Gap(14)
                    Card {
                        Text(w.t("Мост скрипит", "The bridge creaks"), style = T.Title.copy(color = C.red))
                        Text(w.t("Больше месяца без встречи. Доски не сгнили — их просто давно никто не проходил.",
                            "Over a month without meeting. The planks haven't rotted — nobody's walked them for a while."), style = T.Note)
                        Drafts(w, w.creakDrafts())
                    }
                }
                else -> Unit
            }

            Section(w.t("Лоция", "Sailing notes"), w.t("Что помнить к следующей встрече.", "What to remember for next time."))
            val memos = s.memos(id)
            for (memo in memos) Row(Modifier.fillMaxWidth().clickable { c.write { repo.tick(memo) } }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (memo.done) "✓" else "⚓", style = T.Small, modifier = Modifier.size(22.dp))
                Text(memo.text, style = T.HandBody.copy(color = if (memo.done) C.faint else C.ink, textDecoration = if (memo.done) TextDecoration.LineThrough else null), modifier = Modifier.weight(1f))
                Text("×", style = T.Title.copy(color = C.faint), modifier = Modifier.clickable { c.write { repo.dropMemo(memo.id) } }.padding(horizontal = 8.dp))
            }
            var memo by remember(id) { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(memo, { memo = it }, w.t("например: в пятницу экзамен", "e.g. exam on Friday"), Modifier.weight(1f))
                LineButton("+", enabled = memo.isNotBlank()) { val t = memo; memo = ""; c.write { repo.memo(id, t) } }
            }

            Section(w.t("Записать встречу", "Log a meeting"), w.t("Сколько примерно были вместе.", "Roughly how long you were together."))
            var dur by remember(id) { mutableIntStateOf(120) }
            var what by remember(id) { mutableStateOf("") }
            Flow { for (d in DURATIONS) Chip(if (d == 480) w.t("весь день", "all day") else w.dur(d), dur == d) { dur = d } }
            Gap(8)
            Field(what, { what = it }, w.t("что делали", "what you did"))
            Gap(8)
            RedButton(w.t("Мы виделись", "We met"), Modifier.fillMaxWidth()) {
                val before = Bridge.of(m); val after = Bridge.of(m + dur)
                val t = what; what = ""
                c.write { repo.meet(id, dur, t) }
                Toast.makeText(ctx, if (after != before) w.t("Теперь это ${w.bridge(after).lowercase()} — ${w.bond(after)}!", "It's a ${w.bridge(after).lowercase()} now — ${w.bond(after)}!")
                else w.t("+${w.dur(dur)} к мосту", "+${w.dur(dur)} on the bridge"), Toast.LENGTH_SHORT).show()
            }

            val others = s.islands.filter { o -> o.id != id && Rules.sea(s, o, h.now, h.zone) != Sea.FOG && !s.linked(id, o.id) }
            if (others.isNotEmpty()) {
                Section(w.t("Паром", "Ferry"), w.t("Кого из своих ты познакомишь с этим человеком? Между островами пойдёт паром.", "Who of your people will you introduce them to? A ferry will run between the islands."))
                Flow {
                    for (o in others) Chip(o.name, false) {
                        c.write { repo.ferry(id, o.id) }
                        Toast.makeText(ctx, w.t("Паром ${i.name} — ${o.name} пошёл", "The ${i.name} — ${o.name} ferry is running"), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val history = s.contacts(id).sortedByDescending { it.at }
        if (history.isNotEmpty()) {
            Section(w.t("Вахтенный журнал", "Ship's log"), history.count { it.kind == Touch.MET }.let { n -> w.t("$n ${w.plural(n, "встреча", "встречи", "встреч")}", if (n == 1) "1 meeting" else "$n meetings") })
            history.take(12).forEachIndexed { n, ct ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(w.date(ct.at.date(h.zone), h.today), style = T.Small, modifier = Modifier.size(width = 92.dp, height = 18.dp))
                    Text(if (ct.kind == Touch.WROTE) w.t("сообщение", "a message") else listOf(w.dur(ct.minutes), ct.what).filter { it.isNotBlank() }.joinToString(" · ") +
                        if (ct.gathering != null) w.t(" · маяк", " · lighthouse") else "", style = T.Muted.copy(color = C.ink), modifier = Modifier.weight(1f))
                    if (n == 0 && ct.gathering == null) Text("×", style = T.Title.copy(color = C.faint), modifier = Modifier.clickable { c.write { repo.undo(ct.id) } }.padding(horizontal = 8.dp))
                }
            }
        }

        Gap(20)
        if (!editing) LineButton(w.t("Править остров", "Edit the island"), Modifier.fillMaxWidth(), dashed = true) { editing = true }
        else EditIsland(h, id) { editing = false }
    }
}

/** Ready-made messages to copy: a first word is the hardest part. */
@Composable
fun Drafts(w: Words, drafts: List<String>) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    Gap(6)
    for (d in drafts) {
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(C.paper, Shapes.Sm).border(1.dp, C.faint, Shapes.Sm).clickable {
            clip.setText(AnnotatedString(d))
            Toast.makeText(ctx, w.t("Скопировано — вставь в мессенджер", "Copied — paste it into your messenger"), Toast.LENGTH_SHORT).show()
        }.padding(horizontal = 12.dp, vertical = 9.dp)) { Text(d, style = T.Note.copy(color = C.ink)) }
    }
    Text(w.t("Нажми, чтобы скопировать.", "Tap to copy."), style = T.Small)
    Gap(8)
}

@Composable
private fun EditIsland(h: Home, id: Long, onDone: () -> Unit) {
    val c = LocalContext.current.container
    val w = h.w
    val i = h.s.island(id) ?: return
    var name by remember(id) { mutableStateOf(i.name) }
    var detail by remember(id) { mutableStateOf(i.detail) }
    var traits by remember(id) { mutableStateOf(i.traits) }
    var harbor by remember(id) { mutableStateOf(i.harbor) }
    var forget by remember(id) { mutableIntStateOf(0) }
    Card {
        Label(w.t("Имя", "Name")); Field(name, { name = it }, w.t("Имя", "Name"))
        Gap(8); Label(w.t("Деталь", "A detail")); Field(detail, { detail = it }, w.t("одна деталь", "one detail"))
        Gap(8); Label(w.t("Что любит", "What they love"))
        TraitPicker(w, traits) { traits = it }
        if (h.s.harbors.isNotEmpty()) {
            Gap(8); Label(w.t("Гавань", "Harbour"))
            Flow {
                Chip(w.t("нет", "none"), harbor == null) { harbor = null }
                for (hb in h.s.harbors) Chip(hb.name, harbor == hb.id) { harbor = hb.id }
            }
        }
        Gap(12)
        RedButton(w.t("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
            c.write { repo.edit(i.copy(name = name.trim(), detail = detail.trim(), traits = traits, harbor = harbor)) }
            onDone()
        }
        Gap(8)
        LineButton(if (forget == 0) w.t("Стереть остров с карты", "Erase the island") else w.t("Точно стереть? Вся история пропадёт", "Really? All its history goes"),
            Modifier.fillMaxWidth(), dashed = forget == 0) {
            if (forget == 0) forget = 1 else c.write { repo.forget(id) }
        }
    }
}

@Composable
fun TraitPicker(w: Words, traits: List<Trait>, onChange: (List<Trait>) -> Unit) {
    Flow {
        for (t in Trait.entries) Chip(w.trait(t), t in traits) {
            onChange(if (t in traits) traits - t else if (traits.size < 4) traits + t else traits)
        }
    }
    Text(w.t("До четырёх — их нарисуют на острове.", "Up to four — they're drawn on the island."), style = T.Small)
}
