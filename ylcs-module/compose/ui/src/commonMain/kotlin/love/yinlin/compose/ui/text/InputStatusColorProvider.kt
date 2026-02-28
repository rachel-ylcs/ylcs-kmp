package love.yinlin.compose.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.Theme

/**
 * 颜色状态提供器
 *
 * 可以为输入框提供提示灯的颜色
 * 除了悬浮、获得焦点等鼠标指针事件外，也可以自定义例如输入超长、出现错误等特定情况
 */
@Stable
interface InputStatusColorProvider {
    @Composable
    fun color(state: InputState, isFocused: Boolean, isHovered: Boolean, isPressed: Boolean, isDragged: Boolean): Color

    companion object {
        val Default = object : InputStatusColorProvider {
            @Composable
            override fun color(state: InputState, isFocused: Boolean, isHovered: Boolean, isPressed: Boolean, isDragged: Boolean): Color {
                return when {
                    state.isFull -> Theme.color.warning
                    isFocused -> Theme.color.primary
                    isHovered -> Theme.color.primaryContainer.copy(alpha = 0.4f)
                    else -> Color.Transparent
                }
            }
        }
    }
}