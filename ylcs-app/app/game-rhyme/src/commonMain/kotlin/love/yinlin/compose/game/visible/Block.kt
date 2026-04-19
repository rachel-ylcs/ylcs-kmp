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
import love.yinlin.compose.Colors
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.common.InteractStatus
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
        const val DEFAULT_SCALE = 0.9f
        val DefaultSize = Size(DEFAULT_DIMENSION, DEFAULT_DIMENSION)
        val DefaultCenter = DefaultSize.center
        val DefaultRect = Rect(Offset.Zero, DefaultSize)
        val TopLeft = Offset.Zero
        val TopRight = Offset(DEFAULT_DIMENSION, 0f)
        val BottomLeft = Offset(0f, DEFAULT_DIMENSION)
        val BottomRight = Offset(DEFAULT_DIMENSION, DEFAULT_DIMENSION)

        val PrepareStroke = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val ScaleColorList = arrayOf(Colors.Transparent, Colors.Red5, Colors.Green4, Colors.Blue5, Colors.Orange4, Colors.Purple4, Colors.Yellow4, Colors.Cyan4)

        val BounceBorderStroke = arrayOf(Stroke(22f), Stroke(16f), Stroke(10f), Stroke(6f), Stroke(2f))

        val NoteScaleFontMap = arrayOf(
            '9',
            '1', '2', '3', '4', '5', '6', '7',
            '\u0086', '\u0087', '\u0088', '*', '%', '^', '&',
            '\uF021', '@', '#', '$', '\u00A7', '\u00A8', '\u00A9',
        )
    }

    abstract val rhymeAction: RhymeAction // 音符操作
    abstract val time: BlockTime // 时间信息

    abstract fun prepareStatus(): BS
    abstract fun onInteract(interactStatus: Array<InteractStatus>, currentStatus: BlockStatus.Interact)

    var blockStatus: BS? = null
        protected set

    val fromMapLayer: MapLayer? get() = layer as? MapLayer

    inline fun withMapLayer(block: (MapLayer, Int) -> Boolean) {
        (layer as? MapLayer)?.let { mapLayer ->
            if (block(mapLayer, (mapLayer.audioPosition - time.appearance).toInt())) updateDirty()
        }
    }

    override fun onAttached() {
        blockStatus = prepareStatus()
    }

    override fun onDetached() {
        blockStatus = null
    }

    protected inline fun <BRS : BlockStatus.Release, BDS : BS> updateCustomRelease(status: BRS, tick: Int, done: (BRS) -> BDS) {
        // Release, Missing, Done的动画可以根据游戏刻来而不是音轨刻
        // 以防音符终止了但动画仍需要继续
        val oldTick = status.tick
        if (oldTick >= status.duration) blockStatus = done(status)
        else {
            val newTick = oldTick + tick
            status.tick = newTick
            status.progress = (newTick / status.duration.toFloat()).coerceIn(0f, 1f)
        }
    }

    inline fun Drawer.withBlockScale(block: Drawer.() -> Unit) = scale(DEFAULT_SCALE, DefaultCenter, block)

    protected fun Drawer.drawPrepareBorder(color: Color, progress: Float) {
        val delta = progress * DEFAULT_DIMENSION / 2f
        val deltaInv = DEFAULT_DIMENSION - delta
        line(color, TopLeft, Offset(delta, 0f), style = PrepareStroke)
        line(color, TopLeft, Offset(0f, delta), style = PrepareStroke)
        line(color, TopRight, Offset(deltaInv, 0f), style = PrepareStroke)
        line(color, TopRight, Offset(DEFAULT_DIMENSION, delta), style = PrepareStroke)
        line(color, BottomLeft, Offset(0f, deltaInv), style = PrepareStroke)
        line(color, BottomLeft, Offset(delta, DEFAULT_DIMENSION), style = PrepareStroke)
        line(color, BottomRight, Offset(deltaInv, DEFAULT_DIMENSION), style = PrepareStroke)
        line(color, BottomRight, Offset(DEFAULT_DIMENSION, deltaInv), style = PrepareStroke)
    }

    protected fun Drawer.drawFullPrepareBorder(color: Color, alpha: Float = 1f) {
        rect(color, DefaultRect, alpha = alpha, style = PrepareStroke)
    }

    protected fun Drawer.drawScaleBlock(color: Color, scaleRatio: Float, alpha: Float = 0.4f) {
        scale(scaleRatio, DefaultCenter) { rect(color, DefaultRect, alpha = alpha) }
    }

    protected fun Drawer.drawBounceBorder(color: Color, ratio: Float) {
        scale(ratio, DefaultCenter) {
            rect(color, DefaultRect, style = BounceBorderStroke[0], alpha = 0.2f)
            rect(color, DefaultRect, style = BounceBorderStroke[1], alpha = 0.5f)
            rect(color, DefaultRect, style = BounceBorderStroke[2], alpha = 0.9f)
            rect(Colors.White, DefaultRect, style = BounceBorderStroke[3], alpha = 0.4f)
            rect(Colors.White, DefaultRect, style = BounceBorderStroke[4], alpha = 0.8f)
        }
    }

    protected fun Drawer.drawSingleNoteFont(scale: Int, color: Color, alpha: Float, scaleRatio: Float = 0.75f) {
        fromMapLayer?.baseNoteFontMap?.getOrNull(scale)?.let { graph ->
            scale(scaleRatio, DefaultCenter) {
                text(graph, TopLeft, DefaultSize, color.copy(alpha = alpha), TextAlign.Center)
            }
        }
    }

    protected fun Drawer.drawLyricsText(color: Color, scaleRatio: Float) {
        fromMapLayer?.lyricsTextMap?.get(rhymeAction.ch)?.let { graph ->
            scale(scaleRatio, DefaultCenter) {
                text(graph, TopLeft, DefaultSize, color, TextAlign.Center)
            }
        }
    }
}