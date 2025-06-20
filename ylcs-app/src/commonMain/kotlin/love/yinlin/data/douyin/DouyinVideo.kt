package love.yinlin.data.douyin

import androidx.compose.runtime.Stable

@Stable
data class DouyinVideo(
    val id: String, // ID
    val title: String, // 标题
    val createTime: String, // 创建时间
    val picUrl: String, // 封面链接
    val videoUrl: List<String>, // 视频链接
    val likeNum: Int, // [点赞数]
    val commentNum: Int, // [评论数]
    val collectNum: Int, // [收藏数]
    val shareNum: Int, // [分享数]
    val isTop: Boolean, // [是否置顶]
)