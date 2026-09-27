package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class WeiboAlbumPics(
    val items: List<WeiboMedia.Image>,
    val count: Int
)