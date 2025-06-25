package love.yinlin.platform

import androidx.compose.runtime.Stable
import korlibs.korge.Korge
import korlibs.korge.scene.Scene
import love.yinlin.AppModel

@Stable
expect abstract class KorgeGame(
    appModel: AppModel,
    appContext: AppContext
) {
    protected val appModel: AppModel
    protected val appContext: AppContext
    protected val korge: Korge

    abstract val mainScene: Scene

    suspend fun launch()
}