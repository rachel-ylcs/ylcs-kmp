package love.yinlin.compose.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.Theme
import love.yinlin.compose.interaction.InteractionState

/**
 * 颜色状态提供器
 *
 * 可以为输入框提供提示灯的颜色
 * 除了悬浮、获得焦点等鼠标指针事件外，也可以自定义例如输入超长、出现错误等特定情况
 */
@Stable
interface InputStatusColorProvider {
    /**
     * 监听焦点事件
     */
    val useFocused: Boolean

    /**
     * 监听悬浮事件
     */
    val useHovered: Boolean

    /**
     * 监听按下事件
     */
    val usePressed: Boolean

    /**
     * 监听拖拽事件
     */
    val useDragged: Boolean

    @Composable
    fun color(state: InputState, interactionState: InteractionState): Color

    @Stable
    object Default : InputStatusColorProvider {
        override val useFocused: Boolean = true
        override val useHovered: Boolean = true
        override val usePressed: Boolean = false
        override val useDragged: Boolean = false

        @Composable
        override fun color(state: InputState, interactionState: InteractionState): Color {
            return when {
                state.isFull -> Theme.color.warning
                interactionState.isFocused -> Theme.color.primary
                interactionState.isHovered -> Theme.color.primaryContainer.copy(alpha = 0.4f)
                else -> Color.Transparent
            }
        }
    }
}