package love.yinlin.data.rachel.game.info

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.rachel.game.SpeedConfig

@Stable
@Suppress("MayBeConstant")
data object SAConfig : SpeedConfig() {
    val minThreshold: Float = 0.5f // 最小成功阈值
    val maxThreshold: Float = 1f // 最大成功阈值
    val minCount: Int = 3 // 最小数量
    val maxCount: Int = 100 // 最大数量
    val minLength: Int = 1 // 最小长度
    val maxLength: Int = 18 // 最大长度
}

@Stable
@Serializable
data class SAInfo(
    val threshold: Float, // [成功阈值]
    val timeLimit: Int, // [时间限制]
)

@Stable
@Serializable
data class SAResult(val correctCount: Int, val totalCount: Int, val duration: Int)