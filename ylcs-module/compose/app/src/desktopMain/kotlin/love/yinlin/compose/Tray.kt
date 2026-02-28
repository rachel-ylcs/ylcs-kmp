package love.yinlin.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import love.yinlin.compose.extension.mutableRefStateOf

@Stable
class Tray {
    internal val state = TrayState()

    /**
     * 显示托盘图标
     */
    var visible by mutableStateOf(false)

    /**
     * 托盘图标绘制
     */
    var painter: Painter? by mutableRefStateOf(null)

    /**
     * 托盘双击事件
     */
    var onDoubleClick: (() -> Unit)? by mutableStateOf(null)

    /**
     * 发送通知
     */
    fun sendNotification(title: String, message: String, type: Notification.Type = Notification.Type.Info) {
        val notification = Notification(title, message, type)
        state.sendNotification(notification)
    }
}