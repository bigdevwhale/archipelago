package app.mostki.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.mostki.ui.Words
import app.mostki.ui.theme.C
import app.mostki.ui.theme.T

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "archipelago"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

/** Report a bug, suggest an idea, follow updates, join the chat; each opens Telegram. */
@Composable
fun CommunityLinks(w: Words) {
    val ctx = LocalContext.current
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    Card {
        for ((icon, label, sub, url) in listOf(
            Link("🐞", w.t("Сообщить о баге", "Report a bug"), w.t("что сломалось и как повторить", "what broke and how to repeat it"), Community.BUG),
            Link("💡", w.t("Предложить идею", "Suggest an idea"), w.t("функция, вылазка, знак на карте", "a feature, an outing, a mark on the chart"), Community.IDEA),
            Link("🔔", w.t("Подписаться на обновления", "Follow updates"), w.t("бот напишет, когда выйдет версия", "the bot writes when a version is out"), Community.FOLLOW),
            Link("💬", w.t("Чат StillCraft", "StillCraft chat"), w.t("все приложения и люди", "all the apps and people"), Community.CHAT),
        )) {
            Row(Modifier.fillMaxWidth().clickable { open(url) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, style = T.Title, modifier = Modifier.width(34.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = T.Body)
                    Text(sub, style = T.Muted)
                }
                Text("›", style = T.Title.copy(color = C.red))
            }
        }
    }
}

private data class Link(val icon: String, val label: String, val sub: String, val url: String)
