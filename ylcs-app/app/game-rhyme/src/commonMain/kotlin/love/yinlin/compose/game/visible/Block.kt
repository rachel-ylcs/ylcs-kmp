package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEachIndexed
import love.yinlin.compose.Colors
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.common.InteractStatus
import love.yinlin.compose.game.data.RhymeDifficulty
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.layer.MapLayer
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Visible
import love.yinlin.data.music.RhymeAction

@Stable
sealed class Block<BS : BlockStatus>(
    position: Offset,
    val line: BlockLine, // 行信息
    val rawIndex: Int, // 在整个序列中的索引
    val lineIndex: Int, // 在segment中的索引
) : Visible(position, DefaultSize), Dynamic {
    companion object {
        const val DEFAULT_DIMENSION = 200f
        const val DEFAULT_RADIUS = DEFAULT_DIMENSION / 2
        val DefaultSize = Size(DEFAULT_DIMENSION, DEFAULT_DIMENSION)
        protected const val DEFAULT_SCALE = 0.9f
        protected val DefaultRect = Rect(Offset.Zero, DefaultSize)
        protected val TopLeft = Offset.Zero
        protected val TopCenter = Offset(DEFAULT_RADIUS, 0f)
        protected val TopRight = Offset(DEFAULT_DIMENSION, 0f)
        protected val CenterLeft = Offset(0f, DEFAULT_RADIUS)
        protected val DefaultCenter = Offset(DEFAULT_RADIUS, DEFAULT_RADIUS)
        protected val CenterRight = Offset(DEFAULT_DIMENSION, DEFAULT_RADIUS)
        protected val BottomLeft = Offset(0f, DEFAULT_DIMENSION)
        protected val BottomCenter = Offset(DEFAULT_RADIUS, DEFAULT_DIMENSION)
        protected val BottomRight = Offset(DEFAULT_DIMENSION, DEFAULT_DIMENSION)

        protected val PrepareStroke = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        protected val BounceBorderStroke = arrayOf(Stroke(22f), Stroke(16f), Stroke(10f), Stroke(6f), Stroke(2f))

        protected const val LYRICS_TEXT_SCALE = 0.5f

        protected const val PRESS_TOLERANCE = 200

        val ScaleColorList = arrayOf(Colors.Transparent, Colors.Red5, Colors.Green4, Colors.Blue5, Colors.Orange4, Colors.Purple4, Colors.Yellow4, Colors.Cyan4)
        protected val TextColor = Colors.Ghost
        protected val MissingColor = Colors.Gray6

        protected val PrepareDurationMap = mapOf(
            RhymeDifficulty.Easy to 2500,
            RhymeDifficulty.Medium to 2000,
            RhymeDifficulty.Hard to 1500,
            RhymeDifficulty.Extreme to 1000
        )

        val NoteScaleFontMap = arrayOf(
            '9',
            '1', '2', '3', '4', '5', '6', '7',
            '\uF021', '@', '#', '$', '\u00A7', '\u00A8', '\u00A9',
            '\u0086', '\u0087', '\u0088', '*', '%', '^', '&',
        )
    }

    abstract val rhymeAction: RhymeAction // 音符操作
    abstract val time: BlockTime // 时间信息
    abstract val colorList: List<Color> // 主要颜色列表

    abstract fun prepareStatus(): BS
    abstract fun onInteract(interactStatusList: List<InteractStatus?>, currentStatus: BlockStatus.Interact)

    var blockStatus: BS? = null
        protected set

    val fromMapLayer: MapLayer? get() = layer as? MapLayer

    inline fun withMapLayer(block: (MapLayer, Int) -> Boolean) {
        (layer as? MapLayer)?.let { mapLayer ->
            if (block(mapLayer, (mapLayer.momentLayer.audioPosition - time.appearance).toInt())) updateDirty()
        }
    }

    override val layerOrder: Int = 1

    override fun onAttached() {
        blockStatus = prepareStatus()
    }

    override fun onDetached() {
        blockStatus = null
    }

    protected inline fun updateCustomPrepare(status: BlockStatus.Prepare, audioTick: Int, start: Int, interact: () -> BS) {
        if (audioTick >= start) blockStatus = interact()
        else status.progress = (audioTick / start.toFloat()).fastCoerceIn(0f, 1f)
    }

    protected inline fun <BRS : BlockStatus.Release, BDS : BS> updateCustomRelease(status: BRS, tick: Int, done: (BRS) -> BDS) {
        // Release, Missing, Done的动画可以根据游戏刻来而不是音轨刻
        // 以防音符终止了但动画仍需要继续
        val oldTick = status.tick
        if (oldTick >= status.duration) blockStatus = done(status)
        else {
            val newTick = oldTick + tick
            status.tick = newTick
            status.progress = (newTick / status.duration.toFloat()).fastCoerceIn(0f, 1f)
        }
    }

    protected inline fun Drawer.withBlockScale(block: Drawer.() -> Unit) = scale(DEFAULT_SCALE, DefaultCenter, block)

    // 画单字音符
    protected fun Drawer.drawSingleNoteFont(scale: Int, color: Color, alpha: Float) {
        fromMapLayer?.baseNoteFontMap?.getOrNull(scale)?.let { graph ->
            val r = DEFAULT_DIMENSION
            val w = r / 3
            val h = graph.height(w)
            val s = Size(w, h)
            val p = Offset((r - w) / 2, (r - h) / 2)
            text(graph, p, s, color.copy(alpha = alpha))
        }
    }

    // 画多字音符
    protected fun Drawer.drawMultipleNoteFont(scaleList: List<Int>, color: Color, alpha: Float) {
        fromMapLayer?.baseNoteFontMap?.let { map ->
            val n = scaleList.size
            val r = DEFAULT_DIMENSION
            val w = r / 2
            val pw = w / n
            val x = (r - w) / 2
            scaleList.fastForEachIndexed { i, scale ->
                val graph = map[scale]
                val ph = graph.height(pw)
                val s = Size(pw, ph)
                val y = (r - ph) / 2
                val p = Offset(x + i * pw, y)
                text(graph, p, s, color.copy(alpha = alpha))
            }
        }
    }

    // 画单字歌词
    protected fun Drawer.drawLyricsText(color: Color, scaleRatio: Float) {
        fromMapLayer?.lyricsTextMap?.get(rhymeAction.ch)?.let { graph ->
            scale(scaleRatio, DefaultCenter) {
                text(graph, TopLeft, DefaultSize, color, TextAlign.Center)
            }
        }
    }
}