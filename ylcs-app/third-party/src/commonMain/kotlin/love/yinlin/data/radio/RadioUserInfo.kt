package love.yinlin.data.radio

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class RadioUserInfo(
    val id: String,
    val name: String,
    val avatar: String = "",
    val signature: String = "", // 个性签名
) {
    companion object {
        val Default = [
            RadioUserInfo(id = "803019", name = "银临的剪刀手")
        ]
    }
}