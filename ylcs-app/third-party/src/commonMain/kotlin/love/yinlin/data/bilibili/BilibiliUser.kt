package love.yinlin.data.bilibili

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class BilibiliUser(
    val user: BilibiliUserInfo,
    val sex: String, // 性别
    val birthday: String, // 生日
    val signature: String, // 个性签名
    val level: Int, // 等级
    val official: BilibiliOfficial?, // 官方标志
    val vipTitle: String?, // 会员标题
    val background: String, // 背景图
    val liveRoom: BilibiliLiveRoom?, // 直播间
    val school: String?, // 学校
    val tags: List<String>, // 标签
    val charging: List<BilibiliUserInfo>, // 充电列表
)