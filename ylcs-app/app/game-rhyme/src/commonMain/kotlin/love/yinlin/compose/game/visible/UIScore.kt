package love.yinlin.compose.game.visible

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import love.yinlin.app.game_rhyme.resources.Res
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.compose.Colors
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.data.RhymeDifficulty
import love.yinlin.compose.game.data.RhymePlayConfig
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.drawer.TextGraph
import love.yinlin.compose.game.layer.UILayer
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Visible

class UIScore(playConfig: RhymePlayConfig) : Visible(size = DefaultSize), Dynamic {
    private class ResultData(
        val result: BlockResult,
        val combo: Int,
        val resultGraph: TextGraph,
        val comboGraph: TextGraph?
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

    companion object {
        private const val DEFAULT_WIDTH = 400f
        private const val DEFAULT_HEIGHT = 200f
        private const val SCORE_HEIGHT = DEFAULT_HEIGHT * 0.6f
        private const val RESULT_HEIGHT = DEFAULT_HEIGHT - SCORE_HEIGHT
        private val DefaultSize = Size(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        private val ScoreSize = Size(DEFAULT_WIDTH, SCORE_HEIGHT)
        private val ScoreStroke = Stroke(2f)
        private val ResultStroke = Stroke(1f)
    }

    // 连击奖励
    private val comboRewardCount = when (playConfig.difficulty) {
        RhymeDifficulty.Easy -> 30
        RhymeDifficulty.Medium -> 25
        RhymeDifficulty.Hard -> 20
        RhymeDifficulty.Extreme -> 20
    }

    private var viewportWidth: Float = 0f
    private var textBuilder: ((String) -> TextGraph)? = null

    /**
     * 得分
     */
    private var score: Int = 0
    private var scoreGraph: TextGraph? = null

    /**
     * 评级结果
     */
    private var resultData: ResultData? = null
    private var defaultResultGraph = mutableMapOf<BlockResult, TextGraph>()
    private val comboGraphMap = mutableMapOf<Int, TextGraph>()

    /**
     * 统计数据收集
     */
    private val statistics = IntArray(BlockResult.entries.size)

    fun updateResult(result: BlockResult, scoreRatio: Float) {
        // 统计
        statistics[result.ordinal] += 1
        // 计算连击
        val oldCombo = resultData?.combo ?: 0
        val newCombo = if (result == BlockResult.MISS || result == BlockResult.BAD) 0 else oldCombo + 1
        // 计算得分
        val reward = (result.score * scoreRatio).toInt()
        val deltaScore = reward + newCombo / comboRewardCount
        score += deltaScore // 连击奖励

        textBuilder?.let { builder ->
            val resultGraph = defaultResultGraph.getOrPut(result) { builder(result.title) }
            val comboGraph = if (newCombo > 1) comboGraphMap.getOrPut(newCombo) { builder("+$newCombo  ") } else null

            resultData = ResultData(result, newCombo, resultGraph, comboGraph)
            if (deltaScore > 0) scoreGraph = builder(score.toString())

            updateDirty()
        }
    }

    override fun onUpdate(tick: Int) {
        if (resultData?.update(tick) == true) updateDirty()
    }

    override fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) {
        viewportWidth = viewportSize.width
        if (textBuilder == null) textBuilder = { text -> measureText(text, Res.font.rhyme, FontWeight.Bold) }
    }

    override fun Drawer.onDraw() {
        translate(viewportWidth - DEFAULT_WIDTH - UILayer.DEFAULT_PADDING, 0f) {
            scoreGraph?.let { graph ->
                text(graph, Offset.Zero, ScoreSize, Colors.Dark, textAlign = TextAlign.End)
                text(graph, Offset.Zero, ScoreSize, Colors.White, textAlign = TextAlign.End, drawStyle = ScoreStroke)
            }
            resultData?.let { data ->
                val resultGraph = data.resultGraph
                val resultWidth = resultGraph.width(RESULT_HEIGHT)
                val isOpen = data.isOpen
                val alpha = if (isOpen) 1f else data.progress
                val mainColor = data.result.color.copy(alpha = alpha)
                val strokeColor = Colors.White.copy(alpha = alpha)

                val comboGraph = data.comboGraph
                val comboWidth = comboGraph?.width(RESULT_HEIGHT) ?: 0f
                val totalWidth = resultWidth + comboWidth

                val resultOffset = Offset(comboWidth, 0f)
                val resultSize = Size(resultWidth, RESULT_HEIGHT)
                val comboSize = Size(comboWidth, RESULT_HEIGHT)

                transform({
                    translate(DEFAULT_WIDTH - totalWidth, SCORE_HEIGHT)
                    if (isOpen) scale(data.progress, Offset(totalWidth, RESULT_HEIGHT / 2))
                }) {
                    if (comboGraph != null) {
                        text(comboGraph, Offset.Zero, comboSize, mainColor)
                        text(comboGraph, Offset.Zero, comboSize, strokeColor, drawStyle = ResultStroke)
                    }
                    text(resultGraph, resultOffset, resultSize, mainColor)
                    text(resultGraph, resultOffset, resultSize, strokeColor, drawStyle = ResultStroke)
                }
            }
        }
    }
}