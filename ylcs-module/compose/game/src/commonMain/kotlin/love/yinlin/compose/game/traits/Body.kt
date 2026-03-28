package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.common.Transform

@Stable
abstract class Body(
    position: Offset = Offset.Zero,
    size: Size = Size.Zero,
) : Entity() {
    var position: Offset by mutableStateOf(position)
    var size: Size by mutableStateOf(size)
    var transform: Transform? by mutableStateOf(null)
    var clip: Boolean by mutableStateOf(true)
    var shape: Shape by mutableStateOf(Shape.Box)
}