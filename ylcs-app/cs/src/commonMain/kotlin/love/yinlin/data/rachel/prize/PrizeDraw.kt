package love.yinlin.data.rachel.prize

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

// 参与记录 - 只记录用户参与信息
@Stable
@Serializable
data class PrizeDraw(
    val drawid: Int, // 参与记录的id，自增
    val pid: Int, // 关联抽奖事件id
    val uid: Int, // 参与者id
    val name: String?, // 参与者姓名
    val ts: String // 参与抽奖的时间
)
