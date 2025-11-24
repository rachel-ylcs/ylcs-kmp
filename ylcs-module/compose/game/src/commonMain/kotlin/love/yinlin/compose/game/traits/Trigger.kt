package love.yinlin.compose.game.traits

import love.yinlin.compose.game.Pointer

interface Trigger {
    fun onEvent(pointer: Pointer)
}