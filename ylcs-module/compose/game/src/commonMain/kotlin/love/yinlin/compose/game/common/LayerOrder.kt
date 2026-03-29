package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

@Suppress("ConstPropertyName")
@Stable
object LayerOrder {
    const val UI = 10
    const val Default = 0
    const val GameSurface = -10
    const val Low = -20
    const val Invisible = Int.MIN_VALUE
}