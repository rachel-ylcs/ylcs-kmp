package love.yinlin.api.user.game

import kotlinx.serialization.json.JsonElement
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameRecord
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.info.AQAnswer
import love.yinlin.data.rachel.game.info.AQConfig
import love.yinlin.data.rachel.game.info.AQInfo
import love.yinlin.data.rachel.game.info.AQQuestion
import love.yinlin.data.rachel.game.info.AQResult
import love.yinlin.data.rachel.game.info.AQUserAnswer
import love.yinlin.extension.to
import love.yinlin.extension.toJson

// 答题
data object AnswerQuestionManager : RankGameManager() {
    override val config: AQConfig = AQConfig

    override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
        val actualInfo = info.to<AQInfo>()
        val actualQuestion = question.to<List<AQQuestion>>()
        val actualAnswer = answer.to<List<AQAnswer>>()
        require(actualInfo.threshold in config.minThreshold .. config.maxThreshold)
        require(actualQuestion.size in config.minQuestionCount .. config.maxQuestionCount)
        require(actualQuestion.size == actualAnswer.size)
        for (i in actualQuestion.indices) actualAnswer[i].matchQuestion(config, actualQuestion[i])
    }

    private fun verifyAnswer(standardAnswer: JsonElement, userAnswer: JsonElement): AQResult {
        val actualStandardAnswer = standardAnswer.to<List<AQAnswer>>()
        val actualUserAnswer = userAnswer.to<List<AQUserAnswer>>()
        val size = actualStandardAnswer.size
        require(size == actualUserAnswer.size)
        var correctCount = 0
        repeat(size) { i ->
            if (actualUserAnswer[i].verifyAnswer(actualStandardAnswer[i])) ++correctCount
        }
        return AQResult(correctCount = correctCount, totalCount = size)
    }

    override fun generateResult(details: GameDetails, record: GameRecord, userAnswer: JsonElement): GameResult {
        val info = details.info.to<AQInfo>()
        val aqResult = verifyAnswer(details.answer, userAnswer)
        val isCompleted = (aqResult.correctCount.toFloat() / aqResult.totalCount) >= info.threshold
        return GameResult(
            isCompleted = isCompleted,
            reward = if (isCompleted) details.reward / details.num else 0,
            rank = if (isCompleted) details.winner.size + 1 else 0,
            info = aqResult.toJson()
        )
    }
}