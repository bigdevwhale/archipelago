package app.mostki.ui

import app.mostki.domain.Bridge
import app.mostki.domain.Expedition
import app.mostki.domain.Trait
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/** The words of the chart that come from data rather than resources, in both languages. */
class Words(val ru: Boolean) {
    fun t(r: String, e: String) = if (ru) r else e

    fun plural(n: Int, one: String, few: String, many: String): String {
        val a = abs(n) % 100; val b = a % 10
        return when { a in 11..19 -> many; b == 1 -> one; b in 2..4 -> few; else -> many }
    }

    fun trait(x: Trait) = when (x) {
        Trait.MOUNTAINS -> t("горы", "mountains"); Trait.FOREST -> t("лес", "woods"); Trait.COFFEE -> t("кофе", "coffee")
        Trait.BOOKS -> t("книги", "books"); Trait.MUSIC -> t("музыка", "music"); Trait.SPORT -> t("спорт", "sport")
        Trait.SEA -> t("море", "the sea"); Trait.PETS -> t("животные", "animals"); Trait.ART -> t("рисование", "drawing")
        Trait.GAMES -> t("игры", "games"); Trait.TRAVEL -> t("путешествия", "travel"); Trait.GARDEN -> t("сад", "gardening")
    }

    fun bridge(b: Bridge) = when (b) {
        Bridge.ROPE -> t("Верёвочный мост", "Rope bridge"); Bridge.PLANKS -> t("Дощатый мост", "Plank bridge")
        Bridge.STONE -> t("Каменный мост", "Stone bridge"); Bridge.ARCH -> t("Арка с фонарями", "Lantern arch")
    }

    /** "…more to a stone bridge", in the genitive. */
    fun bridgeTo(b: Bridge) = when (b) {
        Bridge.ROPE -> t("верёвочного моста", "a rope bridge"); Bridge.PLANKS -> t("дощатого моста", "a plank bridge")
        Bridge.STONE -> t("каменного моста", "a stone bridge"); Bridge.ARCH -> t("арки с фонарями", "a lantern arch")
    }

    /** What the bridge means, in Hall's words. */
    fun bond(b: Bridge) = when (b) {
        Bridge.ROPE -> t("знакомство", "acquaintance"); Bridge.PLANKS -> t("приятельство", "casual friend")
        Bridge.STONE -> t("дружба", "friend"); Bridge.ARCH -> t("близкая дружба", "close friend")
    }

    fun hours(n: Int) = if (ru) "$n ч" else "$n h"

    /** A length of time together: 40 min, 2 h, 1.5 h. */
    fun dur(minutes: Int): String = when {
        minutes < 60 -> t("$minutes мин", "$minutes min")
        minutes % 60 == 0 -> hours(minutes / 60)
        else -> {
            val h = String.format(Locale.US, "%.1f", minutes / 60f).let { if (ru) it.replace('.', ',') else it }
            t("$h ч", "$h h")
        }
    }

    /** Total time, rounded to whole hours once there are a few. */
    fun total(minutes: Int) = if (minutes < 600) dur(minutes) else hours(minutes / 60)

    fun ago(days: Int): String = when {
        days <= 0 -> t("сегодня", "today")
        days == 1 -> t("вчера", "yesterday")
        days < 14 -> t("$days ${plural(days, "день", "дня", "дней")} назад", "$days days ago")
        days < 60 -> (days / 7).let { t("$it ${plural(it, "неделю", "недели", "недель")} назад", "$it weeks ago") }
        days < 730 -> (days / 30).let { t("$it ${plural(it, "месяц", "месяца", "месяцев")} назад", "$it months ago") }
        else -> (days / 365).let { t("$it ${plural(it, "год", "года", "лет")} назад", "$it years ago") }
    }

    fun daysN(n: Int) = t("$n ${plural(n, "день", "дня", "дней")}", if (n == 1) "1 day" else "$n days")

    fun islandsN(n: Int) = t("$n ${plural(n, "остров", "острова", "островов")}", if (n == 1) "1 island" else "$n islands")
    fun ferriesN(n: Int) = t("$n ${plural(n, "паром", "парома", "паромов")}", if (n == 1) "1 ferry" else "$n ferries")
    fun shellsN(n: Int) = t("$n ${plural(n, "ракушка", "ракушки", "ракушек")}", if (n == 1) "1 shell" else "$n shells")
    fun visitsN(n: Int) = t("$n ${plural(n, "раз", "раза", "раз")}", if (n == 1) "once" else "$n times")

    fun date(d: LocalDate, today: LocalDate): String {
        if (d == today) return t("Сегодня", "Today")
        if (d == today.minusDays(1)) return t("Вчера", "Yesterday")
        val loc = if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH
        val m = d.month.getDisplayName(if (ru) TextStyle.FULL else TextStyle.FULL_STANDALONE, loc)
        val s = if (ru) "${d.dayOfMonth} $m" else "$m ${d.dayOfMonth}"
        return if (d.year == today.year) s else "$s ${d.year}"
    }

    fun expedition(e: Expedition) = when (e) {
        Expedition.FIRST_ISLANDS -> t("Первые острова", "First islands")
        Expedition.SECOND_TIME -> t("Второй раз", "The second time")
        Expedition.PLANKS -> t("Первые доски", "First planks")
        Expedition.HARBOR -> t("Своя гавань", "A home harbour")
        Expedition.REGULAR -> t("Завсегдатай", "A regular")
        Expedition.FERRYMAN -> t("Паромщик", "Ferryman")
        Expedition.LIGHTHOUSE -> t("Маяк", "The lighthouse")
        Expedition.OUT_OF_FOG -> t("Из тумана", "Out of the fog")
        Expedition.BRAVE -> t("Ракушки", "Shells")
        Expedition.LADDER -> t("Вся лестница", "The whole ladder")
        Expedition.STONE -> t("Каменщик", "Stonemason")
        Expedition.DOZEN -> t("Архипелаг", "Archipelago")
        Expedition.ARCH -> t("Фонари", "Lanterns")
    }

    fun expeditionGoal(e: Expedition) = when (e) {
        Expedition.FIRST_ISLANDS -> t("Нанеси на карту трёх новых людей.", "Chart three new people.")
        Expedition.SECOND_TIME -> t("Трижды сделай второй шаг в течение недели после знакомства.", "Three times, take the second step within a week of meeting.")
        Expedition.PLANKS -> t("50 часов с одним человеком — мост станет дощатым.", "50 hours with one person turns the bridge to planks.")
        Expedition.HARBOR -> t("Три острова из одной гавани.", "Three islands from one harbour.")
        Expedition.REGULAR -> t("Восемь раз приди в одну и ту же гавань. Знакомые лица — начало дружбы.", "Go to the same harbour eight times. Familiar faces are where friendships start.")
        Expedition.FERRYMAN -> t("Дважды познакомь своих людей между собой.", "Introduce your people to each other twice.")
        Expedition.LIGHTHOUSE -> t("Собери у себя троих и больше.", "Have three or more over at once.")
        Expedition.OUT_OF_FOG -> t("Верни один остров из тумана — просто напиши.", "Bring one island back from the fog — just write.")
        Expedition.BRAVE -> t("Десять раз рискни и услышь «нет». Каждое — ракушка.", "Risk it and hear \"no\" ten times. Each is a shell.")
        Expedition.LADDER -> t("Пройди все шесть ступеней смелости.", "Climb all six rungs of courage.")
        Expedition.STONE -> t("Первый каменный мост: 90 часов вместе.", "Your first stone bridge: 90 hours together.")
        Expedition.DOZEN -> t("Двенадцать островов на карте.", "Twelve islands on the chart.")
        Expedition.ARCH -> t("Арка с фонарями: 200 часов с одним человеком.", "A lantern arch: 200 hours with one person.")
    }

    fun shell(kind: Int) = listOf(t("Гребешок", "Scallop"), t("Каури", "Cowrie"), t("Трубач", "Whelk"), t("Мидия", "Mussel"), t("Наутилус", "Nautilus"))[kind]

    /** Paper boats: a first message after meeting, ready to copy. */
    fun kiteDrafts(harbor: String?) = listOfNotNull(
        t("Привет! Было здорово поболтать — продолжим за кофе на неделе?", "Hi! It was great talking — shall we carry on over coffee this week?"),
        harbor?.let { t("Привет! Увидимся в следующий раз в «$it»? Можно потом зайти куда-нибудь.", "Hi! See you at $it next time? We could go somewhere after.") },
        t("Привет! Тут есть место, которое тебе понравится. Как насчёт субботы?", "Hi! There's a place I think you'd like. How about Saturday?"),
    )

    fun creakDrafts() = listOf(
        t("Привет! Сто лет не виделись — кофе на этой неделе?", "Hi! It's been ages — coffee this week?"),
        t("Привет! Как ты? Пойдём погуляем в выходные?", "Hi! How are you? Fancy a walk at the weekend?"),
    )

    fun fogDrafts() = listOf(
        t("Привет! Давно не виделись — как ты?", "Hi! It's been so long — how are you?"),
        t("Привет! Есть свободный вечер на неделе? Хочется увидеться.", "Hi! Got a free evening this week? I'd love to catch up."),
    )
}
