package app.mostki.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.mostki.container
import app.mostki.data.DemoSeed
import app.mostki.data.Settings
import app.mostki.domain.Contact
import app.mostki.domain.DAY
import app.mostki.domain.Island
import app.mostki.domain.State
import app.mostki.domain.Trait
import app.mostki.ui.Home
import app.mostki.ui.Words
import app.mostki.ui.art.ChartArt
import app.mostki.ui.art.Glyphs
import app.mostki.ui.art.Scene
import app.mostki.ui.art.Shells
import app.mostki.ui.components.Chip
import app.mostki.ui.components.Flow
import app.mostki.ui.components.Gap
import app.mostki.ui.components.Heading
import app.mostki.ui.components.LineButton
import app.mostki.ui.components.NativeCanvas
import app.mostki.ui.components.RedButton
import app.mostki.ui.components.Section
import app.mostki.ui.components.rememberTime
import app.mostki.ui.map.scene
import app.mostki.ui.theme.C
import app.mostki.ui.theme.T
import app.mostki.ui.theme.paper

private const val PAGES = 4

/**
 * Four pages: why friends take hours, how bridges grow, the sorties and the paper boat, and the first
 * harbours. [replay] shows them again from settings: no harbours are added, and it closes at the end.
 */
@Composable
fun Onboarding(w: Words, replay: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val c = ctx.container
    var page by remember { mutableIntStateOf(0) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val now = remember { System.currentTimeMillis() }
    val chart = remember(w.ru) { scene(Home(DemoSeed.build(now, w.ru), Settings(), w, now)) }
    val bridges = remember(w.ru) { scene(Home(bridgeState(now, w), Settings(), w, now)) }
    val dp = LocalDensity.current.density
    var harbors by remember { mutableStateOf(setOf<String>()) }
    val time = rememberTime()
    ChartArt.fonts(ctx)
    BackHandler(enabled = page > 0 || replay != null) { if (page > 0) page-- else replay?.invoke() }

    @Composable
    fun picture(s: Scene, h: Int) = NativeCanvas(Modifier.fillMaxWidth().height(h.dp)) { cv, cw, ch ->
        ChartArt.draw(cv, cw, ch, s, ChartArt.fit(s, cw, ch), time, dp, top = -1f)
    }

    Column(Modifier.fillMaxSize().paper().statusBarsPadding()) {
        Heading(w.t("Мостки", "Archipelago"), w.t("${page + 1} из $PAGES", "${page + 1} of $PAGES"))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            when (page) {
                0 -> {
                    picture(chart, 300)
                    Gap(6)
                    Text(w.t("Друзья не находятся — они строятся", "Friends aren't found — they're built"), style = T.Display)
                    Gap(8)
                    Text(w.t("Психолог Джеффри Холл посчитал: чтобы знакомый стал приятелем, нужно около 50 часов вместе, другом — около 90, близким другом — больше 200.",
                        "The psychologist Jeffrey Hall counted it: it takes about 50 hours together for an acquaintance to become a casual friend, about 90 for a friend, and over 200 for a close friend."), style = T.Body)
                    Gap(8)
                    Text(w.t("Мало познакомиться — надо возвращаться. «Мостки» помогают и с тем, и с другим: это карта твоих людей, которую ведут годами.",
                        "Meeting people isn't enough — you have to come back. Archipelago helps with both: it's a chart of your people, kept for years."), style = T.Note)
                }
                1 -> {
                    picture(bridges, 260)
                    Gap(6)
                    Text(w.t("Часы строят мосты", "Hours build bridges"), style = T.Display)
                    for (l in listOf(
                        w.t("Ты — остров в центре. Каждый человек — остров вокруг, с тем, что он любит, на берегу.",
                            "You're the island in the middle. Each person is an island around you, with what they love on its shore."),
                        w.t("После встречи запиши, сколько вы были вместе. Мост растёт: верёвка, доски (50 ч), камень (90 ч), арка с фонарями (200 ч) — и остров подплывает ближе.",
                            "After you meet, log how long you were together. The bridge grows: rope, planks (50 h), stone (90 h), a lantern arch (200 h) — and the island drifts closer."),
                        w.t("Месяц без встречи — мост скрипит. Полгода без слова — остров уходит в туман. Но его всегда можно вернуть одним сообщением.",
                            "A month without meeting and the bridge creaks. Half a year without a word and the island is lost in fog. One message can always bring it back."),
                        w.t("За каждый год знакомства на мосту появляется флажок.", "Every year you've known someone adds a pennant to the bridge."),
                    )) Text("·  $l", style = T.Body, modifier = Modifier.padding(top = 8.dp))
                }
                2 -> {
                    NativeCanvas(Modifier.fillMaxWidth().height(120.dp)) { cv, cw, ch ->
                        val k = ch / 120f
                        for (n in 0 until 5) Shells.draw(cv, if (n == 4) 4 else n, cw * (.12f + n * .11f), ch * .55f, 1.5f * k, (n * 23 % 40 - 20).toFloat())
                        Glyphs.kite(cv, cw * .82f, ch * .6f, 3f * k)
                    }
                    Text(w.t("Вылазки и второй раз", "Sorties and the second time"), style = T.Display)
                    for (l in listOf(
                        w.t("Вылазка — маленький шаг смелости в обычной жизни: кивок, вопрос, имя, похвала, контакт, приглашение. Шесть ступеней, по три попытки.",
                            "A sortie is a small step of courage in ordinary life: a nod, a question, a name, praise, a contact, an invitation. Six rungs, three tries each."),
                        w.t("Засчитывается попытка, а не ответ. За каждое «нет» — ракушка смелости. Их не тратят, только собирают.",
                            "The try counts, not the answer. Every \"no\" brings a shell of courage. They aren't spent, only kept."),
                        w.t("Познакомились — у тебя неделя на второй шаг. Это самое трудное в дружбе. Отправь бумажный кораблик — готовые сообщения уже лежат в карточке острова.",
                            "Once you've met, you have a week for the second step — the hardest part of any friendship. Send a paper boat: ready messages wait on the island's card."),
                        w.t("Маяк — собрать людей у себя. Все получают часы, а незнакомые между собой — паром.",
                            "The lighthouse is having people over. Everyone gets the hours, and those who hadn't met get a ferry."),
                    )) Text("·  $l", style = T.Body, modifier = Modifier.padding(top = 8.dp))
                }
                else -> {
                    Gap(10)
                    Text(w.t("Твои гавани", "Your harbours"), style = T.Display)
                    Gap(6)
                    Text(w.t("Дружба чаще всего рождается там, где одни и те же лица: секция, хор, двор, работа. Отметь места, куда ты и так возвращаешься, — или добавь потом в Атласе.",
                        "Friendships are born most often where the same faces keep turning up: a class, a choir, the yard, work. Pick places you already go back to — or add them later in the Atlas."), style = T.Body)
                    if (replay == null) {
                        Gap(10)
                        Flow {
                            for (n in listOf(w.t("Работа", "Work"), w.t("Двор", "The yard"), w.t("Спортзал", "The gym"), w.t("Курсы", "A course"),
                                w.t("Клуб настолок", "Board game club"), w.t("Хор", "Choir"), w.t("Беговой клуб", "Running club"), w.t("Волонтёрство", "Volunteering"))) {
                                Chip(n, n in harbors) { harbors = if (n in harbors) harbors - n else harbors + n }
                            }
                        }
                    }
                    Section(w.t("Вечерние напоминания", "Evening reminders"))
                    Text(w.t("Не чаще раза в день и только по делу: кораблик ещё не отправлен, мост начал скрипеть, годовщина знакомства.",
                        "At most once a day and only when it matters: a paper boat not yet sent, a bridge starting to creak, an anniversary."), style = T.Note)
                    Section(w.t("Только на телефоне", "On this phone only"))
                    Text(w.t("Ни сети, ни аккаунтов, ни рекламы. Карту можно сохранить в файл в настройках и перенести на новый телефон — она рассчитана на годы.",
                        "No network, no accounts, no ads. The chart can be saved to a file in settings and moved to a new phone — it's meant to last for years."), style = T.Note)
                }
            }
            Gap(16)
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.Center) {
            for (k in 0 until PAGES) Box(Modifier.padding(4.dp).size(if (k == page) 9.dp else 7.dp).background(if (k == page) C.red else C.faint, CircleShape))
        }
        Row(Modifier.navigationBarsPadding().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (page > 0) LineButton(w.t("Назад", "Back"), Modifier.weight(1f)) { page-- }
            val last = page == PAGES - 1
            RedButton(if (!last) w.t("Дальше", "Next") else if (replay != null) w.t("К карте", "To the chart") else w.t("Открыть карту", "Open the chart"), Modifier.weight(2f)) {
                if (!last) page++
                else if (replay != null) replay()
                else {
                    if (Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                    val hs = harbors.toList()
                    c.write {
                        for (n in hs) repo.harbor(n)
                        settings.update { it.copy(onboarded = true) }
                    }
                }
            }
        }
    }
}

/** Four islands, one for each kind of bridge, for the second page. */
private fun bridgeState(now: Long, w: Words): State {
    val names = listOf(w.t("Новый", "New"), w.t("Приятель", "A pal"), w.t("Друг", "A friend"), w.t("Близкий", "Close"))
    val hours = listOf(8, 60, 120, 230)
    val traits = listOf(listOf(Trait.COFFEE), listOf(Trait.SPORT, Trait.MOUNTAINS), listOf(Trait.BOOKS, Trait.MUSIC), listOf(Trait.SEA, Trait.FOREST, Trait.GARDEN))
    val islands = (0 until 4).map { k -> Island(k + 1L, names[k], "", traits[k], null, -2.6 + k * 1.45, now - 400 * DAY, now - 399 * DAY) }
    val contacts = (0 until 4).map { k -> Contact(k + 1L, k + 1L, now - 2 * DAY, hours[k] * 60, "") }
    return State(islands, contacts)
}
