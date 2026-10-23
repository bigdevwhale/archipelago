package app.mostki.domain

/** A text in both languages. */
data class Two(val ru: String, val en: String) {
    operator fun invoke(isRu: Boolean) = if (isRu) ru else en
}

data class Rung(val name: Two, val why: Two, val tasks: List<Two>)

/** The sorties: small, real steps towards people, from easy to bold. */
object Tasks {
    val rungs = listOf(
        Rung(
            Two("Кивок", "A nod"),
            Two("Просто заметить людей и дать им заметить тебя. С этого начинается всё остальное.",
                "Just noticing people and letting them notice you. Everything else starts here."),
            listOf(
                Two("Кивни и улыбнись трём незнакомцам", "Nod and smile at three strangers"),
                Two("Поздоровайся первым с соседом в лифте или на лестнице", "Be the first to say hello to a neighbour in the lift or on the stairs"),
                Two("Скажи «спасибо» кассиру или бариста, глядя в глаза", "Say thank you to a cashier or barista, looking them in the eye"),
                Two("Придержи дверь и скажи что-нибудь, кроме «пожалуйста»", "Hold a door and say something more than \"after you\""),
                Two("Поздоровайся с охранником, водителем или курьером по имени — или спроси имя", "Greet a guard, driver or courier by name — or ask their name"),
                Two("Посмотри в глаза и улыбнись тому, кто идёт навстречу с собакой", "Catch the eye of someone walking a dog and smile"),
                Two("Пожелай хорошего дня трём людям, которые тебя обслужили", "Wish a good day to three people who served you"),
            ),
        ),
        Rung(
            Two("Вопрос", "A question"),
            Two("Вопрос — самый лёгкий мостик: люди любят, когда их спрашивают о том, что они знают.",
                "A question is the easiest little bridge: people like being asked about what they know."),
            listOf(
                Two("Спроси у человека в очереди, что он посоветует взять", "Ask someone in the queue what they'd recommend"),
                Two("Спроси у читающего, что за книга и стоит ли она того", "Ask someone reading what the book is and whether it's worth it"),
                Two("Спроси дорогу — даже если знаешь", "Ask the way — even if you know it"),
                Two("Спроси у продавца, что он сам любит из того, что продаёт", "Ask a seller which of their things they like best themselves"),
                Two("Спроси у хозяина собаки, как её зовут и сколько ей лет", "Ask a dog owner the dog's name and age"),
                Two("Спроси у коллеги из другого отдела, чем он на самом деле занят", "Ask a colleague from another team what they actually do all day"),
                Two("Спроси у соседа, давно ли он здесь живёт и что тут было раньше", "Ask a neighbour how long they've lived here and what used to be around"),
            ),
        ),
        Rung(
            Two("Имя", "A name"),
            Two("Имя превращает «того парня с велосипедом» в человека. И его надо запомнить.",
                "A name turns \"the guy with the bike\" into a person. And it has to be remembered."),
            listOf(
                Two("Узнай имя бариста и назови своё", "Learn the barista's name and give yours"),
                Two("Представься тому, кого видишь в своей гавани каждую неделю", "Introduce yourself to someone you see at your harbour every week"),
                Two("Назови нового знакомого по имени при прощании", "Use a new person's name when you say goodbye"),
                Two("Познакомься с соседом по площадке, если ещё не знаешь, как его зовут", "Meet the neighbour across the landing, if you don't know their name yet"),
                Two("На встрече, где все чужие, узнай имена троих", "At a gathering of strangers, learn three names"),
                Two("Спроси имя у того, с кем часто пересекаешься по дороге", "Ask the name of someone you keep passing on your way"),
            ),
        ),
        Rung(
            Two("Похвала", "Praise"),
            Two("Конкретная похвала запоминается надолго — и тому, кто её получил, и тебе.",
                "Specific praise is remembered for a long time — by them and by you."),
            listOf(
                Two("Похвали конкретную вещь: шарф, собаку, выбор пирожного", "Praise one specific thing: a scarf, a dog, a choice of pastry"),
                Two("Скажи кому-то, что у него хорошо получилось — и что именно", "Tell someone they did well — and what exactly"),
                Two("Отметь вслух чужую удачную шутку", "Say out loud that someone's joke was good"),
                Two("Скажи уличному музыканту, какая песня понравилась", "Tell a street musician which song you liked"),
                Two("Напиши автору поста или видео, чем оно помогло — лично", "Write to the author of a post or video how it helped you — personally"),
                Two("Похвали работу мастера, повара или парикмахера ему самому", "Praise a craftsman's, cook's or barber's work to them directly"),
            ),
        ),
        Rung(
            Two("Контакт", "A contact"),
            Two("Без контакта случайное знакомство остаётся случайным. Повод нужен маленький.",
                "Without a contact a chance meeting stays a chance. The excuse can be tiny."),
            listOf(
                Two("Обменяйся контактами с кем-то из гавани", "Swap contacts with someone from your harbour"),
                Two("Предложи скинуть ссылку, о которой зашла речь", "Offer to send a link that came up in conversation"),
                Two("Добавь нового знакомого в общий чат или позови в него", "Add a new acquaintance to a group chat, or invite them in"),
                Two("Попроси совета, которого не нагуглить, — и номер, чтобы рассказать, как вышло", "Ask for advice you can't google — and a number to tell them how it went"),
                Two("Сфотографируй компанию и предложи прислать фото", "Take a photo of the group and offer to send it"),
                Two("Предложи книгу, игру или инструмент на время", "Offer to lend a book, a game or a tool"),
            ),
        ),
        Rung(
            Two("Приглашение", "An invitation"),
            Two("Дружба не случается сама: кто-то должен позвать. Пусть это будешь ты.",
                "Friendship doesn't just happen: someone has to ask. Let it be you."),
            listOf(
                Two("Позови кого-то из новых знакомых на кофе", "Ask someone new out for a coffee"),
                Two("Позови двоих сразу — так легче всем", "Ask two people at once — it's easier for everyone"),
                Two("Предложи пойти вместе туда, куда ты и так идёшь", "Suggest going together somewhere you're going anyway"),
                Two("Позови кого-то на прогулку с конкретным временем и местом", "Ask someone for a walk with a definite time and place"),
                Two("Предложи после занятия зайти куда-нибудь вместе", "After a class, suggest going somewhere together"),
                Two("Позови соседей на чай — с чем-нибудь испечённым", "Have the neighbours over for tea — with something baked"),
            ),
        ),
    )

    /** Past the top of the ladder: bolder things, and the slow work of keeping friends. */
    val open = listOf(
        Two("Позови на ужин того, с кем вы ещё не встречались один на один", "Invite to dinner someone you've never seen one-to-one"),
        Two("Познакомь двоих своих друзей, которым будет о чём поговорить", "Introduce two of your friends who'll have plenty to talk about"),
        Two("Напиши тому, с кем вы давно не виделись, и предложи день", "Write to someone you haven't seen in ages and suggest a day"),
        Two("Устрой встречу на троих и больше — маяк", "Host a gathering of three or more — light the lighthouse"),
        Two("Предложи регулярную встречу: каждую среду, каждое первое воскресенье", "Suggest a regular meeting: every Wednesday, every first Sunday"),
        Two("Сходи в новое место, где собираются по интересу, и заговори там с двумя", "Go to a new place where people meet over a hobby, and talk to two of them"),
        Two("Спроси друга о том, что он рассказывал в прошлый раз", "Ask a friend about what they told you last time"),
        Two("Помоги кому-то с переездом, ремонтом или делом — руками", "Help someone move, fix or make something — with your hands"),
        Two("Расскажи другу что-то о себе, чего он ещё не знает", "Tell a friend something about yourself they don't know yet"),
        Two("Пригласи нового знакомого в компанию старых друзей", "Bring a new acquaintance along to your old friends"),
    )

    fun pool(rung: Int): List<Two> = rungs.getOrNull(rung)?.tasks ?: open
}
