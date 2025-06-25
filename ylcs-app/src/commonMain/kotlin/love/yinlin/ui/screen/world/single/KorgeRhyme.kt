package love.yinlin.ui.screen.world.single

import androidx.compose.runtime.Stable
import korlibs.image.color.RGBA
import korlibs.korge.input.onClickSuspend
import korlibs.korge.scene.Scene
import korlibs.korge.ui.uiButton
import korlibs.korge.view.SContainer
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import love.yinlin.AppModel
import love.yinlin.platform.AppContext
import love.yinlin.platform.KorgeGame
import kotlin.random.Random

@Stable
class RhymeGame(appModel: AppModel, appContext: AppContext) : KorgeGame(appModel, appContext) {
    class SceneStart : Scene() {
        override suspend fun SContainer.sceneMain() {
            solidRect(200, 200, RGBA(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255)))

            uiButton(label = "Reset").position(stage!!.width - 200, 200).onClickSuspend {
                sceneContainer.changeTo { SceneStart() }
            }
        }
    }

    override val mainScene: Scene get() = SceneStart()
}