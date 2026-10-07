package love.yinlin.compose.screen

import androidx.compose.runtime.Stable

@Stable
internal sealed interface NavigationPlan {
    @Stable
    data class StackChange(val fromIndex: Int, val toIndex: Int)

    val key: ScreenKey
    val change: StackChange?

    @Stable
    data class Create(override val key: ScreenKey, override val change: StackChange) : NavigationPlan
    @Stable
    data class Reuse(override val key: ScreenKey, override val change: StackChange?) : NavigationPlan
    @Stable
    data class Resume(override val key: ScreenKey, override val change: StackChange?, val args: ScreenArgs) : NavigationPlan
}
