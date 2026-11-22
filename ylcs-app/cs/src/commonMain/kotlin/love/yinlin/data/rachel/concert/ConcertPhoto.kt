package love.yinlin.data.rachel.concert

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ConcertPhoto(
    val name: String,
    val title: String,
    val picNum: Int
)