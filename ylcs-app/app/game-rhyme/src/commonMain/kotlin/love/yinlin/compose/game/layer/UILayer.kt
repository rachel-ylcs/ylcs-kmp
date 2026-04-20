package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.drawer.LayerType
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.visible.UICover
import love.yinlin.compose.game.visible.UIMusicInfo
import love.yinlin.compose.game.visible.UIScore

@Stable
class UILayer(info: RhymePlayInfo) : Layer(
    layerOrder = 3,
    layerType = LayerType.Absolute
) {
    companion object {
        const val DEFAULT_PADDING: Float = 25f
        const val DEFAULT_HEIGHT: Float = 175f
    }

    val uiCover = UICover(info)
    val uiMusicInfo = UIMusicInfo(info)
    val uiScore = UIScore(info.playConfig)

    override fun onLayerAttached(scene: ScenePlugin) {
        this += uiCover
        this += uiMusicInfo
        this += uiScore
    }
}