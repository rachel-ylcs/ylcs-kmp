package love.yinlin.data.rachel.prize
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.rachel.prize.PrizeItem
@Stable
@Serializable
enum class PrizeStatus {
    draft,
    published,
    closed,
    cancelled
}
//抽奖所处的状态，分为草稿（未发布），发布，结束，取消

@Stable
@Serializable
data class Prize(
    val pid : Int ,  //抽奖事件ID
    val title :String, //标题
    val content :String, //内容描述
    val ts : String, // 成功创建某个抽奖时的创建时间
    val organizerUid : Int, //发布活动者的ID，用于鉴权
    val status: PrizeStatus = PrizeStatus.draft,  //抽奖状态，默认为草稿状态
    val deadline: String, //参与抽奖的截止日期，必须填写
    val drawtime: String?, //计划开奖时间，用于展nt示
    val drawNum:Int, //中奖人数
    val mixAppLevel: Int, //参与者的最小等级
    val totalSlots :Int?, //参与者的总人数限制，不填为无限制
    val prizeItems: List<PrizeItem>, //奖品，可以包含多个奖品
    val seedCommitment: String?, //种子的SHA256哈希（公开承诺），发布时生成
    val revealedSeed: String? //公开的种子（开奖后揭示），开奖时公开
)


//API参数限制在5以下，所以把需要输入的属性提取出来封装成新类，该类仅会在createPrize时使用到
@Serializable
data class Prizedata(
    val title :String,
    val content :String,
    val deadline: String,
    val drawtime: String?,
    val mixAppLevel: Int,
    val totalSlots :Int?
)

