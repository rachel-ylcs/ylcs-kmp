package love.yinlin.compose.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.FilterQuality
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class ImageQuality {
    Low, Medium, High, Full;

    val value: Int get() = when (this) {
        Low -> 50
        Medium -> 75
        High -> 85
        Full -> 100
    }

    val filterQuality: FilterQuality get() = when (this) {
        Low -> FilterQuality.Low
        Medium -> FilterQuality.Medium
        High, Full -> FilterQuality.High
    }
}