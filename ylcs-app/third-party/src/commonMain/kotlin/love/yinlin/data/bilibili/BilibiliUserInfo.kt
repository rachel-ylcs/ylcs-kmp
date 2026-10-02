package love.yinlin.data.bilibili

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedUserInfo

@Stable
@Serializable
data class BilibiliUserInfo(
    override val id: String,
    override val name: String,
    override val avatar: String
) : UnifiedUserInfo {
    companion object {
        val Default = [
            BilibiliUserInfo(id = "2460656", name = "银临", avatar = "")
        ]
    }
}