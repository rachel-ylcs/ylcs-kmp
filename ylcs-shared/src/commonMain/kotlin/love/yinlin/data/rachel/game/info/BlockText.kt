package love.yinlin.data.rachel.game.info

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.rachel.game.RankConfig

@Stable
@Suppress("MayBeConstant")
data object BTConfig : RankConfig() {
    const val CHAR_EMPTY = '$' // 空字符
    const val CHAR_BLOCK = '#' // 方格字符
    val minBlockSize: Int = 7 // 最小网格大小
    val maxBlockSize: Int = 12 // 最大网格大小
}

@Stable
@Serializable
data class BTResult(val correctCount: Int, val totalCount: Int)