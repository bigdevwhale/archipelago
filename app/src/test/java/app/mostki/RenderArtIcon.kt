package app.mostki

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The launcher icon's two layers drawn together (run with `-Prender`), and docs/logo.png from them. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArtIcon {
    @Test fun icon() {
        val c = ApplicationProvider.getApplicationContext<MostkiApp>()
        val n = 512
        val b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        for (id in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) ContextCompat.getDrawable(c, id)!!.apply { setBounds(0, 0, n, n); draw(cv) }
        val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }
        File(out, "icon.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        File(out, "../../../docs/logo.png").apply { parentFile?.mkdirs() }.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
