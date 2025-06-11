package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable

@Stable
abstract class GameConfig {
    val rewardCostRatio: Float = 1.2f // 佣金比

    open val minReward: Int = 1 // 最小奖励
    open val maxReward: Int = 30 // 最大奖励
    open val minRank: Int = 1 // 最小名额
    open val maxRank: Int = 3 // 最大名额

    val maxCostRatio: Int get() = maxReward / maxRank

    fun checkReward(reward: Int, num: Int, cost: Int): Boolean = reward in minReward .. maxReward &&
            num in minRank .. maxRank &&
            (cost in 0 .. (reward / maxCostRatio).coerceAtLeast(1))

    companion object : GameConfig()
}

@Stable
abstract class RankConfig : GameConfig() {
    companion object : RankConfig()
}

@Stable
abstract class ExplorationConfig : GameConfig() {
    open val minTryCount: Int = 3 // 最小尝试次数
    open val maxTryCount: Int = 10 // 最大尝试次数

    companion object : ExplorationConfig()
}

@Stable
abstract class SpeedConfig : GameConfig() {
    open val minTimeLimit: Int = 10 // 最短时间限制
    open val maxTimeLimit: Int = 3600 // 最长时间限制

    companion object : SpeedConfig()
}