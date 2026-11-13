package love.yinlin.compose.ui.input

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastRoundToInt
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.node.pointerIcon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Stable
class StarBarState(initialValue: Float = 0f) {
    internal companion object {
        const val SPACE_PERCENT = 0.15f

        fun ratingAt(x: Float, height: Float, num: Int, half: Boolean): Float {
            if (height <= 0f) return 0f

            val stride = height * (1f + SPACE_PERCENT)
            val index = (x.coerceAtLeast(0f) / stride).toInt().coerceIn(0, num - 1)
            val fraction = ((x - index * stride) / height).coerceIn(0f, 1f)
            return if (half && fraction < 0.5f) index + 0.5f else index + 1f
        }

        fun buildPath(height: Float): Path = Path().apply {
            for (point in 0 ..< 10) {
                val angle = point * PI / 5 - PI / 2
                val halfHeight = height / 2
                val radius = if (point % 2 == 0) halfHeight else height * 0.21f
                val x = halfHeight + (cos(angle) * radius).toFloat()
                val y = halfHeight + (sin(angle) * radius).toFloat()
                if (point == 0) moveTo(x, y)
                else lineTo(x, y)
            }
            close()
        }
    }

    constructor(initialValue: Int) : this(initialValue.toFloat())
    constructor(initialValue: Double) : this(initialValue.toFloat())

    var value: Float by mutableFloatStateOf(initialValue)
        internal set
}

/**
 * 星级评分条。点击当前评分对应的位置可清除评分。
 *
 * @param state 评分状态，value 为 0 到 num 之间的星数
 * @param num 星星数量，必须大于 0
 * @param half 是否允许半星
 * @param enable 是否允许交互
 * @param activeColor 已选星星颜色
 * @param trackColor 未选星星颜色
 */
@Composable
fun StarBar(
    state: StarBarState,
    modifier: Modifier = Modifier,
    num: Int = 5,
    half: Boolean = false,
    enable: Boolean = true,
    activeColor: Color = Theme.color.primary,
    trackColor: Color = Theme.color.backgroundVariant
) {
    require(num > 0) { "StarBar num must be positive" }

    val defaultHeight = Theme.size.input9
    val currentValue = state.value.coerceIn(0f, num.toFloat())
    val actualActiveColor = if (enable) activeColor else Theme.color.disabledContent
    val actualTrackColor = if (enable) trackColor else Theme.color.disabledContainer
    var hoveredValue by rememberValueState(Float.NaN, enable, num, half)

    Layout(
        content = {},
        modifier = modifier
            .height(defaultHeight)
            .pointerIcon(PointerIcon.Hand, enabled = enable)
            .pointerInput(state, enable, num, half) {
                if (enable) {
                    detectTapGestures { offset ->
                        val rating = StarBarState.ratingAt(offset.x, size.height.toFloat(), num, half)
                        state.value = if (rating == state.value) 0f else rating
                    }
                }
            }.pointerInput(enable, num, half) {
                if (enable) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.type == PointerType.Mouse) {
                                when (event.type) {
                                    PointerEventType.Enter, PointerEventType.Move -> hoveredValue = StarBarState.ratingAt(change.position.x, size.height.toFloat(), num, half)
                                    PointerEventType.Exit -> hoveredValue = Float.NaN
                                }
                            }
                        }
                    }
                }
            }.drawWithCache {
                val height = size.height
                val gap = height * StarBarState.SPACE_PERCENT
                val starPath = StarBarState.buildPath(height)

                onDrawBehind {
                    for (index in 0 ..< num) {
                        translate(left = index * (height + gap)) {
                            drawPath(starPath, actualTrackColor)
                            val selected = (currentValue - index).coerceIn(0f, 1f)
                            val actualValue = if (hoveredValue.isNaN()) 0f else hoveredValue
                            val hovered = (actualValue - index).coerceIn(0f, 1f)

                            if (hovered > selected && enable) {
                                clipRect(left = height * selected, right = height * hovered) {
                                    drawPath(starPath, actualActiveColor.copy(alpha = actualActiveColor.alpha * 0.5f))
                                }
                            }

                            if (selected > 0f) {
                                clipRect(right = height * selected) {
                                    drawPath(starPath, actualActiveColor)
                                }
                            }
                        }
                    }
                }
            }
    ) { _, constraints ->
        val height = constraints.constrainHeight(defaultHeight.roundToPx())
        val width = (num * height + (num - 1) * height * StarBarState.SPACE_PERCENT).fastRoundToInt()

        layout(constraints.constrainWidth(width), height) { }
    }
}