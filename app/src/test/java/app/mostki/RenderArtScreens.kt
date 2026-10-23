package app.mostki

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.mostki.data.DemoSeed
import app.mostki.ui.MostkiRoot
import app.mostki.ui.Overlay
import app.mostki.ui.Tab
import app.mostki.ui.Words
import app.mostki.ui.components.Art
import app.mostki.ui.onboarding.Onboarding
import app.mostki.ui.theme.MostkiTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo chart, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    @Before fun seed() {
        Art.still = true
        val app = ApplicationProvider.getApplicationContext<MostkiApp>()
        runBlocking { DemoSeed.seedIfEmpty(app, app.container) }
    }

    private fun shot(name: String, tab: Tab = Tab.CHART, overlay: Overlay? = null, welcome: Boolean = false) {
        val app = ApplicationProvider.getApplicationContext<MostkiApp>()
        val settings = runBlocking { app.container.settings.now() }
        rule.mainClock.autoAdvance = false
        rule.setContent { MostkiTheme { if (welcome) Onboarding(Words(app.ru)) else MostkiRoot(settings, tab, overlay) } }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        Thread.sleep(600)
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun chart() = shot("1-chart")
    @Test fun island() = shot("2-island", overlay = Overlay.Isle(1))
    @Test fun newIsland() = shot("3-kite", overlay = Overlay.Isle(4))
    @Test fun sortie() = shot("4-sortie", Tab.SORTIE)
    @Test fun atlas() = shot("5-atlas", Tab.ATLAS)
    @Test fun log() = shot("6-log", Tab.LOG)
    @Test fun lighthouse() = shot("7-lighthouse", overlay = Overlay.Lighthouse)
    @Test fun fog() = shot("8-fog", overlay = Overlay.Isle(6))
    @Test fun chartAnIsland() = shot("9-new", overlay = Overlay.New)
    @Test fun welcome() = shot("0-welcome", welcome = true)
}

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() { override val lang = "ru" }

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderArtScreensEn : ScreensBase() { override val lang = "en" }
