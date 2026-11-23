package love.yinlin.compose.ui.media

import androidx.compose.runtime.Stable

@Stable
internal data class WebVideoParameters(
    val source: String? = null,
    val loadVersion: Long = 0L,
    val playing: Boolean = false,
    val commandVersion: Long = 0L,
    val seekPosition: Long = 0L,
    val seekVersion: Long = 0L,
)