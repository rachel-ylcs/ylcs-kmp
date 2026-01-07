package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable

@Stable
interface PreTransform : Soul {
    val preTransform: List<Transform> get() = emptyList()
}