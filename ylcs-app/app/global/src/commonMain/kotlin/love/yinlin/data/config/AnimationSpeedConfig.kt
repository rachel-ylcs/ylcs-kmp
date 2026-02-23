package love.yinlin.data.config

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class AnimationSpeedConfig(val value: Float, val title: String) {
    SLOW(1.5f, "慢"),
    STANDARD(1f, "标准"),
    FAST(0.66666f, "快");
}