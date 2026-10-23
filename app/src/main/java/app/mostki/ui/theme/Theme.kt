package app.mostki.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mostki.R
import kotlin.random.Random

/** An old sea chart: rag paper, iron-gall ink, a red for the things to do, gold for lanterns. */
@Immutable
object C {
    val paper = Color(0xFFEFE3C6)
    val paper2 = Color(0xFFE6D5AB)
    val paper3 = Color(0xFFD8C291)
    val light = Color(0xFFF7EED9)
    val ink = Color(0xFF3A2D1F)
    val muted = Color(0xFF6B5841)
    val faint = Color(0xFFA08C6C)
    val red = Color(0xFF8C2F1C)
    val redSoft = Color(0xFFE9C9B4)
    val gold = Color(0xFFE2A43A)
    val sea = Color(0xFF5F7A74)
    val shade = Color(0x88241C12)
}

/** PT Serif for everything printed on the chart. */
val Serif = FontFamily(
    Font(R.font.pt_serif_regular, FontWeight.Normal),
    Font(R.font.pt_serif_bold, FontWeight.Bold),
    Font(R.font.pt_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Caveat: names written in by hand. */
val Hand = FontFamily(Font(R.font.caveat, FontWeight.Normal))

object T {
    val Display = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 32.sp, color = C.ink)
    val Title = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Serif, fontSize = 16.sp, lineHeight = 23.sp, color = C.ink)
    val Muted = TextStyle(fontFamily = Serif, fontSize = 14.sp, lineHeight = 20.sp, color = C.muted)
    val Note = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 14.sp, lineHeight = 20.sp, color = C.muted)
    val Small = TextStyle(fontFamily = Serif, fontSize = 12.sp, lineHeight = 16.sp, color = C.muted)
    val Task = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, color = C.ink)
    val Name = TextStyle(fontFamily = Hand, fontSize = 40.sp, lineHeight = 40.sp, color = C.ink)
    val HandBody = TextStyle(fontFamily = Hand, fontSize = 23.sp, lineHeight = 26.sp, color = C.ink)
    val Button = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    val Label = TextStyle(fontFamily = Serif, fontSize = 11.5.sp, letterSpacing = 1.sp, color = C.muted)
}

object Shapes {
    val Sm = RoundedCornerShape(2.dp)
    val Pill = RoundedCornerShape(50)
}

/** The rag paper behind every screen: fibres and foxing, tiled. */
object Paper {
    private var tile: Bitmap? = null

    fun tile(): Bitmap = tile ?: run {
        val n = 256
        val b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(0xFFEFE3C6.toInt())
        val r = Random(9)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        repeat(200) {
            p.color = if (r.nextBoolean()) 0x12A88A5C else 0x10FFF8E6
            p.strokeWidth = 1f + r.nextFloat()
            val x = r.nextFloat() * n
            val y = r.nextFloat() * n
            val l = 5 + r.nextFloat() * 18
            for (dx in intArrayOf(-n, 0, n)) for (dy in intArrayOf(-n, 0, n)) c.drawLine(x + dx, y + dy, x + dx + l, y + dy + (r.nextFloat() - .5f) * 3, p)
        }
        repeat(420) {
            p.color = 0x1EA07E50
            c.drawCircle(r.nextFloat() * n, r.nextFloat() * n, .5f + r.nextFloat() * .8f, p)
        }
        b.also { tile = it }
    }
}

fun Modifier.paper(): Modifier = drawBehind {
    drawRect(ShaderBrush(ImageShader(Paper.tile().asImageBitmap(), TileMode.Repeated, TileMode.Repeated)))
}

@Composable
fun MostkiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.red, onPrimary = C.light, secondary = C.ink, background = C.paper, surface = C.light,
            surfaceContainer = C.light, surfaceContainerHigh = C.light, surfaceContainerHighest = C.paper2, surfaceContainerLow = C.light,
            onBackground = C.ink, onSurface = C.ink, onSurfaceVariant = C.muted, outline = C.ink, outlineVariant = C.faint, error = C.red,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body.copy(fontSize = 15.sp), bodySmall = T.Small, labelLarge = T.Button),
        content = content,
    )
}
