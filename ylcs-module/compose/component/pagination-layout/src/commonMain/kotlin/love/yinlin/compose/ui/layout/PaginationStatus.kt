package love.yinlin.compose.ui.layout

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
internal enum class PaginationStatus {
    IDLE, RUNNING, PULL, RELEASE
}