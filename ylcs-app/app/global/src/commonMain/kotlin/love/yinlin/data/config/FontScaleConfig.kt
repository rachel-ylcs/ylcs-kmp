package love.yinlin.data.config

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class FontScaleConfig(val value: Float, val title: String) {
    LARGE(1.2f, "大"),
    STANDARD(1f, "标准"),
    SMALL(0.83333f, "小");
}