package love.yinlin.data.rachel.server

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ServerStatus(
    val targetVersion: Int, // [服务器最新版本]
    val minVersion: Int, // [最低兼容版本]
    val downloadUrl: String // [更新链接]
)