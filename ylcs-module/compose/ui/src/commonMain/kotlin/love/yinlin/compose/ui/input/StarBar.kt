package love.yinlin.compose.ui.input

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.util.fastRoundToInt
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.node.pointerIcon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val STAR_SPACE_PERCENT = 0.15f

@Stable
class StarBarState(initialValue: Float = 0f) {
    var value: Float by mutableFloatStateOf(initialValue)
}

@Composable
fun rememberStarBarState(initialValue: Float = 0f): StarBarState = remember(initialValue) { StarBarState(initialValue) }

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

    val currentValue = state.value.coerceIn(0f, num.toFloat())
    val actualActiveColor = if (enable) activeColor else Theme.color.disabledContent
    val actualTrackColor = if (enable) trackColor else Theme.color.disabledContainer
    var hoveredValue by remember { mutableStateOf<Float?>(null) }

    Layout(
        content = {},
        modifier = modifier
            .height(Theme.size.input8)
            .pointerIcon(PointerIcon.Hand, enabled = enable)
            .semantics {
                stateDescription = "${currentValue} / ${num}"
                progressBarRangeInfo = ProgressBarRangeInfo(currentValue, 0f .. num.toFloat(), if (half) num * 2 - 1 else num - 1)
                if (!enable) disabled()
                else setProgress { target ->
                    val rounded = (if (half) (target * 2).fastRoundToInt() / 2f else target.fastRoundToInt().toFloat()).coerceIn(0f, num.toFloat())
                    state.value = if (rounded == state.value) 0f else rounded
                    true
                }
            }
            .pointerInput(enable, num, half) {
                if (enable) {
                    detectTapGestures { offset ->
                        val rating = starBarRatingAt(offset.x, size.height.toFloat(), num, half)
                        state.value = if (rating == state.value) 0f else rating
                    }
                }
            }
            .pointerInput(enable, num, half) {
                if (enable) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.type == PointerType.Mouse) {
                                when (event.type) {
                                    PointerEventType.Enter, PointerEventType.Move -> hoveredValue = starBarRatingAt(change.position.x, size.height.toFloat(), num, half)
                                    PointerEventType.Exit -> hoveredValue = null
                                }
                            }
                        }
                    }
                }
            }
            .drawWithCache {
                val height = size.height
                val gap = height * STAR_SPACE_PERCENT
                val starPath = Path().apply {
                    for (point in 0 ..< 10) {
                        val angle = point * PI / 5 - PI / 2
                        val radius = if (point % 2 == 0) height / 2 else height * 0.21f
                        val x = height / 2 + (cos(angle) * radius).toFloat()
                        val y = height / 2 + (sin(angle) * radius).toFloat()
                        if (point == 0) moveTo(x, y)
                        else lineTo(x, y)
                    }
                    close()
                }

                onDrawBehind {
                    for (index in 0 ..< num) {
                        translate(left = index * (height + gap)) {
                            drawPath(starPath, actualTrackColor)
                            val selected = (currentValue - index).coerceIn(0f, 1f)
                            val hovered = ((hoveredValue ?: 0f) - index).coerceIn(0f, 1f)

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
        val height = constraints.constrainHeight(Theme.size.input8.roundToPx())
        val width = (num * height + (num - 1) * height * STAR_SPACE_PERCENT).fastRoundToInt()
        layout(constraints.constrainWidth(width), height) {}
    }
}

private fun starBarRatingAt(x: Float, height: Float, num: Int, half: Boolean): Float {
    if (height <= 0f) return 0f

    val stride = height * (1f + STAR_SPACE_PERCENT)
    val index = (x.coerceAtLeast(0f) / stride).toInt().coerceIn(0, num - 1)
    val fraction = ((x - index * stride) / height).coerceIn(0f, 1f)
    return if (half && fraction < 0.5f) index + 0.5f else index + 1f
}