package love.yinlin.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ModMetadata(
    val version: Int, // 版本
    val mediaNum: Int, // 媒体数
    val info: ModInfo, // 额外信息
)