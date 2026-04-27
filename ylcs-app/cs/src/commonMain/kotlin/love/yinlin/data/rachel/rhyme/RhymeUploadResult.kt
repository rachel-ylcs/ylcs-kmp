package love.yinlin.data.rachel.rhyme

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Serializable
@Stable
data class RhymeUploadResult(
    val difficulty: Int, // 难度
    val character: Int, // 角色ID
    val statistics: List<Int>, // 统计数据
    val score: Int, // 分数
    val hash: String,
)