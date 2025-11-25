package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.min

@Stable
interface Positionable : Soul {
    val size: Size

    val radius: Float get() = min(size.width, size.height) / 2
    val center: Offset get() = Offset(size.width / 2, size.height / 2)
}