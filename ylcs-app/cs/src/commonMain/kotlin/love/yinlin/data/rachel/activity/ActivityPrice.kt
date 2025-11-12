package love.yinlin.data.rachel.activity

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ActivityPrice(
    val name: String,
    val value: Int
)