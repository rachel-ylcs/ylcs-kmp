package love.yinlin.compose.data.media

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class MediaPlayMode {
    Order, Loop, Random;

    val next: MediaPlayMode get() = when (this) {
        Order -> Loop
        Loop -> Random
        Random -> Order
    }

    companion object {
        val Default = Order
    }
}