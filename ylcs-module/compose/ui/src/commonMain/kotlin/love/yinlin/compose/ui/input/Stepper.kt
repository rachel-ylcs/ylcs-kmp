package love.yinlin.compose.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.text.SimpleEllipsisText

/**
 * 步进器状态。
 *
 * @param initialValue 初始整数值
 */
@Stable
class StepperState(initialValue: Int = 0) {
    var value: Int by mutableIntStateOf(initialValue)
        private set

    /**
     * 在指定范围内更新当前值。
     *
     * @param value 目标值
     * @param min 最小值
     * @param max 最大值
     */
    fun setValue(value: Int, min: Int, max: Int) {
        require(min <= max) { "Stepper min must not exceed max" }
        this.value = value.coerceIn(min, max)
    }
}

/**
 * 横向整数步进器，通过左右按钮递减或递增数值。
 *
 * @param state 当前数值状态
 * @param enabled 是否允许调整
 * @param faster 是否允许长按连续调整
 * @param min 最小值，达到后递减按钮禁用
 * @param max 最大值，达到后递增按钮禁用
 * @param padding 按钮与中间数值之间的间距
 * @param label 可选的次级标签，显示在数值下方
 * @param converter 数值显示转换器
 */
@Composable
fun Stepper(
    state: StepperState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    faster: Boolean = false,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    padding: Dp = Theme.padding.h,
    label: String? = null,
    converter: (Int) -> String = { it.toString() },
) {
    require(min <= max) { "Stepper min must not exceed max" }

    val value = state.value.coerceIn(min, max)

    Row(
        modifier = modifier
            .clip(Theme.shape.v7)
            .background(Theme.color.onSurface.copy(alpha = 0.06f))
            .padding(Theme.padding.value),
        horizontalArrangement = Arrangement.spacedBy(padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton(
            icon = Icons.Remove,
            description = "减少",
            enabled = enabled && value > min,
            faster = faster,
            onStep = { state.setValue(value - 1, min, max) }
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SimpleEllipsisText(text = converter(value), style = Theme.typography.v7.bold, color = Theme.color.onSurface)
            if (label != null) {
                SimpleEllipsisText(text = label, style = Theme.typography.v9, color = Theme.color.onSurfaceVariant)
            }
        }

        StepperButton(
            icon = Icons.Add,
            description = "增加",
            enabled = enabled && value < max,
            faster = faster,
            onStep = { state.setValue(value + 1, min, max) }
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    faster: Boolean,
    onStep: () -> Unit
) {
    val currentOnStep by rememberUpdatedState(onStep)

    Box(
        modifier = Modifier.size(width = Theme.size.icon, height = Theme.size.input9)
            .clip(Theme.shape.v7)
            .pointerIcon(PointerIcon.Hand, enabled)
            .semantics(role = Role.Button, description = description)
            .pointerInput(enabled, faster) {
                if (!enabled) return@pointerInput

                if (faster) {
                    detectTapGestures(onPress = {
                        coroutineScope {
                            var repeated = false
                            val repeatJob = launch {
                                delay(viewConfiguration.longPressTimeoutMillis)
                                while (isActive) {
                                    repeated = true
                                    currentOnStep()
                                    delay(100L)
                                }
                            }
                            try {
                                if (tryAwaitRelease() && !repeated) currentOnStep()
                            }
                            finally {
                                repeatJob.cancel()
                            }
                        }
                    })
                }
                else {
                    detectTapGestures(onTap = { currentOnStep() })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon = icon, color = if (enabled) Theme.color.primary else Theme.color.disabledContent)
    }
}
