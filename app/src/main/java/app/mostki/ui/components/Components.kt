package app.mostki.ui.components

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.mostki.ui.theme.C
import app.mostki.ui.theme.Shapes
import app.mostki.ui.theme.T
import app.mostki.ui.theme.paper

/** Screens are drawn still for the renders and screenshots: no creaking, no flicker. */
object Art {
    @Volatile var still = false
}

/** Seconds since the screen appeared, ticking every frame (still for the renders). */
@Composable
fun rememberTime(): Double {
    var t by remember { mutableDoubleStateOf(0.0) }
    LaunchedEffect(Unit) {
        if (Art.still) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t = (it - start) / 1e9 }
    }
    return t
}

/** Draws with an android.graphics.Canvas: the art objects are written for it. */
@Composable
fun NativeCanvas(modifier: Modifier, draw: (android.graphics.Canvas, Float, Float) -> Unit) {
    Canvas(modifier.clipToBounds()) { drawContext.canvas.nativeCanvas.let { draw(it, size.width, size.height) } }
}

/** The double rule of a chart's cartouche, round a block of text. */
fun Modifier.ruled(bg: Color = C.light): Modifier = this
    .background(bg)
    .border(1.5.dp, C.ink)
    .drawBehind {
        val e = 4.dp.toPx()
        drawRect(C.ink.copy(alpha = .35f), Offset(e, e), androidx.compose.ui.geometry.Size(size.width - 2 * e, size.height - 2 * e), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
    }

@Composable
fun Card(modifier: Modifier = Modifier, bg: Color = C.light, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().ruled(bg).padding(horizontal = 16.dp, vertical = 14.dp), content = content)
}

/** The red button, for the thing to do. */
@Composable
fun RedButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    // These buttons save and close: a second tap while the sheet is going would save twice.
    val last = remember { longArrayOf(0) }
    Box(
        modifier
            .height(48.dp)
            .background(if (enabled) C.red else C.paper3, Shapes.Sm)
            .then(if (enabled) Modifier.clickable {
                val now = SystemClock.uptimeMillis()
                if (now - last[0] > 700) { last[0] = now; onClick() }
            } else Modifier)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = T.Button, color = if (enabled) C.light else C.faint, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

/** An inked outline, for the second thing. */
@Composable
fun LineButton(text: String, modifier: Modifier = Modifier, dashed: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .height(46.dp)
            .background(if (dashed) Color.Transparent else C.light, Shapes.Sm)
            .border(1.5.dp, if (enabled) (if (dashed) C.muted else C.ink) else C.faint, Shapes.Sm)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = T.Button, color = if (enabled) C.ink else C.faint, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .background(if (on) C.ink else Color.Transparent, Shapes.Pill)
            .border(1.dp, if (on) C.ink else C.muted, Shapes.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) { Text(text, style = T.Muted.copy(color = if (on) C.light else C.ink)) }
}

@Composable
fun Flow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { content() }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier) = Text(text.uppercase(), style = T.Label, modifier = modifier)

@Composable
fun Gap(h: Int = 10) = Spacer(Modifier.height(h.dp))

@Composable
fun Section(title: String, sub: String? = null) {
    Gap(18)
    Text(title, style = T.Title)
    if (sub != null) Text(sub, style = T.Note)
    Gap(6)
}

/** A text field drawn as a line on the paper. */
@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, style: TextStyle = T.Body, single: Boolean = true) {
    Box(modifier.fillMaxWidth().background(C.light, Shapes.Sm).border(1.dp, C.muted, Shapes.Sm).padding(horizontal = 12.dp, vertical = 10.dp)) {
        if (value.isEmpty()) Text(hint, style = style.copy(color = C.faint))
        BasicTextField(value, onChange, Modifier.fillMaxWidth(), textStyle = style, singleLine = single, cursorBrush = SolidColor(C.red),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
    }
}

/** A full-screen sheet over the chart, with a title and a way back. */
@Composable
fun Sheet(title: String?, close: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(C.shade).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 28.dp).paper().imePadding()) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(C.ink))
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title.orEmpty(), style = T.Title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(close, style = T.Button.copy(color = C.muted), modifier = Modifier.clickable(onClick = onClose).padding(12.dp))
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 24.dp), content = content)
        }
    }
}

/** The title strip of a tab: a name in print and a line under it. */
@Composable
fun Heading(title: String, sub: String? = null, end: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Display)
            if (sub != null) Text(sub, style = T.Note)
        }
        end()
    }
}

/**
 * Hours together on a scale to 220, with Hall's three thresholds marked; [labels] are written under
 * the marks, [top] over them.
 */
@Composable
fun HoursBar(minutes: Int, marks: List<Int>, top: List<String>, labels: List<String>, modifier: Modifier = Modifier) {
    val max = 220f
    BoxWithConstraints(modifier.fillMaxWidth().height(58.dp)) {
        val w = maxWidth
        Box(Modifier.offset(y = 20.dp).fillMaxWidth().height(14.dp).background(C.light).border(1.5.dp, C.ink)) {
            Box(Modifier.fillMaxWidth((minutes / 60f / max).coerceIn(0f, 1f)).height(14.dp).padding(2.dp).background(C.muted))
        }
        marks.forEachIndexed { i, h ->
            val x = w * (h / max)
            Box(Modifier.offset(x = x - 1.dp, y = 15.dp).height(24.dp).background(C.red).padding(horizontal = 1.dp))
            Text(top[i], style = T.Small.copy(color = C.red), textAlign = TextAlign.Center, modifier = Modifier.offset(x = x - 30.dp, y = 0.dp).width(60.dp))
            // The last label would run off the edge: it ends at the mark instead of centring on it.
            val last = i == marks.lastIndex
            Text(labels[i], style = T.Small, textAlign = if (last) TextAlign.End else TextAlign.Center, maxLines = 1,
                modifier = Modifier.offset(x = if (last) x - 108.dp else x - 50.dp, y = 40.dp).width(if (last) 112.dp else 100.dp))
        }
    }
}
