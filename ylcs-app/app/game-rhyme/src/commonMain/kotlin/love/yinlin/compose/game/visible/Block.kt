package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import love.yinlin.compose.Colors
import love.yinlin.compose.game.common.BlockLine
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
        val TopLeft = Offset.Zero
        val TopRight = Offset(DEFAULT_DIMENSION, 0f)
        val BottomLeft = Offset(0f, DEFAULT_DIMENSION)
        val BottomRight = Offset(DEFAULT_DIMENSION, DEFAULT_DIMENSION)

        val PrepareColor = Colors.White
        val DefaultPrepareStroke = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val InteractColor = Colors(0xFFA29BFE)

        val MissColors = listOf(Colors(0xFFFF0844), Colors(0xFFFFB199))
        val BadColors = listOf(Colors(0xFF6BBBFF), Colors(0xFFB8DCFF))
        val GoodColors = listOf(Colors(0xFF43E97B), Colors(0xFF38F9D7))
        val PerfectColors = listOf(Colors(0xFFF6D365), Colors(0xFFFDA085))
    }

    abstract val rhymeAction: RhymeAction

    protected var status: BlockStatus = BlockStatus.None

    inline fun withMapLayer(block: (MapLayer, Int) -> Boolean) {
        (layer as? MapLayer)?.let { mapLayer ->
            if (block(mapLayer, (mapLayer.audioPosition - time.rawAppearance).toInt())) updateDirty()
        }
    }

    override fun onAttached() {
        status = BlockStatus.Prepare(0)
    }

    override fun onDetached() {
        status = BlockStatus.None
    }

    inline fun Drawer.withBlockScale(block: Drawer.() -> Unit) = scale(DEFAULT_SCALE, DefaultCenter, block)

    protected fun Drawer.drawCommonPrepare(progress: Float) {
        val delta = progress * DEFAULT_DIMENSION / 2f
        val deltaInv = DEFAULT_DIMENSION - delta
        line(PrepareColor, TopLeft, Offset(delta, 0f), DefaultPrepareStroke)
        line(PrepareColor, TopLeft, Offset(0f, delta), DefaultPrepareStroke)
        line(PrepareColor, TopRight, Offset(deltaInv, 0f), DefaultPrepareStroke)
        line(PrepareColor, TopRight, Offset(DEFAULT_DIMENSION, delta), DefaultPrepareStroke)
        line(PrepareColor, BottomLeft, Offset(0f, deltaInv), DefaultPrepareStroke)
        line(PrepareColor, BottomLeft, Offset(delta, DEFAULT_DIMENSION), DefaultPrepareStroke)
        line(PrepareColor, BottomRight, Offset(deltaInv, DEFAULT_DIMENSION), DefaultPrepareStroke)
        line(PrepareColor, BottomRight, Offset(DEFAULT_DIMENSION, deltaInv), DefaultPrepareStroke)
    }
}