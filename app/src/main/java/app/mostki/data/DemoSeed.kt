package app.mostki.data

import app.mostki.AppContainer
import app.mostki.MostkiApp
import app.mostki.domain.Contact
import app.mostki.domain.DAY
import app.mostki.domain.Ferry
import app.mostki.domain.Harbor
import app.mostki.domain.Island
import app.mostki.domain.Memo
import app.mostki.domain.Sortie
import app.mostki.domain.State
import app.mostki.domain.Touch
import app.mostki.domain.Trait
import app.mostki.domain.Visit
import app.mostki.ru
import kotlin.math.PI
import kotlin.random.Random

/**
 * The demo build's chart: a year of an ordinary life — a friend from school under an arch of lanterns,
 * a climbing partner on planks, a choir friend whose bridge has started to creak, an old neighbour lost
 * in the fog, and someone met at a quiz two days ago, still waiting for the paper boat.
 */
object DemoSeed {
    suspend fun seedIfEmpty(app: MostkiApp, c: AppContainer) {
        if (c.repo.refresh().islands.isNotEmpty()) return
        c.settings.update { it.copy(onboarded = true) }
        c.repo.seed(build(System.currentTimeMillis(), app.ru))
    }

    fun build(now: Long, ru: Boolean): State {
        fun t(r: String, e: String) = if (ru) r else e
        val rnd = Random(11)
        val h = { d: Int -> now - d * DAY }
        val harbors = listOf(
            Harbor(1, t("Скалодром", "Climbing gym"), h(330)),
            Harbor(2, t("Квиз по средам", "Wednesday quiz"), h(60)),
            Harbor(3, t("Хор", "Choir"), h(400)),
            Harbor(4, t("Двор", "The yard"), h(500)),
        )
        val islands = listOf(
            Island(1, t("Дима", "Dima"), t("лезет 6b и боится высоты", "climbs 6b and is scared of heights"), listOf(Trait.MOUNTAINS, Trait.SPORT), 1, -1.95, h(300), h(296)),
            Island(2, t("Аня", "Anya"), t("поёт альтом, собирает марки", "sings alto, collects stamps"), listOf(Trait.MUSIC, Trait.BOOKS), 3, -0.55, h(390), h(385)),
            Island(3, t("Лёша", "Lyosha"), t("друг со школы", "friend since school"), listOf(Trait.SEA, Trait.COFFEE, Trait.FOREST), null, 0.55, h(3000), h(2990)),
            Island(4, t("Марта", "Marta"), t("рассказывала про сыроварню в Карелии", "told me about a cheese dairy up north"), listOf(Trait.COFFEE, Trait.PETS), 2, 2.05, h(2)),
            Island(5, t("Кирилл", "Kirill"), t("чинит старые велосипеды", "fixes up old bicycles"), listOf(Trait.SPORT, Trait.TRAVEL), 4, 2.85, h(200), h(196)),
            Island(6, t("Оля", "Olya"), t("бывшая соседка по общаге", "my old dorm neighbour"), listOf(Trait.BOOKS, Trait.GARDEN), null, -2.75, h(1500), h(1490)),
            Island(7, t("Тимур", "Timur"), t("рисует комиксы на полях бланка", "draws comics on the answer sheet"), listOf(Trait.ART, Trait.GAMES), 2, 1.35, h(26), h(23)),
            Island(8, t("Вера", "Vera"), t("соседка с таксой Бусей", "the neighbour with a dachshund called Busya"), listOf(Trait.PETS, Trait.GARDEN), 4, -PI + .2, h(150), h(146)),
        )
        val contacts = mutableListOf<Contact>()
        var cid = 1L
        val whats = listOf(t("кофе", "coffee"), t("прогулка", "a walk"), t("ужин", "dinner"), "", t("кино", "cinema"), "", t("помог с переездом", "helped with a move"))
        /** Meetings from [from] to [to] days ago adding up to about [hours], the last one exactly at [to]. */
        fun life(island: Long, hours: Int, from: Int, to: Int, what: String) {
            val n = (hours * 60 / 150).coerceAtLeast(1)
            var left = hours * 60
            for (k in 0 until n) {
                val d = if (n == 1) to else from - (from - to) * k / (n - 1)
                val m = if (k == n - 1) left.coerceAtLeast(30) else (90 + rnd.nextInt(120)).coerceAtMost(left - 30 * (n - 1 - k)).coerceAtLeast(30)
                left -= m
                val w = if (k % 3 == 0) what else whats[rnd.nextInt(whats.size)]
                contacts += Contact(cid++, island, h(d) - rnd.nextInt(6) * 3600_000L, m, w)
            }
        }
        life(1, 75, 300, 3, t("скалодром", "climbing"))
        life(2, 132, 390, 38, t("после репетиции", "after rehearsal"))
        life(3, 232, 2900, 5, t("кофе и долгий разговор", "coffee and a long talk"))
        contacts += Contact(cid++, 4, h(2), 40, t("квиз, команда «Сырная тарелка»", "the quiz, team Cheese Board"))
        life(5, 41, 196, 20, t("мастерская", "the workshop"))
        life(6, 64, 1490, 210, t("чай на кухне", "tea in the kitchen"))
        contacts += Contact(cid++, 7, h(26), 30, t("квиз", "the quiz"))
        contacts += Contact(cid++, 7, h(23), 0, "", Touch.WROTE)
        life(7, 5, 16, 9, t("квиз", "the quiz"))
        life(8, 14, 146, 12, t("гуляли с Бусей", "walked Busya"))
        // An evening under the lighthouse nine days ago.
        val g = h(9) + 4 * 3600_000L
        for (id in listOf(1L, 3L, 7L, 8L)) contacts += Contact(cid++, id, g, 180, t("настолки у меня", "board games at mine"), gathering = g)

        val ferries = listOf(Ferry(1, 1, 3, h(150)), Ferry(2, 1, 7, g), Ferry(3, 3, 7, g), Ferry(4, 3, 8, g), Ferry(5, 7, 8, g), Ferry(6, 1, 8, g))
        val visits = mutableListOf<Visit>()
        var vid = 1L
        for (w in 0 until 24) visits += Visit(vid++, 1, h(3 + w * 7))
        for (w in 0 until 8) visits += Visit(vid++, 2, h(2 + w * 7))
        for (w in 0 until 5) visits += Visit(vid++, 3, h(40 + w * 7))
        val sorties = listOf(
            Sortie(1, h(30), 0, 0, true), Sortie(2, h(27), 0, 2, true), Sortie(3, h(24), 0, 1, false),
            Sortie(4, h(20), 1, 0, true), Sortie(5, h(16), 1, 3, false), Sortie(6, h(13), 1, 1, true),
            Sortie(7, h(6), 2, 0, false), Sortie(8, h(2), 2, 1, true),
        )
        val memos = listOf(
            Memo(1, 1, t("В субботу соревнования", "Competition on Saturday"), h(3)),
            Memo(2, 1, t("Пса зовут Бублик", "The dog is called Bagel"), h(80)),
            Memo(3, 2, t("Переезжала в сентябре — как обжилась?", "Moved flat in September — settled in?"), h(40)),
            Memo(4, 3, t("День рождения 14 ноября", "Birthday on 14 November"), h(200)),
            Memo(5, 5, t("Обещал показать мастерскую", "Promised to show me his workshop"), h(20)),
            Memo(6, 4, t("Карельский сыр — спросить, где купить", "That northern cheese — ask where to buy it"), h(2)),
        )
        return State(islands, contacts, ferries, harbors, visits, sorties, memos)
    }
}
