package love.yinlin.api.game

import kotlinx.serialization.json.JsonElement
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameRecord
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.info.SAConfig
import love.yinlin.data.rachel.game.info.SAInfo
import love.yinlin.data.rachel.game.info.SAResult
import love.yinlin.extension.Int
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.server.SQLConverter

// 词寻
data object SearchAllManager : SpeedGameManager() {
    override val config: SAConfig = SAConfig

    override fun fetchTimeLimit(info: JsonElement): Int = info.to<SAInfo>().timeLimit

    override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
        super.check(info, question, answer)
        val actualInfo = info.to<SAInfo>()
        val actualQuestion = question.Int
        val actualAnswer = answer.to<List<String>>()
        // 阈值限制
        require(actualInfo.threshold in config.minThreshold .. config.maxThreshold)
        // 答案数量限制
        require(actualAnswer.size in config.minCount .. config.maxCount)
        // 答案长度与问题一致
        require(actualAnswer.size == actualQuestion)
        // 备选答案长度限制
        require(actualAnswer.all { it.length in config.minLength .. config.maxLength })
        // 答案无重复
        require(actualAnswer.size == actualAnswer.toSet().size)
    }

    private fun verifyAnswer(standardAnswer: JsonElement, userAnswer: JsonElement, duration: Int): SAResult {
        val actualStandardAnswer = standardAnswer.to<List<String>>().toHashSet()
        val actualUserAnswer = userAnswer.to<List<String>>().toHashSet()
        var correctCount = 0
        for (answer in actualUserAnswer) {
            if (answer in actualStandardAnswer) ++correctCount
        }
        return SAResult(
            correctCount = correctCount,
            totalCount = actualStandardAnswer.size,
            duration = duration
        )
    }

    override fun generateResult(details: GameDetails, record: GameRecord, userAnswer: JsonElement): GameResult {
        val info = details.info.to<SAInfo>()
        val startTime = SQLConverter.convertTime(record.ts)
        val endTime = System.currentTimeMillis()
        val duration = ((endTime - startTime) / 1000).toInt()
        val saResult = verifyAnswer(details.answer, userAnswer, duration)
        val isCompleted = saResult.correctCount.toFloat() / saResult.totalCount >= info.threshold && duration <= info.timeLimit
        return GameResult(
            isCompleted = isCompleted,
            reward = if (isCompleted) details.reward / details.num else 0,
            rank = if (isCompleted) details.winner.size + 1 else 0,
            info = saResult.toJson()
        )
    }
}