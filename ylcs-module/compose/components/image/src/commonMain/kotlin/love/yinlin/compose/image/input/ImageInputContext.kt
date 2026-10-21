package love.yinlin.compose.image.input

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageInputData
import love.yinlin.compose.image.data.ImageProgress
import love.yinlin.foundation.PlatformContext

@Stable
class ImageInputContext internal constructor(
    val context: PlatformContext,
    val maxBytes: Int,
    val onProgress: (ImageProgress) -> Unit,
    internal val network: suspend (ImageSource.Network) -> ImageInputData,
)
