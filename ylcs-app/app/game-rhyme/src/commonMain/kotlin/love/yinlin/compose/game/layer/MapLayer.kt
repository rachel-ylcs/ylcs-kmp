package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.font.FontWeight
import love.yinlin.app.game_rhyme.resources.Res as RhymeRes
import love.yinlin.app.game_rhyme.resources.music
import love.yinlin.app.global.resources.Res as GlobalRes
import love.yinlin.app.global.resources.xwwk
import love.yinlin.compose.game.character.Character
import love.yinlin.compose.game.character.CharacterLiDiShiGongFenB
import love.yinlin.compose.game.character.CharacterPiFuDuHai
import love.yinlin.compose.game.common.BlockMapGenerator
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.drawer.InitialDrawer
import love.yinlin.compose.game.drawer.TextGraph
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.viewport.Camera
import love.yinlin.compose.game.visible.Block
import love.yinlin.compose.game.visible.CornerTail
import love.yinlin.extension.then

// 地图层
@Stable
class MapLayer(
    private val camera: Camera,
    private val character: Character,
    private val playInfo: RhymePlayInfo,
    val momentLayer: MomentLayer,
    val backgroundLayer: BackgroundLayer,
    private val interactLayer: InteractLayer,
    private val uiLayer: UILayer,
) : Layer(layerOrder = 2) {
    companion object {
        // 镜头跟随安全区域比例
        const val CAMERA_BLOCK_AREA_RATIO = 0.6f
    }

    // 地图
    private val blocks = BlockMapGenerator.generate(
        blockDimension = Block.DEFAULT_DIMENSION,
        lyricsConfig = playInfo.lyricsConfig,
        playConfig = playInfo.playConfig,
        character = character
    )

    // 当前位置 用于相机跟随 与音频发声一致
    private var currentIndex: Int = 0
    // 预准备位置 用于提前显示动画
    private var prepareIndex: Int = -1

    override val interactive: Boolean = false

    var baseNoteFontMap: List<TextGraph>? = null
        private set

    private var lyricsTextBuilder: ((String) -> TextGraph)? = null
    val lyricsTextMap = mutableMapOf<String, TextGraph>()

    override fun preUpdate(tick: Int) {
        // 检查新方块
        blocks.getOrNull(prepareIndex + 1)?.then { nextBlock ->
            if (momentLayer.audioPosition >= nextBlock.time.appearance) {
                // 到达方块出现刻
                ++prepareIndex
                // 生成文字
                lyricsTextBuilder?.then { builder ->
                    val ch = nextBlock.rhymeAction.ch
                    if (!lyricsTextMap.containsKey(ch)) lyricsTextMap[ch] = builder(ch)
                }
                // 加入序列
                this += nextBlock
            }
        }

        // 处理方块交互
        interactLayer.withInteractInfo { interactStatusList ->
            blocks.getOrNull(currentIndex)?.then { currentBlock ->
                // 只在交互状态下触发
                when (val blockStatus = currentBlock.blockStatus) {
                    is BlockStatus.Interact -> currentBlock.onInteract(interactStatusList, blockStatus)
                    is BlockStatus.Release -> {
                        // 更新位置
                        val newIndex = currentIndex + 1
                        currentIndex = newIndex

                        // --  边角判定  --
                        if (character !is CharacterLiDiShiGongFenB) {
                            val currentLine = currentBlock.line
                            if (currentBlock.rawIndex == currentLine.lastRawIndex) { // 检查是否是末尾
                                // 添加尾角动画
                                currentLine.endDirection?.then { endDirection ->
                                    this += CornerTail.build(currentBlock, currentLine.startDirection, endDirection)
                                }
                            }
                        }

                        // 检查相机跟踪
                        blocks.getOrNull(newIndex)?.then { nextBlock ->
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
                            if (blockLeft <= limitLeft || blockRight >= limitRight || blockTop <= limitTop || blockBottom >= limitBottom) {
                                camera.animateUpdatePosition(center)
                            }
                        }
                    }
                    else -> { }
                }
            }
        }
    }

    override fun InitialDrawer.preInitialDraw() {
        baseNoteFontMap = Block.NoteScaleFontMap.map { index ->
            measureText(index.toString(), font = RhymeRes.font.music, fontWeight = FontWeight.Bold)
        }
        lyricsTextBuilder = { text ->
            measureText(text, font = GlobalRes.font.xwwk, fontWeight = FontWeight.Bold)
        }
        if (character is CharacterPiFuDuHai) camera.updateScale(1 / (1 + character.range)) // 蚍蜉渡海具备额外视野
    }

    fun updateBlockResult(block: Block<*>, result: BlockResult) {
        val skillResult = character.modifyResult(playInfo.musicInfo, playInfo.playConfig, block, result)
        if (skillResult.active) backgroundLayer.activateSkill()
        else if (skillResult.dirty) backgroundLayer.updateSkill()
        uiLayer.updateResult(skillResult)
    }
}