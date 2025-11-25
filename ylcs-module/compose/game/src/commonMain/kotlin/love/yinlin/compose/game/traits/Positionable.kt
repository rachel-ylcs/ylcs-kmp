package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.min

@Stable
interface Positionable : Soul {
    val size: Size

    val topLeft: Offset get() = Offset.Zero
    val topRight: Offset get() = Offset(size.width, 0f)
    val bottomLeft: Offset get() = Offset(0f, size.height)
    val bottomRight: Offset get() = Offset(size.width, size.height)
    val leftCenter: Offset get() = Offset(0f, size.height / 2)
    val topCenter: Offset get() = Offset(size.width / 2, 0f)
    val bottomCenter: Offset get() = Offset(size.width / 2, size.height)
    val rightCenter: Offset get() = Offset(size.width, size.height / 2)
    val center: Offset get() = Offset(size.width / 2, size.height / 2)
    val radius: Float get() = min(size.width, size.height) / 2
}