package love.yinlin.compose.game

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform

@Stable
abstract class Spirit(private val manager: Manager) {
    abstract val box: AABB
    open val transform : (DrawTransform.() -> Unit)? = null
    abstract fun DrawScope.onDraw(textDrawer: TextDrawer)
}