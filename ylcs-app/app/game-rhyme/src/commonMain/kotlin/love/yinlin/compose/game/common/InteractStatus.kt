package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

@Stable
sealed interface InteractStatus {
    val id: Long

    @Stable
    data class Down(override val id: Long) : InteractStatus

    @Stable
    data class AwaitUp(override val id: Long) : InteractStatus

    @Stable
    data class Up(override val id: Long) : InteractStatus
}