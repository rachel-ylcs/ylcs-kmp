package love.yinlin.data.rachel.game.info

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class SAInfo(
    val threshold: Float, // [成功阈值]
    val timeLimit: Int, // [时间限制]
)

@Stable
@Serializable
data class SAResult(val correctCount: Int, val totalCount: Int, val duration: Int)