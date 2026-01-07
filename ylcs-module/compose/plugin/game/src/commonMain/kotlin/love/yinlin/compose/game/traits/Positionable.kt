package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size

@Stable
interface Positionable : Soul {
    val size: Size
}