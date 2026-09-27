package love.yinlin.data.douyin

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedUserInfo

@Stable
@Serializable
data class DouyinUserInfo(
    override val id: String, // ID
    override val name: String, // 昵称
    override val avatar: String, // 头像
    val uid: String, // 抖音的UID
) : UnifiedUserInfo {
    companion object {
        val Default = [
            DouyinUserInfo("MS4wLjABAAAATAf7yHksdW6CBPSjl9CW8k3c_x_drbwg0CVLTowlwzE", "银临", "", "589727862"),
        ]
    }
}