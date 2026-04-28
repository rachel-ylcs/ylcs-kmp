package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.fastForEach
import love.yinlin.app.game_rhyme.resources.Res as RhymeRes
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.app.global.resources.Res as GlobalRes
import love.yinlin.app.global.resources.xwwk
import love.yinlin.compose.Colors
import love.yinlin.compose.extension.scale
import love.yinlin.compose.game.character.Character
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.common.FPSCounter
import love.yinlin.compose.game.data.RhymeDifficulty
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.InitialDrawer
import love.yinlin.compose.game.drawer.LayerType
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.drawer.StrokeTextGraph
import love.yinlin.compose.game.drawer.TextGraph
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.ui.StarPaths
import love.yinlin.data.rachel.rhyme.RhymePlayResult

@Stable
class UILayer(
    private val character: Character,
    private val info: RhymePlayInfo,
    private val momentLayer: MomentLayer
) : Layer(
    layerOrder = 4,
    layerType = LayerType.Absolute
) {
    companion object {
        private val TextStroke = Stroke(2f)
    }

    override val interactive: Boolean = false

    private var viewportWidth: Float = 0f
    private var viewportHeight: Float = 0f

    // 音乐信息
    private var audioProgress: Float = 0f
    private var lastAudioPosition: Long = 0L
    private var title: TextGraph? = null

    // FPS统计
    private val fpsCounter = FPSCounter(3000)
    private val fpsCache = mutableMapOf<Int, TextGraph>()
    private val fpsColor = Colors.White.copy(alpha = 0.3f)
    private var currentFpsGraph: TextGraph? = null
    private var fpsTextBuilder: ((Int) -> TextGraph)? = null

    private class ResultData(
        val result: BlockResult,
        val combo: Int,
        val resultGraph: StrokeTextGraph,
        val comboGraph: StrokeTextGraph?
    ) {
        companion object {
            private const val ANIMATION_DURATION = 800
            private const val OPEN_INITIAL_SCALE = 0.5f
            private const val OPEN_RATIO = 0.3f
        }

        private var tick: Int = 0
        private var rawProgress: Float = 0f
        var progress: Float = 0f
            private set

        val isOpen: Boolean get() = rawProgress < OPEN_RATIO

        fun update(delta: Int): Boolean = if (tick < ANIMATION_DURATION) {
            tick += delta
            rawProgress = tick / ANIMATION_DURATION.toFloat()
            progress = if (rawProgress < OPEN_RATIO) {
                rawProgress / OPEN_RATIO * (1 - OPEN_INITIAL_SCALE) + OPEN_INITIAL_SCALE
            } else (1 - rawProgress) / (1 - OPEN_RATIO)
            true
        } else false
    }

    // 难度
    private val difficulty = info.playConfig.difficulty
    // 连击奖励
    private val comboRewardCount = when (info.playConfig.difficulty) {
        RhymeDifficulty.Easy -> 30
        RhymeDifficulty.Medium -> 25
        RhymeDifficulty.Hard -> 20
        RhymeDifficulty.Extreme -> 20
    }

    private var strokeTextBuilder: ((String) -> StrokeTextGraph)? = null

    // 得分
    private var score: Int = 0
    private var scoreGraph: StrokeTextGraph? = null

    // 评级结果
    private var resultData: ResultData? = null
    private var defaultResultGraph = mutableMapOf<BlockResult, StrokeTextGraph>()
    private val comboGraphMap = mutableMapOf<Int, StrokeTextGraph>()

    // 统计数据收集
    private val statistics = IntArray(BlockResult.entries.size) // 各评级数量
    private var maxCombo: Int = 0

    fun updateResult(result: BlockResult) {
        // 统计
        statistics[result.ordinal] += 1
        // 计算连击
        val oldCombo = resultData?.combo ?: 0
        val newCombo = if (result == BlockResult.MISS || result == BlockResult.BAD) 0 else oldCombo + 1
        // 记录最大连击
        if (newCombo > maxCombo) maxCombo = newCombo
        // 计算得分
        val reward = result.score // 向上取整
        val deltaScore = reward + newCombo / comboRewardCount
        score += deltaScore // 连击奖励

        strokeTextBuilder?.let { builder ->
            val resultGraph = defaultResultGraph.getOrPut(result) { builder(result.title) }
            val comboGraph = if (newCombo > 1) comboGraphMap.getOrPut(newCombo) { builder(" +$newCombo") } else null

            resultData = ResultData(result, newCombo, resultGraph, comboGraph)
            if (deltaScore > 0) scoreGraph = builder(score.toString())

            updateDirty()
        }
    }

    fun submitResult(): RhymePlayResult = RhymePlayResult(
        duration = info.lyricsConfig.duration,
        score = score,
        maxCombo = maxCombo,
        statistics = statistics.toList()
    )

    override fun preUpdate(tick: Int) {
        var isDirty = false

        // 更新进度
        val audioPosition = momentLayer.audioPosition
        val audioDuration = momentLayer.audioDuration
        if (audioPosition - lastAudioPosition > 500L) { // 降频
            audioProgress = if (audioDuration == 0L) 0f else audioPosition / audioDuration.toFloat()
            lastAudioPosition = audioPosition
            isDirty = true
        }

        // 更新FPS
        fpsCounter.update(tick) { fps ->
            fpsTextBuilder?.let { builder ->
                if (fpsCache.size >= 16) fpsCache.clear()
                currentFpsGraph = fpsCache.getOrPut(fps) { builder(fps) }
                isDirty = true
            }
        }

        // 更新评级结果
        if (resultData?.update(tick) == true) isDirty = true

        if (isDirty) updateDirty()
    }

    override fun InitialDrawer.preInitialDraw() {
        title = measureText(info.musicInfo.name, GlobalRes.font.xwwk, FontWeight.Bold)
        fpsTextBuilder = { measureText("FPS: $it", RhymeRes.font.rhyme, FontWeight.Bold) }
        strokeTextBuilder = { measureStrokeText(it, RhymeRes.font.rhyme, FontWeight.Bold) }
        scoreGraph = measureStrokeText("0", RhymeRes.font.rhyme, FontWeight.Bold)
    }

    override fun PrepareDrawer.prePrepareDraw(viewportSize: Size, viewportBounds: Rect) {
        viewportWidth = viewportSize.width
        viewportHeight = viewportSize.height
    }

    override fun Drawer.preOnDraw() {
        val minDimension = minOf(viewportWidth, viewportHeight)

        val barHeight = minDimension / 50
        val barRadius = barHeight / 2
        val barPosition = Offset(0f, -barRadius)
        // 画时长
        roundRect(Colors.Ghost, barRadius, barPosition, Size(viewportWidth, barHeight))
        // 画进度
        roundRect(Colors.Green6, barRadius, barPosition, Size(viewportWidth * audioProgress, barHeight))

        val textHeight = minDimension / 24
        title?.let { graph ->
            val textWidth = graph.width(textHeight)
            // 画封面
            val coverRect = Rect(Offset(barRadius, barHeight), Size(textHeight, textHeight))
            circle(Colors.Ghost, coverRect.center, textHeight / 2, style = Stroke(textHeight / 10))
            clipCircle(coverRect) { image(info.musicRecord, coverRect) }

            // 画歌名
            text(graph, Offset(textHeight * 1.5f + barRadius, barHeight), Size(textWidth, textHeight), Colors.Ghost)
            // 画难度
            repeat(difficulty.ordinal + 1) { index ->
                transform({
                    translate(textWidth + (index + 2) * textHeight, barHeight)
                    scale(textHeight / 1024f, Offset.Zero)
                }) {
                    StarPaths.fastForEach { (path, color) -> path(color, path) }
                }
            }
        }

        // 画FPS
        val barBottom = Offset(0f, barRadius)
        currentFpsGraph?.let { graph ->
            text(graph, barBottom, Size(viewportWidth, textHeight * 0.75f), fpsColor, TextAlign.Center)
        }

        val resultHeight = minDimension / 16
        resultData?.let { data ->
            val resultGraph = data.resultGraph
            val resultWidth = resultGraph.width(resultHeight)
            val isOpen = data.isOpen
            val alpha = if (isOpen) 1f else data.progress
            val mainColor = data.result.color.copy(alpha = alpha)
            val strokeColor = Colors.White.copy(alpha = alpha)

            val comboGraph = data.comboGraph
            val comboWidth = comboGraph?.width(resultHeight) ?: 0f
            val totalWidth = resultWidth + comboWidth

            transform({
                translate((viewportWidth - totalWidth) / 2, barRadius + textHeight)
                if (isOpen) scale(data.progress, Offset(totalWidth / 2, resultHeight / 2))
            }) {
                // 画评级
                strokeText(resultGraph, Offset.Zero, Size(resultWidth, resultHeight), mainColor, strokeColor, TextStroke)
                // 画连击
                comboGraph?.let { graph ->
                    strokeText(graph, Offset(resultWidth, 0f), Size(comboWidth, resultHeight), mainColor, strokeColor, TextStroke)
                }
            }
        }

        // 画分数
        scoreGraph?.let { graph ->
            val scoreWidth = viewportWidth - barRadius
            strokeText(graph, barBottom, Size(scoreWidth, resultHeight), Colors.Dark, Colors.White, TextStroke, TextAlign.End)
        }
    }
}