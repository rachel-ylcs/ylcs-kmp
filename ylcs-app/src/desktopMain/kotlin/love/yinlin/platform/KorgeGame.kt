package love.yinlin.platform

import androidx.compose.runtime.Stable
import korlibs.image.awt.toAwt
import korlibs.image.bitmap.Bitmap
import korlibs.korge.Korge
import korlibs.korge.KorgeDisplayMode
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.math.geom.Anchor2D
import korlibs.math.geom.ScaleMode
import korlibs.math.geom.Size2D
import korlibs.render.GameWindowCreationConfig
import korlibs.render.awt.*
import korlibs.render.platform.BaseOpenglContext
import korlibs.render.platform.glContextFromComponent
import love.yinlin.AppModel
import love.yinlin.data.rachel.game.Game
import love.yinlin.extension.catching
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame

private class ActualAwtGameWindow : BaseAwtGameWindow(AGOpenglAWT(GameWindowCreationConfig())) {
    override var ctx: BaseOpenglContext? = null

    override fun ensureContext() {
        if (ctx == null) ctx = glContextFromComponent(frame, this)
    }

    val frame: JFrame = object : JFrame("Korgw") {
        init {
            isVisible = false
            ignoreRepaint = true
            isUndecorated = false
            isResizable = false
            exitProcessOnClose = false
            background = Color(0, 0, 0, 255)
            setBounds(0, 0, 1280, 720)
            setLocationRelativeTo(null)
            setKorgeDropTarget(this@ActualAwtGameWindow)
            setIconIncludingTaskbarFromResource("@appicon.png")
            initTools()
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    running = false
                }
            })
        }

        override fun paintComponents(g: Graphics?) = Unit
        override fun paint(g: Graphics) = catching { framePaint(g) }
    }

    override var alwaysOnTop: Boolean by frame::_isAlwaysOnTop
    override var title: String by frame::title
    override var icon: Bitmap? = null
        set(value) {
            field = value
            frame.setIconIncludingTaskbarFromImage(value?.toAwt())
        }
    override var fullscreen: Boolean by frame::isFullScreen

    override fun setSize(width: Int, height: Int) {
        contentComponent.setSize(width, height)
        contentComponent.preferredSize = Dimension(width, height)
        frame.pack()
        frame.setLocationRelativeTo(null)
    }

    override val component: Component get() = frame
    override val contentComponent: Component get() = frame.contentPane
    override fun frameDispose() = frame.dispose()
}


@Stable
actual abstract class KorgeGame actual constructor(
    actual val appModel: AppModel,
    actual val appContext: AppContext
) {
    private val actualContext = appContext as ActualAppContext

    actual abstract val mainScene: Scene

    actual val korge: Korge = Korge(
        gameWindow = ActualAwtGameWindow(),
        title = Game.Rhyme.title,
        windowSize = Size2D(1280, 720),
        virtualSize = Size2D(1280, 720),
        displayMode = KorgeDisplayMode(ScaleMode.SHOW_ALL, Anchor2D.CENTER, false),
        main = {
            sceneContainer().changeTo { mainScene }
        }
    )

    actual suspend fun launch() {
        actualContext.windowVisible = false
        Coroutines.startIO {
            try {
                korge.start()
            }
            finally {
                actualContext.windowVisible = true
            }
        }
    }
}