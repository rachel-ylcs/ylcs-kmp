package love.yinlin.data.bilibili

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class BilibiliLiveRoom(
    val id: String,
    val url: String, // 直播间链接
    val title: String,
    val cover: String, // 封面链接
    val live: Boolean, // 正在直播
)