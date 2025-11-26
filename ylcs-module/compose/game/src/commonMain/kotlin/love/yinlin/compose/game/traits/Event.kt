package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.Pointer

@Stable
sealed interface Event

@Stable
data class PointerEvent(val pointer: Pointer) : Event