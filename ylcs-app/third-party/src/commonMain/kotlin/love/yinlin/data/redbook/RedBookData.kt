package love.yinlin.data.redbook

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedData

@Stable
@Serializable
data class RedBookData(val likeNum: Int) : UnifiedData