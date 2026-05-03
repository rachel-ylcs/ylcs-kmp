package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

@Stable
sealed interface InteractTarget {
    @Stable
    data object None : InteractTarget
    @Stable
    data object Multiple : InteractTarget
    @Stable
    data class Single(val id: Long, val index: Int) : InteractTarget
}