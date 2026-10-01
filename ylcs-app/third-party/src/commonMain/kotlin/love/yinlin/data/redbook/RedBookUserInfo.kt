package love.yinlin.data.redbook

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedUserInfo

@Stable
@Serializable
data class RedBookUserInfo(
    override val id: String,
    override val name: String,
    override val avatar: String,
) : UnifiedUserInfo {
    companion object {
        val Default = [
            RedBookUserInfo(id = "5d9c417e0000000001002887", name = "银临-欢迎光临", avatar = "")
        ]
    }
}