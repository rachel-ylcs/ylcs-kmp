package love.yinlin.ui.screen.world.single

import androidx.compose.runtime.Stable
import korlibs.image.color.Colors
import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.solidRect
import love.yinlin.AppModel
import love.yinlin.platform.AppContext
import love.yinlin.platform.KorgeGame

@Stable
class RhymeGame(appModel: AppModel, appContext: AppContext) : KorgeGame(appModel, appContext) {
    class SceneStart : Scene() {
        override suspend fun SContainer.sceneMain() {
            solidRect(200, 200, Colors.RED)
        }
    }

    override val mainScene: Scene get() = SceneStart()
}