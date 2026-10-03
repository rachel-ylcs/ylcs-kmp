package love.yinlin.common

import androidx.compose.runtime.Stable

@Stable
enum class APILevel(val value: Float) {
    UNUSED(0f), // 不支持
    ALPHA(0.25f), // Alpha
    BETA(0.5f), // Beta
    RC(0.75f), // RC
    STABLE(1f), // 稳定
}