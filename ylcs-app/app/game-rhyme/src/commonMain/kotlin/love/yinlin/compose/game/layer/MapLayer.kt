package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.font.FontWeight
import love.yinlin.app.game_rhyme.resources.Res as RhymeRes
import love.yinlin.app.game_rhyme.resources.music
import love.yinlin.app.global.resources.Res as GlobalRes
import love.yinlin.app.global.resources.xwwk
import love.yinlin.compose.game.common.BlockMapGenerator
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.DataUpdater
import love.yinlin.compose.game.common.InteractStatus
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.drawer.InitialDrawer
import love.yinlin.compose.game.drawer.TextGraph
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
        const val CAMERA_BLOCK_AREA_RATIO = 0.6f
    }

    // 地图
    private val blocks = BlockMapGenerator.generate(Block.DEFAULT_DIMENSION, playInfo.lyricsConfig, playInfo.playConfig)

    var audioPosition: Long = 0L
    private var lastAudioPosition: Long = 0L
    // 当前位置 用于相机跟随 与音频发声一致
    private var currentIndex: Int = 0
    // 预准备位置 用于提前显示动画
    private var prepareIndex: Int = -1

    override val interactive: Boolean = false

    var baseNoteFontMap: List<TextGraph>? = null
        private set

    var lyricsTextBuilder: ((String) -> TextGraph)? = null
    val lyricsTextMap = mutableMapOf<String, TextGraph>()

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
            if (currentAudioPosition >= nextBlock.time.appearance) {
                // 到达方块出现刻
                ++prepareIndex
                // 生成文字
                lyricsTextBuilder?.let { builder ->
                    val ch = nextBlock.rhymeAction.ch
                    lyricsTextMap.getOrPut(ch) { builder(ch) }
                }
                // 加入序列
                this += nextBlock
            }
        }

        // 处理方块交互
        val interactStatus = interactLayer.interactStatus
        blocks.getOrNull(currentIndex)?.let { currentBlock ->
            // 只在交互状态下触发
            when (val blockStatus = currentBlock.blockStatus) {
                is BlockStatus.Interact -> currentBlock.onInteract(interactStatus, blockStatus)
                is BlockStatus.Release -> {
                    // 检查相机跟踪
                    blocks.getOrNull(++currentIndex)?.let { nextBlock ->
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
                else -> { }
            }
        }
        interactStatus.fill(InteractStatus.None) // 重置状态
    }

    override suspend fun InitialDrawer.preInitialDraw() {
        baseNoteFontMap = Block.NoteScaleFontMap.map { index ->
            measureText(index.toString(), font = RhymeRes.font.music, fontWeight = FontWeight.Bold)
        }
        lyricsTextBuilder = { text ->
            measureText(text, font = GlobalRes.font.xwwk, fontWeight = FontWeight.Bold)
        }
        updateDirty()
    }

    fun updateResult(result: BlockResult, scoreRatio: Float = 1f) {
        updater.updateResult(audioPosition, result, scoreRatio)
    }
}