package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import kotlin.jvm.JvmInline

@Stable
@JvmInline
value class LayerOrder(val value: Int) : Comparable<LayerOrder> {
    val zIndex: Float get() = value.toFloat()

    override fun compareTo(other: LayerOrder): Int = this.value.compareTo(other.value)

    companion object {
        val UI = LayerOrder(10)
        val Default = LayerOrder(0)
        val GameSurface = LayerOrder(-10)
        val Invisible = LayerOrder(-20)
    }
}