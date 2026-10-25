package app.mostki.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.mostki.BuildConfig
import app.mostki.container
import app.mostki.data.Backup
import app.mostki.domain.State
import app.mostki.notify.Nudge
import app.mostki.ui.Home
import app.mostki.ui.Nav
import app.mostki.ui.components.Chip
import app.mostki.ui.components.CommunityLinks
import app.mostki.ui.components.Flow
import app.mostki.ui.components.Gap
import app.mostki.ui.components.LineButton
import app.mostki.ui.components.RedButton
import app.mostki.ui.components.Section
import app.mostki.ui.components.Sheet
import app.mostki.ui.theme.C
import app.mostki.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SettingsSheet(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val st = h.settings
    var pending by remember { mutableStateOf<State?>(null) }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        c.write {
            val ok = runCatching { app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(Backup.write(repo.refresh(), System.currentTimeMillis()).toByteArray()) } }.isSuccess
            withContext(Dispatchers.Main) {
                Toast.makeText(ctx, if (ok) w.t("Карта сохранена в файл", "The chart is saved to the file") else w.t("Не удалось сохранить", "Couldn't save"), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val load = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        c.write {
            val s = runCatching { Backup.read(app.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }) }.getOrNull()
            withContext(Dispatchers.Main) {
                if (s == null) Toast.makeText(ctx, w.t("Это не файл «Мостков»", "That's not an Archipelago file"), Toast.LENGTH_SHORT).show()
                else pending = s
            }
        }
    }

    Sheet(w.t("Настройки", "Settings"), w.t("Закрыть", "Close"), nav.close) {
        Section(w.t("Язык", "Language"))
        Flow {
            Chip("Русский", h.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru")) }
            Chip("English", !h.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
        }

        Section(w.t("Вечерние напоминания", "Evening reminders"),
            w.t("Не чаще раза в день, около половины седьмого: кораблик ждёт, мост заскрипел, годовщина знакомства.",
                "At most once a day, around half past six: a paper boat waiting, a bridge starting to creak, an anniversary."))
        Flow {
            Chip(w.t("включены", "on"), st.nudges) { c.write { settings.update { it.copy(nudges = true) }; Nudge.plan(ctx) } }
            Chip(w.t("выключены", "off"), !st.nudges) { c.write { settings.update { it.copy(nudges = false) }; Nudge.plan(ctx) } }
        }

        Section(w.t("Карта на годы", "A chart for years"),
            w.t("Всё хранится только на этом телефоне. Сохрани карту в файл — и перенеси её на новый телефон, когда придёт время.",
                "Everything is kept on this phone only. Save the chart to a file — and take it to a new phone when the time comes."))
        LineButton(w.t("Сохранить карту в файл", "Save the chart to a file"), Modifier.fillMaxWidth()) { save.launch("mostki-${LocalDate.now()}.json") }
        Gap(8)
        LineButton(w.t("Открыть карту из файла", "Open a chart from a file"), Modifier.fillMaxWidth(), dashed = true) { load.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
        pending?.let { p ->
            Gap(10)
            Text(w.t("В файле ${w.islandsN(p.islands.size)} и ${p.contacts.size} записей. Карта на этом телефоне будет заменена целиком.",
                "The file has ${w.islandsN(p.islands.size)} and ${p.contacts.size} entries. The chart on this phone will be replaced whole."), style = T.Note.copy(color = C.red))
            Gap(6)
            RedButton(w.t("Заменить карту", "Replace the chart"), Modifier.fillMaxWidth()) {
                pending = null
                c.write { repo.restore(p) }
                Toast.makeText(ctx, w.t("Карта открыта", "The chart is open"), Toast.LENGTH_SHORT).show()
                nav.close()
            }
        }

        Section(w.t("Как это устроено", "How it works"))
        LineButton(w.t("Показать знакомство с картой", "Show the welcome pages"), Modifier.fillMaxWidth(), dashed = true) { nav.open(app.mostki.ui.Overlay.Intro) }

        Section(w.t("Сообщество", "Community"),
            w.t("Приложение делается вместе с теми, кто им пользуется. Всё идёт через бота StillCraft в Telegram.",
                "The app is made together with the people who use it. It all goes through the StillCraft bot on Telegram."))
        CommunityLinks(w)

        Section(w.t("О «Мостках»", "About Archipelago"))
        Text(w.t("Приложение не ищет тебе людей — оно отправляет тебя к ним и помогает возвращаться. Пороги мостов — 50, 90 и 200 часов — из исследования Джеффри Холла (2018) о том, сколько времени нужно, чтобы подружиться.",
            "The app doesn't find people for you — it sends you to them and helps you come back. The bridge thresholds — 50, 90 and 200 hours — come from Jeffrey Hall's 2018 study of how much time it takes to make a friend."), style = T.Note)
        Text(w.t("Версия ${BuildConfig.VERSION_NAME} · без рекламы и сети · исходники на GitHub", "Version ${BuildConfig.VERSION_NAME} · no ads, no network · source on GitHub"),
            style = T.Small.copy(color = C.red),
            modifier = Modifier.padding(top = 8.dp).clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bigdevwhale/archipelago"))) })
    }
}
