package love.yinlin.data.rachel.rhyme

import androidx.compose.runtime.Stable

@Stable
data class RhymePlayResult(
    val duration: Long, // 时长
    val score: Int, // 得分
    val maxCombo: Int, // 最大连击
    val statistics: List<Int>, // 统计
)