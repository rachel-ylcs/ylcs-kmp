package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable

@Stable
enum class ImageOrigin {
    Memory,
    Disk,
    Network,
    Local,
    Resource;
}
