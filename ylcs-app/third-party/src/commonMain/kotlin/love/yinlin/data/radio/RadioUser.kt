package love.yinlin.data.radio

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class RadioUser(
    val id: String, // ID
    val user: RadioUserInfo, // 用户信息
    val name: String, // 名称
    val cover: String, // 封面图片
    val title: String, // 标题
    val description: String, // 简介
    val subscriberNum: Int, // 订阅数
    val repostNum: Int, // 分享数
    val programNum: Int, // 节目数
    val commentNum: Int, // 评论数
    val updateTime: LocalDateTime, // 更新时间
)