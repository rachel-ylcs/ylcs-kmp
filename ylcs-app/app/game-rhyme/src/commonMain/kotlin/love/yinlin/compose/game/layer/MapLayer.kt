package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.common.BlockMapGenerator
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.viewport.Camera
import love.yinlin.compose.game.visible.Block
import love.yinlin.media.AudioPlayer

// 地图层
@Stable
class MapLayer(
    private val camera: Camera,
    private val player: AudioPlayer,
    playInfo: RhymePlayInfo
) : Layer(layerOrder = 1), Dynamic {
    companion object {
        const val CAMERA_BLOCK_AREA_RATIO = 0.8f
    }

    // 地图
    private val blocks = BlockMapGenerator.generate(Block.DEFAULT_DIMENSION, playInfo.lyricsConfig, playInfo.playConfig)

    var audioPosition: Long = 0L
    var currentIndex = -1
        private set
    private var prepareIndex = -1

    override fun preUpdate(tick: Int) {
        val currentAudioPosition = player.position
        audioPosition = currentAudioPosition

        // 检查新方块
        blocks.getOrNull(prepareIndex + 1)?.let { nextBlock ->
            if (currentAudioPosition >= nextBlock.time.rawAppearance) {
                // 到达方块出现刻
                ++prepareIndex
                this += nextBlock
            }
        }

        // 检查当前方块
        blocks.getOrNull(currentIndex + 1)?.let { nextBlock ->
            if (currentAudioPosition >= nextBlock.time.rawInteract) { // 当方块可交互的时候
                ++currentIndex

                // 检查相机跟踪
                val boundary = camera.viewportBounds
                val horizontalMargin = boundary.width * (1f - CAMERA_BLOCK_AREA_RATIO) / 2f
                val verticalMargin = boundary.height * (1f - CAMERA_BLOCK_AREA_RATIO) / 2f

                val limitLeft = boundary.left + horizontalMargin
                val limitRight = boundary.right - horizontalMargin
                val limitTop = boundary.top + verticalMargin
                val limitBottom = boundary.bottom - verticalMargin

                val (halfWidth, halfHeight) = nextBlock.size / 2f
                val center = nextBlock.position

                val blockLeft = center.x - halfWidth
                val blockRight = center.x + halfWidth
                val blockTop = center.y - halfHeight
                val blockBottom = center.y + halfHeight

                // 当前方块在视口边界的限制外
                if (blockLeft < limitLeft || blockRight > limitRight || blockTop < limitTop || blockBottom > limitBottom) {
                    camera.animateUpdatePosition(center)
                }
            }
        }
    }
}