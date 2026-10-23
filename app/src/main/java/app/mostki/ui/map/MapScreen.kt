package app.mostki.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.mostki.domain.Bridge
import app.mostki.domain.Chart
import app.mostki.domain.Rules
import app.mostki.domain.Sea
import app.mostki.domain.Years
import app.mostki.domain.date
import app.mostki.ui.Home
import app.mostki.ui.Nav
import app.mostki.ui.Overlay
import app.mostki.ui.art.ChartArt
import app.mostki.ui.art.Isle
import app.mostki.ui.art.Scene
import app.mostki.ui.art.View
import app.mostki.ui.components.Card
import app.mostki.ui.components.Gap
import app.mostki.ui.components.LineButton
import app.mostki.ui.components.NativeCanvas
import app.mostki.ui.components.RedButton
import app.mostki.ui.components.rememberTime
import app.mostki.ui.components.ruled
import app.mostki.ui.theme.C
import app.mostki.ui.theme.T

/** The chart as it stands now: where every island lies and how it's doing. */
fun scene(h: Home, selected: Long? = null): Scene {
    val s = h.s; val w = h.w
    val pts = Chart.place(s, h.now, h.zone)
    val anniversaries = Years.anniversaries(s, h.now, h.zone).map { it.first.id }.toSet()
    val isles = s.islands.map { i ->
        val sea = Rules.sea(s, i, h.now, h.zone)
        val m = s.minutes(i.id)
        val b = Bridge.of(m)
        val p = pts.getValue(i.id)
        val next = b.next
        val (hint, warn) = when {
            i.id in anniversaries && sea != Sea.FOG -> w.t("годовщина!", "anniversary!") to true
            sea == Sea.CREAKING -> w.t("скрипит…", "creaking…") to true
            sea == Sea.NEW -> w.t("второй раз?", "second time?") to true
            sea == Sea.DRIFTING -> w.t("дрейфует", "drifting") to true
            next != null -> {
                val left = ((next.hours * 60 - m + 59) / 60)
                w.t("ещё $left ч до ${when (next) { Bridge.PLANKS -> "досок"; Bridge.STONE -> "камня"; else -> "фонарей" }}",
                    "$left h to ${when (next) { Bridge.PLANKS -> "planks"; Bridge.STONE -> "stone"; else -> "lanterns" }}") to false
            }
            else -> null to false
        }
        Isle(i.id, i.name, p.x, p.y, Chart.radius(s, i), i.traits, sea, b, Rules.sag(s, i, h.now, h.zone), hint, warn,
            if (i.anchoredAt != null) Years.known(i, h.now, h.zone) else 0)
    }
    val lit = Rules.gatherings(s).values.any { it.first().at.date(h.zone) == h.today }
    return Scene(isles, s.ferries.map { it.a to it.b }, Chart.fog(s, pts, h.now, h.zone), w.t("Я", "Me"),
        w.t("— здесь начинается туман —", "— here the fog begins —"), w.t("в тумане", "in the fog"), lit, w.t("С", "N"), selected)
}

@Composable
fun MapScreen(h: Home, nav: Nav) {
    val w = h.w
    val sc = scene(h)
    val scene by rememberUpdatedState(sc)
    val dp = LocalDensity.current.density
    val top = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
    val ctx = LocalContext.current
    ChartArt.fonts(ctx)
    var size by remember { mutableStateOf(IntSize.Zero) }
    var view by remember { mutableStateOf<View?>(null) }
    var fitFor by remember { mutableStateOf(-1) }
    if (size.width > 0 && (view == null || fitFor != sc.isles.size)) {
        // Room at the top for the cartouche and at the bottom for the buttons.
        val f = ChartArt.fit(sc, size.width.toFloat(), size.height - 150 * dp)
        view = f.copy(ty = f.ty + 20 * dp)
        fitFor = sc.isles.size
    }
    val time = if (sc.isles.any { it.sea == Sea.CREAKING || it.sea == Sea.NEW || it.bridge == Bridge.ARCH } || sc.lit) rememberTime() else 0.0
    Box(Modifier.fillMaxSize().onSizeChanged { size = it }) {
        NativeCanvas(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { c, pan, zoom, _ ->
                        val v = view ?: return@detectTransformGestures
                        val k = (v.k * zoom).coerceIn(.3f, 3.5f)
                        val z = k / v.k
                        val cx = c.x - size.width / 2f; val cy = c.y - size.height / 2f
                        view = View(cx - (cx - v.tx) * z + pan.x, cy - (cy - v.ty) * z + pan.y, k)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { o ->
                            val v = view ?: return@detectTapGestures
                            when (val id = ChartArt.hit(scene, o.x, o.y, size.width.toFloat(), size.height.toFloat(), v)) {
                                null -> Unit
                                -1L -> nav.open(Overlay.Lighthouse)
                                else -> nav.open(Overlay.Isle(id))
                            }
                        },
                        onDoubleTap = { view = null; fitFor = -1 },
                    )
                },
        ) { c, cw, ch -> view?.let { ChartArt.draw(c, cw, ch, sc, it, time, dp, top) } }

        Column(Modifier.statusBarsPadding().padding(start = 14.dp, top = 12.dp).widthIn(max = 250.dp).ruled(C.light).padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(w.t("Мой архипелаг", "My archipelago"), style = T.Title)
            Text("${w.islandsN(h.s.islands.size)} · ${w.ferriesN(h.s.ferries.size)}", style = T.Note)
        }
        Icon(Icons.Outlined.Settings, w.t("Настройки", "Settings"), tint = C.muted,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 108.dp, end = 12.dp).size(40.dp).clickable { nav.open(Overlay.Settings) }.padding(8.dp))

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(14.dp)) {
            val years = Years.anniversaries(h.s, h.now, h.zone)
            for ((i, y) in years.take(2)) {
                Card(Modifier.clickable { nav.open(Overlay.Isle(i.id)) }) {
                    Text(w.t("Годовщина: ${i.name}", "Anniversary: ${i.name}"), style = T.Title)
                    Text(w.t("Сегодня ${w.plural(y, "год", "года", "лет").let { "$y $it" }}, как этот остров на карте. Хороший повод написать.",
                        "${if (y == 1) "A year" else "$y years"} today since this island was charted. A good reason to write."), style = T.Note)
                }
                Gap(8)
            }
            if (h.s.islands.isEmpty()) {
                Card {
                    Text(w.t("Карта пуста — пока", "The chart is empty — for now"), style = T.Title)
                    Text(w.t("Каждый человек, с которым ты познакомишься, станет островом. Начни с вылазки или нанеси того, кого уже знаешь.",
                        "Every person you meet becomes an island. Start with a sortie, or chart someone you already know."), style = T.Note)
                }
                Gap(8)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LineButton(w.t("✦ Маяк", "✦ Lighthouse"), Modifier.weight(1f), enabled = h.s.islands.size >= 2) { nav.open(Overlay.Lighthouse) }
                RedButton(w.t("+ Нанести остров", "+ Chart an island"), Modifier.weight(1.3f)) { nav.open(Overlay.New) }
            }
        }
    }
}
