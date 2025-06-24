package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import korlibs.image.color.RGBA
import korlibs.korge.Korge
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.view.SContainer
import korlibs.korge.view.solidRect
import korlibs.render.GameWindowCreationConfig
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.data.rachel.game.Game
import love.yinlin.ui.component.platform.KorgeState
import love.yinlin.ui.component.platform.KorgeView
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenRhyme(model: AppModel) : CommonSubScreen(model) {
    override val title: String = Game.Rhyme.title

    @Composable
    override fun SubContent(device: Device) {
        KorgeView(remember { KorgeState(GameWindowCreationConfig(), Korge(main = {
            sceneContainer().changeTo {
                object : Scene() {
                    override suspend fun SContainer.sceneMain() {
                        solidRect(100, 100, RGBA(0x46, 0x82, 0xb4))
                    }
                }
            }
        })) })
    }
}