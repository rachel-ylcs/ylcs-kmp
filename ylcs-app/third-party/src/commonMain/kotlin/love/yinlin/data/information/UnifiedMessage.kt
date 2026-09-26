package love.yinlin.data.information

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime

/**
 * 统一资讯接口
 */
@Stable
interface UnifiedMessage : Comparable<UnifiedMessage> {
    val id: String // 唯一ID
    val user: UnifiedUserInfo // 用户信息
    val time: LocalDateTime // 时间
    val location: String // 定位
    val title: String // 标题
    val content: String // 内容
    val data: UnifiedData? // 数据
    val pictures: List<UnifiedPicture> // 图片集
}