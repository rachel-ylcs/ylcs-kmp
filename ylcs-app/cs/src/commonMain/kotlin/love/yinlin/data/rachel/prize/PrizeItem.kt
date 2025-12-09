package love.yinlin.data.rachel.prize
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import love.yinlin.api.ServerRes
import love.yinlin.Local
import love.yinlin.api.APIFile

@Stable
@Serializable
// 奖品类，可以有一二三等多种奖项，以及不同的奖项描述。该类作为Prize类的内部类关联
data class PrizeItem(
    val itemID:Int, //奖品ID
    val pid:Int,   //关联的抽奖事件id
    val prizeLevel:Int?, //奖品等级，如一等奖，二等奖，不填表示不分类的普通奖
    val name: String, //奖品名称
    val description:String?,//奖品描述
    val pic:String?, //图片描述
    val count:Int ,//奖品数量
){
    fun picPath(itemID: Int) : String ="${Local.API_BASE_URL}/${ServerRes.Prize.prize(itemID)}"
}

//API参数限制在5以下，所以把需要输入的属性提取出来做一个新类，这个类为不含APIFile的数据类，APIFile在API处会单独传入，特殊处理
@Serializable
data class PrizeItemdata(
    val prizeLevel:Int?,
    val name: String,
    val description:String?,
    val count:Int,
)

// API返回的奖品图片信息
@Stable
@Serializable
data class PrizeItemPic(
    val itemid: Int,
    val pic: String?
)
