package love.yinlin.data.rachel.prize

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

// 开奖结果,win表示中奖,loss表示没中奖,notdrawn表示还没开奖
@Stable
@Serializable
enum class PrizeResult {
    Win,
    Loss,
    Notdrawn,
    Cancelled,
}

@Stable
@Serializable
data class PrizeDraw(
    val drawid: Int, // 参与记录的id，自增
    val pid: Int, // 关联抽奖事件id
    val uid: Int, // 参与者id
    val result: PrizeResult = PrizeResult.Notdrawn, // 状态，默认为未开奖状态
    val prizeLevel: Int?, // 奖品等级，一二三等奖，只有在抽奖中设置了等级这里才会有，否则是空
    val name: String?, // 参与者姓名
    val ts: String // 参与抽奖的时间
)

@Stable
@Serializable
data class VerifyDrawResult(
    val isValid: Boolean, // 验证是否通过
    val seedCommitmentMatch: Boolean, // 种子承诺是否匹配
    val seedCommitment: String?, // 原始种子承诺
    val calculatedCommitment: String?, // 计算出的种子承诺
    val revealedSeed: String?, // 揭示的种子
    val participantCount: Int, // 参与者总数
    val drawNum: Int, // 中奖人数
    val calculatedWinners: List<Int>, // 计算出的中奖者UID列表
    val actualWinners: List<Int>, // 实际中奖者UID列表
    val winnersMatch: Boolean, // 中奖者列表是否匹配
    val calculatedWinnerLevels: Map<Int, Int?>, // 计算出的中奖者UID -> 奖品等级映射
    val actualWinnerLevels: Map<Int, Int?>, // 实际中奖者UID -> 奖品等级映射
    val winnerLevelsMatch: Boolean, // 中奖者等级分配是否匹配
    val errorMessage: String? // 错误信息（如果有）
)