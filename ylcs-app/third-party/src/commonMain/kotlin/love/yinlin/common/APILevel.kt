package love.yinlin.common

import androidx.compose.runtime.Stable

@Stable
enum class APILevel(val value: Float) {
    Unsupported(0f), // 不支持
    Alpha(0.25f), // Alpha
    Beta(0.5f), // Beta
    RC(0.75f), // RC
    Stable(1f), // 稳定
}