package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
object PAGConfig {
    /** 重复播放 */
    const val INFINITY = -1

    /** 缩放模式 */
    enum class ScaleMode {
        None, Stretch, LetterBox, Zoom
    }
}