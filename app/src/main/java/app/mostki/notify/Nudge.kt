package app.mostki.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.mostki.MainActivity
import app.mostki.R
import app.mostki.container
import app.mostki.domain.Rules
import app.mostki.domain.Sea
import app.mostki.domain.State
import app.mostki.domain.Years
import app.mostki.domain.days
import app.mostki.localized
import app.mostki.ru
import app.mostki.ui.Words
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * One evening look at the chart, around 18:30. At most one notification, and each reason only once:
 * a paper boat still in harbour on the last days of its week, an anniversary, a bridge that has just
 * started to creak.
 */
object Nudge {
    private const val CHANNEL = "evening"
    private const val WORK = "evening"
    const val HOUR = 18
    const val MINUTE = 30

    data class Ask(val key: String, val island: Long, val title: String, val text: String)

    fun channel(c: Context) {
        val ch = NotificationChannel(CHANNEL, c.localized().let { if (it.ru) "Вечерний обход" else "Evening round" }, NotificationManager.IMPORTANCE_DEFAULT)
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    /** What, if anything, is worth saying tonight. */
    fun pick(s: State, now: Long, zone: ZoneId, told: Set<String>, w: Words): Ask? {
        for (i in s.islands) {
            if (Rules.sea(s, i, now, zone) != Sea.NEW) continue
            val left = Rules.kiteDaysLeft(i, now, zone)
            val key = "kite:${i.id}"
            if (left in 1..3 && key !in told) return Ask(key, i.id, w.t("⛵ Кораблик для: ${i.name}", "⛵ A paper boat for ${i.name}"),
                w.t("Осталось ${w.daysN(left)}, чтобы сделать второй шаг. Готовые сообщения — в «Мостках».", "${w.daysN(left).replaceFirstChar { it.uppercase() }} left for the second step. Ready messages are waiting in the app."))
        }
        for ((i, y) in Years.anniversaries(s, now, zone)) {
            val key = "year:${i.id}:$y"
            if (key !in told) return Ask(key, i.id, w.t("Годовщина: ${i.name}", "Anniversary: ${i.name}"),
                w.t("Сегодня ${y} ${w.plural(y, "год", "года", "лет")}, как вы знакомы. Хороший повод написать.", "${if (y == 1) "A year" else "$y years"} today since you met. A good reason to write."))
        }
        for (i in s.islands) {
            if (Rules.sea(s, i, now, zone) != Sea.CREAKING) continue
            val since = days(s.lastMet(i), now, zone)
            val key = "creak:${i.id}:${s.lastMet(i)}"
            if (since <= Rules.CREAK_DAYS + 6 && key !in told) return Ask(key, i.id, w.t("Мост скрипит: ${i.name}", "A bridge creaks: ${i.name}"),
                w.t("Месяц без встречи. Может, кофе на этой неделе?", "A month without meeting. Coffee this week, perhaps?"))
        }
        return null
    }

    fun next(now: Long, zone: ZoneId): Long {
        val z = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone)
        var t = z.withHour(HOUR).withMinute(MINUTE).withSecond(0).withNano(0)
        if (!t.isAfter(z)) t = t.plusDays(1)
        return t.toInstant().toEpochMilli()
    }

    suspend fun plan(c: Context) {
        val app = c.applicationContext
        val st = app.container.settings.now()
        val wm = WorkManager.getInstance(app)
        if (!st.onboarded || !st.nudges) { wm.cancelUniqueWork(WORK); return }
        val now = System.currentTimeMillis()
        val req = OneTimeWorkRequestBuilder<EveningWorker>().setInitialDelay(next(now, ZoneId.systemDefault()) - now, TimeUnit.MILLISECONDS).build()
        wm.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, req)
    }

    fun post(c: Context, a: Ask) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_boat)
            .setContentTitle(a.title)
            .setContentText(a.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(a.text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(c).notify(1, n)
    }
}

class EveningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val st = c.settings.now()
        if (st.nudges) {
            val s = c.repo.refresh()
            val w = Words(applicationContext.localized().ru)
            Nudge.pick(s, System.currentTimeMillis(), ZoneId.systemDefault(), st.told, w)?.let { a ->
                Nudge.post(applicationContext, a)
                c.settings.update { it.copy(told = it.told + a.key) }
            }
        }
        Nudge.plan(applicationContext)
        return Result.success()
    }
}
