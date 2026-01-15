package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
sealed interface PAGSource {
    @Stable
    data class File(val path: String) : PAGSource
    @Stable
    class Data(val data: ByteArray) : PAGSource
    @Stable
    data class Asset(val path: String) : PAGSource
}