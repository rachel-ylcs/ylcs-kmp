package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
enum class PAGColorType {
    UNKNOWN,
    ALPHA_8,
    RGBA_8888,
    BGRA_8888,
    RGB_565,
    GRAY_8,
    RGBA_F16,
    RGBA_1010102;
}