package love.yinlin.compose.image.data

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow

internal class ImageFlight<T>(
    val result: Deferred<T>,
    val progress: MutableStateFlow<ImageProgress>,
    var users: Int = 0,
)
