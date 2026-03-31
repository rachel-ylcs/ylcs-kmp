package love.yinlin.compose.game.common

import androidx.compose.ui.geometry.Rect

/**
 * 视口剔除数据
 */
internal class Culling {
    internal var bounds: Rect = Rect.Zero
    internal var dirty: Boolean = false
    internal var enabled: Boolean = false
}