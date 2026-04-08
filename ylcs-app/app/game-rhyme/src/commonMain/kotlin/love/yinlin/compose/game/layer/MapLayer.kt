package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.common.BlockMapGenerator
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.viewport.Camera
import love.yinlin.media.AudioPlayer

// 地图层
@Stable
class MapLayer(
    private val camera: Camera,
    private val player: AudioPlayer,
    playInfo: RhymePlayInfo
) : Layer(layerOrder = 1), Dynamic {
    // 地图
    private val blocks = BlockMapGenerator.generate(200f, playInfo.lyricsConfig, playInfo.playConfig)

    private var currentIndex = -1
    private var prepareIndex = -1

    override fun preUpdate(tick: Int) {
        val audioPosition = player.position
        val nextBlock = blocks.getOrNull(currentIndex + 1)
        if (nextBlock != null) {
            if (audioPosition >= nextBlock.timeStart) {
                // 相机跟踪下一个方块
                camera.animateUpdatePosition(nextBlock.position)
                ++currentIndex
            }
        }
        val nextPrepareBlock = blocks.getOrNull(prepareIndex + 1)
        if (nextPrepareBlock != null) {
            if (audioPosition >= nextPrepareBlock.timeAppearance) {
                // 到达方块出现刻
                ++prepareIndex
                this += nextPrepareBlock
            }
        }
    }
}