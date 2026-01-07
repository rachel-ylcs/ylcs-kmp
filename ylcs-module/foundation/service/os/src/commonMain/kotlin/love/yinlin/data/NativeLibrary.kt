package love.yinlin.data

import androidx.compose.runtime.Stable
import love.yinlin.platform.Platform

@Stable
class NativeLibrary(
    val name: String,
    vararg val platform: Platform
)