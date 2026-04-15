package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.common.BlockMapGenerator
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.common.DataUpdater
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
    playInfo: RhymePlayInfo,
    private val updater: DataUpdater,
    private val interactLayer: InteractLayer,
) : Layer(layerOrder = 1), Dynamic {
    companion object {
        const val CAMERA_BLOCK_AREA_RATIO = 0.8f
    }

    // 地图
    private val blocks = BlockMapGenerator.generate(Block.DEFAULT_DIMENSION, playInfo.lyricsConfig, playInfo.playConfig)

    var audioPosition: Long = 0L
    var lastAudioPosition: Long = 0L
    // 当前位置 用于相机跟随 与音频发声一致
    private var currentIndex: Int = -1
    // 预准备位置 用于提前显示动画
    private var prepareIndex: Int = -1

    override val interactive: Boolean = false

    override fun preUpdate(tick: Int) {
        // 更新进度
        val currentAudioPosition = player.position
        val currentAudioDuration = player.duration
        // 降频
        if (currentAudioPosition - lastAudioPosition > 1000L) {
            updater.audioProgress = if (currentAudioDuration == 0L) 0f else currentAudioPosition / currentAudioDuration.toFloat()
            lastAudioPosition = currentAudioPosition
        }
        audioPosition = currentAudioPosition

        // 检查新方块
        blocks.getOrNull(prepareIndex + 1)?.let { nextBlock ->
            if (currentAudioPosition >= nextBlock.time.rawAppearance) {
                // 到达方块出现刻
                ++prepareIndex
                this += nextBlock
            }
        }

        // 处理方块交互
        blocks.getOrNull(currentIndex)?.let { currentBlock ->

        }

        // 检查当前音轨方块
        blocks.getOrNull(currentIndex + 1)?.let { nextBlock ->
            if (currentAudioPosition >= nextBlock.time.rawInteract) { // 当方块可交互的时候
                ++currentIndex

                // 检查相机跟踪
                val boundary = camera.viewportBounds
                val gapRatio = (1 - CAMERA_BLOCK_AREA_RATIO) / 2
                val horizontalMargin = boundary.width * gapRatio
                val verticalMargin = boundary.height * gapRatio

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

    fun updateResult(result: BlockResult, scoreRatio: Float = 1f) = updater.updateResult(audioPosition, result, scoreRatio)
}