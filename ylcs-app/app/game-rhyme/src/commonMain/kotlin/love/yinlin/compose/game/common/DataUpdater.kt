package love.yinlin.compose.game.common

import androidx.compose.runtime.*
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.game.data.RhymeDifficulty
import love.yinlin.compose.game.data.RhymePlayInfo

@Stable
class DataUpdater {
    /**
     * 音频进度
     */
    var audioProgress: Float by mutableFloatStateOf(0f)

    /**
     * 得分
     */
    var score: Int by mutableIntStateOf(0)
        private set

    /**
     * 当前结果 连击
     */
    var currentResult: BlockUpdateResult? by mutableRefStateOf(null)
        private set

    /**
     * 得分计数器
     */
    val statistics = IntArray(BlockResult.entries.size)

    // 连击奖励
    private var comboRewardCount = 20

    fun init(playInfo: RhymePlayInfo) {
        comboRewardCount = when (playInfo.playConfig.difficulty) {
            RhymeDifficulty.Easy -> 30
            RhymeDifficulty.Medium -> 25
            RhymeDifficulty.Hard -> 20
            RhymeDifficulty.Extreme -> 20
        }
    }

    fun reset() {
        audioProgress = 0f
        score = 0
        comboRewardCount = 20
    }

    fun updateResult(id: Long, result: BlockResult, scoreRatio: Float = 1f) {
        // 统计
        statistics[result.ordinal] += 1
        // 计算连击
        val oldCombo = currentResult?.combo ?: 0
        val newCombo = if (result == BlockResult.MISS || result == BlockResult.BAD) 0 else oldCombo + 1
        currentResult = BlockUpdateResult(result, newCombo, id)
        // 计算得分
        val reward = (result.score * scoreRatio).toInt()
        score = reward + newCombo / comboRewardCount // 连击奖励
    }
}