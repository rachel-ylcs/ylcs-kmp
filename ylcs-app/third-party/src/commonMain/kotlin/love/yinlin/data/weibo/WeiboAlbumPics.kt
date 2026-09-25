package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedPicture

@Stable
@Serializable
data class WeiboAlbumPics(
    val items: List<UnifiedPicture>,
    val count: Int
)