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
import love.yinlin.compose.Colors
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.layer.MapLayer
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Visible
import love.yinlin.data.music.RhymeAction

@Stable
sealed class Block(
    position: Offset,
    val line: BlockLine, // 行信息
    val time: BlockTime, // 时间信息
    val rawIndex: Int, // 在整个序列中的索引
    val lineIndex: Int, // 在segment中的索引
) : Visible(position, DefaultSize), Dynamic {
    companion object {
        const val DEFAULT_DIMENSION = 200f
        const val DEFAULT_SCALE = 0.9f
        val DefaultSize = Size(DEFAULT_DIMENSION, DEFAULT_DIMENSION)
        val DefaultCenter = DefaultSize.center
        val DefaultRect = Rect(Offset.Zero, DefaultSize)
        val TopLeft = Offset.Zero
        val TopRight = Offset(DEFAULT_DIMENSION, 0f)
        val BottomLeft = Offset(0f, DEFAULT_DIMENSION)
        val BottomRight = Offset(DEFAULT_DIMENSION, DEFAULT_DIMENSION)

        val PrepareStroke = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val ScaleColorList = arrayOf(Colors.Transparent, Colors.Red4, Colors.Orange4, Colors.Yellow4, Colors.Green4, Colors.Cyan4, Colors.Blue4, Colors.Purple4)

        const val BLOCK_RESULT_SCALE = 0.35f
        const val RELEASE_ANIMATION_DURATION = 300
        val ReleaseStroke = Stroke(width = 20f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }

    abstract val rhymeAction: RhymeAction

    protected var blockStatus: BlockStatus = BlockStatus.None

    inline fun withMapLayer(block: (MapLayer, Int) -> Boolean) {
        (layer as? MapLayer)?.let { mapLayer ->
            if (block(mapLayer, (mapLayer.audioPosition - time.rawAppearance).toInt())) updateDirty()
        }
    }

    override fun onAttached() {
        blockStatus = BlockStatus.Prepare(0f)
    }

    override fun onDetached() {
        blockStatus = BlockStatus.None
    }

    inline fun Drawer.withBlockScale(block: Drawer.() -> Unit) = scale(DEFAULT_SCALE, DefaultCenter, block)

    protected fun Drawer.drawCommonPrepare(color: Color, progress: Float) {
        val delta = progress * DEFAULT_DIMENSION / 2f
        val deltaInv = DEFAULT_DIMENSION - delta
        line(color, TopLeft, Offset(delta, 0f), PrepareStroke)
        line(color, TopLeft, Offset(0f, delta), PrepareStroke)
        line(color, TopRight, Offset(deltaInv, 0f), PrepareStroke)
        line(color, TopRight, Offset(DEFAULT_DIMENSION, delta), PrepareStroke)
        line(color, BottomLeft, Offset(0f, deltaInv), PrepareStroke)
        line(color, BottomLeft, Offset(delta, DEFAULT_DIMENSION), PrepareStroke)
        line(color, BottomRight, Offset(deltaInv, DEFAULT_DIMENSION), PrepareStroke)
        line(color, BottomRight, Offset(DEFAULT_DIMENSION, deltaInv), PrepareStroke)
    }

    protected fun Drawer.drawBlockResultMiss(color: Color) {
        line(color, TopLeft, BottomRight, ReleaseStroke)
        line(color, TopRight, BottomLeft, ReleaseStroke)
    }

    protected fun Drawer.drawBlockResultBad(color: Color) {

    }

    protected fun Drawer.drawBlockResultGood(color: Color) {

    }

    protected fun Drawer.drawBlockResultPerfect(color: Color) {

    }

    protected fun Drawer.drawBlockResult(color: Color, result: BlockResult) {
        when (result) {
            BlockResult.MISS -> drawBlockResultMiss(color)
            BlockResult.BAD -> drawBlockResultBad(color)
            BlockResult.GOOD -> drawBlockResultGood(color)
            BlockResult.PERFECT -> drawBlockResultPerfect(color)
        }
    }
}