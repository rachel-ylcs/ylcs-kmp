package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class WeiboAlbum(
	val containerId: String, // ID
	val title: String, // 标题
	val num: String, // 照片数
	val time: String, // 更新时间
	val pic: String, // 封面图
)