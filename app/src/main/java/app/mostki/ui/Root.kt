package app.mostki.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.mostki.ui.components.Gap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.mostki.R
import app.mostki.container
import app.mostki.data.Settings
import app.mostki.domain.State
import app.mostki.domain.date
import app.mostki.notify.Nudge
import app.mostki.ru
import app.mostki.ui.atlas.AtlasScreen
import app.mostki.ui.components.Art
import app.mostki.ui.island.IslandSheet
import app.mostki.ui.island.LighthouseSheet
import app.mostki.ui.island.NewIslandSheet
import app.mostki.ui.journal.JournalScreen
import app.mostki.ui.map.MapScreen
import app.mostki.ui.onboarding.Onboarding
import app.mostki.ui.settings.SettingsSheet
import app.mostki.ui.sortie.SortieScreen
import app.mostki.ui.theme.C
import app.mostki.ui.theme.T
import app.mostki.ui.theme.paper
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

/** Everything a screen reads, at one moment. */
class Home(val s: State, val settings: Settings, val w: Words, val now: Long, val zone: ZoneId = ZoneId.systemDefault()) {
    val ru get() = w.ru
    val today: LocalDate = now.date(zone)
}

enum class Tab { CHART, SORTIE, ATLAS, LOG }

sealed interface Overlay {
    data class Isle(val id: Long) : Overlay
    data object New : Overlay
    data object Lighthouse : Overlay
    data object Settings : Overlay
    /** The welcome pages again, from settings. */
    data object Intro : Overlay
}

/** Opens sheets from anywhere below the root. */
class Nav(val open: (Overlay) -> Unit, val close: () -> Unit, val tab: (Tab) -> Unit)

@Composable
fun MostkiRoot(settings: Settings, startTab: Tab = Tab.CHART, startOverlay: Overlay? = null) {
    val ctx = LocalContext.current
    val c = ctx.container
    val state by c.repo.state.collectAsStateWithLifecycle()
    val words = remember(ctx.ru) { Words(ctx.ru) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        c.repo.refresh()
        if (Art.still) return@LaunchedEffect
        while (true) { delay(60_000); now = System.currentTimeMillis() }
    }
    if (!settings.onboarded) {
        Onboarding(words)
        return
    }
    LaunchedEffect(Unit) { if (!Art.still) Nudge.plan(ctx) }
    val s = state ?: run { Box(Modifier.fillMaxSize().paper()); return }
    val home = Home(s, settings, words, now)

    var tab by remember { mutableStateOf(startTab) }
    val overlays = remember { mutableStateListOf<Overlay>().also { if (startOverlay != null) it.add(startOverlay) } }
    val nav = Nav(open = { overlays.add(it) }, close = { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }, tab = { overlays.clear(); tab = it })
    BackHandler(enabled = overlays.isEmpty() && tab != Tab.CHART) { tab = Tab.CHART }

    Box(Modifier.fillMaxSize().paper()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    Tab.CHART -> MapScreen(home, nav)
                    Tab.SORTIE -> SortieScreen(home, nav)
                    Tab.ATLAS -> AtlasScreen(home, nav)
                    Tab.LOG -> JournalScreen(home, nav)
                }
            }
            TabBar(tab) { tab = it }
        }
        when (val o = overlays.lastOrNull()) {
            is Overlay.Isle -> IslandSheet(home, o.id, nav)
            Overlay.New -> NewIslandSheet(home, nav)
            Overlay.Lighthouse -> LighthouseSheet(home, nav)
            Overlay.Settings -> SettingsSheet(home, nav)
            Overlay.Intro -> Onboarding(words, replay = nav.close)
            null -> Unit
        }
    }
}

/** The foot of the chart: a double rule, four engraved marks, the chosen one inked red with a pennant over it. */
@Composable
private fun TabBar(tab: Tab, onTab: (Tab) -> Unit) {
    val items = listOf(Tab.CHART to R.string.tab_chart, Tab.SORTIE to R.string.tab_sortie, Tab.ATLAS to R.string.tab_atlas, Tab.LOG to R.string.tab_log)
    Column(Modifier.fillMaxWidth().background(C.paper2).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(C.ink))
        Gap(2)
        Box(Modifier.fillMaxWidth().height(.8.dp).background(C.ink.copy(alpha = .45f)))
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp)) {
            for ((t, label) in items) {
                val sel = t == tab
                Column(Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTab(t) }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    TabMark(t, sel, Modifier.size(width = 44.dp, height = 34.dp))
                    Text(stringResource(label), style = T.Small.copy(fontSize = 12.5.sp, color = if (sel) C.red else C.muted, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal))
                }
            }
        }
    }
}

@Composable
private fun TabMark(t: Tab, sel: Boolean, modifier: Modifier) {
    val ink = if (sel) C.red else C.muted
    val fill = if (sel) C.redSoft else C.paper2
    Canvas(modifier) {
        val k = size.height / 34f
        val cx = size.width / 2
        val st = Stroke(1.5f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(vararg xy: Float) = Path().apply {
            moveTo(cx + xy[0] * k, xy[1] * k)
            var i = 2
            while (i < xy.size) { lineTo(cx + xy[i] * k, xy[i + 1] * k); i += 2 }
            close()
        }
        fun shape(path: Path) { drawPath(path, fill); drawPath(path, ink, style = st) }
        if (sel) {
            // The pennant that marks where you are.
            drawLine(ink, Offset(cx + 14 * k, 2 * k), Offset(cx + 14 * k, 12 * k), 1.2f * k)
            drawPath(p(14f, 2f, 21f, 4.5f, 14f, 7f), C.red)
        }
        when (t) {
            Tab.CHART -> {
                drawCircle(ink, 11 * k, Offset(cx, 19 * k), style = Stroke(1f * k))
                shape(p(0f, 6f, 3f, 16f, 0f, 19f, -3f, 16f))
                drawPath(p(0f, 32f, 3f, 22f, 0f, 19f, -3f, 22f), ink)
                shape(p(-13f, 19f, -3f, 16f, 0f, 19f, -3f, 22f))
                drawPath(p(13f, 19f, 3f, 22f, 0f, 19f, 3f, 16f), ink)
            }
            Tab.SORTIE -> {
                shape(p(-13f, 22f, 13f, 22f, 8f, 29f, -8f, 29f))
                shape(p(-9f, 22f, 0f, 7f, 9f, 22f))
                drawLine(ink, Offset(cx, 7 * k), Offset(cx, 22 * k), 1.2f * k)
            }
            Tab.ATLAS -> {
                shape(p(0f, 11f, -13f, 8f, -13f, 28f, 0f, 31f))
                shape(p(0f, 11f, 13f, 8f, 13f, 28f, 0f, 31f))
                for (y in listOf(15f, 19f, 23f)) {
                    drawLine(ink, Offset(cx - 10 * k, (y - 1) * k), Offset(cx - 3 * k, (y + 1) * k), 1f * k)
                    drawLine(ink, Offset(cx + 3 * k, (y + 1) * k), Offset(cx + 10 * k, (y - 1) * k), 1f * k)
                }
            }
            Tab.LOG -> {
                shape(p(-10f, 9f, 8f, 9f, 8f, 30f, -10f, 30f))
                for (y in listOf(15f, 19f, 23f)) drawLine(ink, Offset(cx - 6 * k, y * k), Offset(cx + 4 * k, y * k), 1f * k)
                // A quill across the page.
                shape(p(14f, 5f, 4f, 21f, 2f, 26f, 6f, 22f))
                drawLine(ink, Offset(cx + 2 * k, 26 * k), Offset(cx + 10 * k, 13 * k), 1f * k)
            }
        }
    }
}
