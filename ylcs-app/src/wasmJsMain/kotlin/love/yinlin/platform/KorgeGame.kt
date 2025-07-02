package love.yinlin.platform

import androidx.compose.runtime.Stable
import korlibs.io.async.launchImmediately
import korlibs.korge.Korge
import korlibs.korge.KorgeDisplayMode
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.math.geom.Anchor2D
import korlibs.math.geom.ScaleMode
import korlibs.math.geom.Size2D
import korlibs.render.BrowserCanvasJsGameWindow
import korlibs.render.GameWindow
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancelAndJoin
import love.yinlin.AppModel
import love.yinlin.data.rachel.game.Game
import org.w3c.dom.HTMLCanvasElement

private class ActualWasmGameWindow(canvas: HTMLCanvasElement) : BrowserCanvasJsGameWindow(canvas) {
    private var fakeLoopJob: Job? = null
    private var fakeJsFrame: (Double) -> Unit

    init {
        fakeJsFrame = { _: Double ->
            window.requestAnimationFrame(fakeJsFrame)
            frame()
        }
    }

    // 手动模拟Canvas绘制第一帧
    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        fakeLoopJob = launchImmediately(getCoroutineDispatcherWithCurrentContext()) {
            entry()
        }
        fakeJsFrame(0.0)
    }
    // 取消引擎关闭窗口, 而是还原compose的Canvas
    override fun close(exitCode: Int) {
        MainScope().launchImmediately {
            fakeLoopJob?.cancelAndJoin()
        }
        fakeLoopJob = null
    }
}

@Stable
actual abstract class KorgeGame actual constructor(
    actual val appModel: AppModel,
    actual val appContext: AppContext
) {
    private val actualContext = appContext as ActualAppContext

    actual abstract val mainScene: Scene

    private val canvas = (document.createElement("canvas") as HTMLCanvasElement).apply {
        setAttribute("z-index", "1")
    }

    protected actual val korge: Korge = Korge(
        gameWindow = ActualWasmGameWindow(canvas),
        title = Game.Rhyme.title,
        windowSize = Size2D(1280, 720),
        virtualSize = Size2D(1280, 720),
        displayMode = KorgeDisplayMode(ScaleMode.SHOW_ALL, Anchor2D.CENTER, false),
        main = {
            sceneContainer().changeTo { mainScene }
        }
    )

    actual suspend fun launch() {
        document.body?.let { body ->
            body.appendChild(canvas)
            korge.start()
            body.removeChild(canvas)
        }
    }
}