package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.Drawer

@Stable
interface Visible : Soul {
    val zIndex: Int
    fun Drawer.onDraw()
}