package love.yinlin.compose.game

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Stable
data class Pointer(
    val id: Long,
    val position: Offset, // 按下位置
    val startTime: Long, // 按下时间
    val endTime: Long? = null, // 抬起时间
) {
    companion object {
        const val LONG_PRESS_TIMEOUT = 500L
    }

    val isDown: Boolean get() = endTime == null
    val isUp: Boolean get() = endTime != null
    val isClick: Boolean get() = endTime?.let { it - startTime < LONG_PRESS_TIMEOUT } ?: false // 是否单击
    val isLongClick: Boolean get() = endTime?.let { it - startTime >= LONG_PRESS_TIMEOUT } ?: false // 是否长按
    inline fun handle(down: () -> Unit, up: (isClick: Boolean, endTime: Long) -> Unit) = endTime?.let { up(it - startTime < LONG_PRESS_TIMEOUT, it) } ?: down()
}