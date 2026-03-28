package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center

@Stable
abstract class Body(
    position: Offset = Offset.Zero,
    size: Size = Size.Zero,
) : Entity() {
    var position: Offset by mutableStateOf(position)
    var size: Size by mutableStateOf(size)
    var scale: Float by mutableFloatStateOf(1f)
    var rotate: Float by mutableFloatStateOf(0f)
    var clip: Boolean by mutableStateOf(true)
    var shape: Shape by mutableStateOf(Shape.Box)

    val center: Offset get() = size.center
}