package love.yinlin.data.radio

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedUserInfo

@Stable
@Serializable
data class RadioUserInfo(
    override val id: String,
    override val name: String,
    override val avatar: String = "",
    val signature: String = "", // 个性签名
) : UnifiedUserInfo {
    companion object {
        val Default = [
            RadioUserInfo(id = "803019", name = "银临的剪刀手")
        ]
    }
}