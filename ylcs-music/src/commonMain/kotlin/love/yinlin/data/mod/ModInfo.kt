package love.yinlin.data.mod

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ModInfo(
    val author: String = "无名", // 作者
)