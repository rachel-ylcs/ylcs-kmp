package love.yinlin.platform

import androidx.compose.runtime.Stable
import korlibs.korge.Korge
import korlibs.korge.scene.Scene
import love.yinlin.AppModel

@Stable
actual abstract class KorgeGame actual constructor(
    actual val appModel: AppModel,
    actual val appContext: AppContext
) {
    private val actualContext = appContext as ActualAppContext

    actual abstract val mainScene: Scene

    protected actual val korge: Korge = Korge()

    actual suspend fun launch() {

    }
}