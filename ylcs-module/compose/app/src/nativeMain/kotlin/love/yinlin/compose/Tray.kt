package love.yinlin.compose

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import love.yinlin.compose.extension.mutableRefStateOf
import org.jetbrains.skiko.window.NativeTray
import org.jetbrains.skiko.window.NativeWindowScope

/**
 * 跟随窗口生命周期创建、更新及释放；不持有已关闭的窗口
 */
@Stable
class Tray {
    var visible by mutableStateOf(false)
    var painter: Painter? by mutableRefStateOf(null)
    private var trayTooltip: String? by mutableStateOf(null)
    private var nativeTray by mutableRefStateOf<NativeTray?>(null)
    private var defaultPainter: Painter? by mutableRefStateOf(null)
    private var direction by mutableStateOf(LayoutDirection.Ltr)
    private var scope: CoroutineScope? = null

    /** null 使用窗口标题；平台不支持提示文字时由系统忽略。 */
    var tooltip: String?
        get() = trayTooltip
        set(value) {
            require(value == null || value.length <= 1024 && '\u0000' !in value) { "Invalid tray tooltip" }
            trayTooltip = value
        }

    val isActive: Boolean get() = nativeTray?.isActive == true

    /** 最近一次创建托盘是否成功，null 表示尚未尝试。桌面没有托盘服务时为 false。 */
    var isAvailable: Boolean? by mutableStateOf(null)
        private set

    @Composable
    internal fun Bind(defaultPainter: Painter) {
        val direction = LocalLayoutDirection.current
        SideEffect {
            this.defaultPainter = defaultPainter
            this.direction = direction
        }
    }

    internal fun attach(window: NativeWindowScope, title: () -> String) {
        check(scope == null) { "Tray is already attached" }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        this.scope = scope

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            var previous: TrayConfiguration? = null
            snapshotFlow {
                val painter = painter ?: defaultPainter
                TrayConfiguration(visible, tooltip ?: title(), painter, painter?.intrinsicSize, direction)
            }.collect { configuration ->
                if (!configuration.visible) {
                    nativeTray?.close()
                    nativeTray = null
                    previous = null
                }
                else {
                    val tray = nativeTray ?: window.createTray(configuration.tooltip).also {
                        nativeTray = it
                        isAvailable = it != null
                    }
                    if (tray != null) {
                        tray.setTooltip(configuration.tooltip)
                        val painter = configuration.painter
                        if (painter != null && (previous?.painter !== painter || previous?.size != configuration.size || previous?.direction != configuration.direction)) {
                            tray.setIcon(64, 64, painter.toWindowIconPixels(Density(1f), configuration.direction, edge = 64))
                        }
                        previous = configuration
                    }
                }
            }
        }
    }

    internal fun detach() {
        scope?.cancel()
        scope = null
        nativeTray?.close()
        nativeTray = null
        defaultPainter = null
    }
}

@Stable
private data class TrayConfiguration(val visible: Boolean, val tooltip: String, val painter: Painter?, val size: Size?, val direction: LayoutDirection)
