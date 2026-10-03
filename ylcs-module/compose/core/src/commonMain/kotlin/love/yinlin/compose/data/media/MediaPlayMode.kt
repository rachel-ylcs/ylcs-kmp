package love.yinlin.compose.data.media

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class MediaPlayMode {
    Order, Loop, Random;

    companion object {
        val Default = Order
    }
}