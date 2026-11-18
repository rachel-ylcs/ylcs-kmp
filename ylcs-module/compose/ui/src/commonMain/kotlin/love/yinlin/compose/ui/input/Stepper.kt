package love.yinlin.compose.ui.input

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.extension.cleaning
import kotlin.time.Duration.Companion.milliseconds

@Composable
private fun StepperButton(
    state: StepperState,
    icon: ImageVector,
    enabled: Boolean,
    faster: Boolean,
    increase: Boolean
) {
    Box(
        modifier = Modifier
            .pointerIcon(PointerIcon.Hand, enabled)
            .semantics(role = Role.Button)
            .pointerInput(state, enabled, faster, increase) {
                if (!enabled) return@pointerInput

                if (faster) {
                    detectTapGestures(onPress = {
                        coroutineScope {
                            var longPressed = false

                            val repeatJob = launch {
                                delay(viewConfiguration.longPressTimeoutMillis.milliseconds)
                                longPressed = true
                                while (isActive && state.step(increase)) delay(100L.milliseconds)
                            }

                            cleaning({
                                if (tryAwaitRelease() && !longPressed) state.step(increase)
                            }, repeatJob::cancel)
                        }
                    })
                }
                else detectTapGestures(onTap = { state.step(increase) })
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon = icon, color = if (enabled) LocalColor.current else Theme.color.disabledContent)
    }
}


/**
 * 横向整数步进器，通过左右按钮递减或递增数值
 *
 * @param faster 是否允许连续调整
 * @param label 次级标签
 * @param converter 数值显示转换器
 */
@Composable
fun Stepper(
    state: StepperState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    faster: Boolean = false,
    padding: Dp = Theme.padding.h,
    label: String? = null,
    converter: (Int) -> String = { it.toString() },
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val enableDecrease by rememberDerivedState(state) { enabled && state.value > state.min }
        val enableIncrease by rememberDerivedState(state) { enabled && state.value < state.max }

        StepperButton(state = state, icon = Icons.Remove, enabled = enableDecrease, faster = faster, increase = false)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SimpleEllipsisText(text = converter(state.value), style = Theme.typography.v7.bold)

            if (label != null) SimpleEllipsisText(text = label, style = Theme.typography.v9, color = LocalColorVariant.current)
        }

        StepperButton(state = state, icon = Icons.Add, enabled = enableIncrease, faster = faster, increase = true)
    }
}
