package love.yinlin.platform

import androidx.compose.runtime.Stable
import korlibs.korge.Korge
import korlibs.korge.KorgeDisplayMode
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.math.geom.Anchor2D
import korlibs.math.geom.ScaleMode
import korlibs.math.geom.Size2D
import love.yinlin.AppModel
import love.yinlin.data.rachel.game.Game

@Stable
actual abstract class KorgeGame actual constructor(
    actual val appModel: AppModel,
    actual val appContext: AppContext
) {
    private val actualContext = appContext as ActualAppContext

    actual abstract val mainScene: Scene

    protected actual val korge: Korge = Korge(
        title = Game.Rhyme.title,
        windowSize = Size2D(1280, 720),
        virtualSize = Size2D(1280, 720),
        displayMode = KorgeDisplayMode(ScaleMode.SHOW_ALL, Anchor2D.CENTER, false),
        main = {
            sceneContainer().changeTo { mainScene }
        }
    )

    actual suspend fun launch() {

    }
}