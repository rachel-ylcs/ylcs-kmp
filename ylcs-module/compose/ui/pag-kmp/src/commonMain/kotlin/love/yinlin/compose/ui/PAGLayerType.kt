package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
enum class PAGLayerType {
    Unknown,
    Null,
    Solid,
    Text,
    Shape,
    Image,
    PreCompose,
    Camera;
}