package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.font.FontWeight
import love.yinlin.app.game_rhyme.resources.Res as RhymeRes
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.app.global.resources.Res as GlobalRes
import love.yinlin.app.global.resources.xwwk
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.drawer.InitialDrawer
import love.yinlin.compose.game.drawer.LayerType
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.visible.UICover
import love.yinlin.compose.game.visible.UIMusicInfo
import love.yinlin.compose.game.visible.UIScore

@Stable
class UILayer(private val info: RhymePlayInfo) : Layer(
    layerOrder = 4,
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
        this += listOf(uiCover, uiMusicInfo, uiScore)
    }

    override fun InitialDrawer.preInitialDraw() {
        uiMusicInfo.title = measureText(info.musicInfo.name, GlobalRes.font.xwwk, FontWeight.Bold)
        uiScore.textBuilder = { text -> measureText(text, RhymeRes.font.rhyme, FontWeight.Bold) }
        uiScore.scoreGraph = measureText("0", RhymeRes.font.rhyme, FontWeight.Bold)
    }
}