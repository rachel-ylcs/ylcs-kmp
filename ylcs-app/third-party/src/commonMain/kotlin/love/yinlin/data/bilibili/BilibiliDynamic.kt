package love.yinlin.data.bilibili

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class BilibiliDynamic(
    val id: String,
    val user: BilibiliUserInfo,
    val time: LocalDateTime,
    val location: String,
    val data: BilibiliData
)