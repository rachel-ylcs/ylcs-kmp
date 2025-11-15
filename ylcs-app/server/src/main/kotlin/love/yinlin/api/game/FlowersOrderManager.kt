package love.yinlin.api.game

import kotlinx.serialization.json.JsonElement
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameRecord
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.info.FOConfig
import love.yinlin.data.rachel.game.info.FOInfo
import love.yinlin.data.rachel.game.info.FOType
import love.yinlin.extension.Int
import love.yinlin.extension.String
import love.yinlin.extension.to
import love.yinlin.extension.toJson

// 寻花令
data object FlowersOrderManager : ExplorationGameManager() {
    override val config: FOConfig = FOConfig

    override fun fetchTryCount(info: JsonElement): Int = info.to<FOInfo>().tryCount

    override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
        super.check(info, question, answer)
        val actualQuestion = question.Int
        val actualAnswer = answer.String
        // 问题长度
        require(actualQuestion in config.minLength .. config.maxLength)
        // 答案长度与问题一致
        require(actualAnswer.length == actualQuestion)
        // 无ASCII字符
        require(actualAnswer.all { FOType.check(it) })
    }

    private fun verifyAnswer(standardAnswer: JsonElement, userAnswer: JsonElement): Int {
        val actualStandardAnswer = standardAnswer.String
        val actualUserAnswer = userAnswer.String
        require(actualStandardAnswer.length == actualUserAnswer.length)
        val items = List(actualStandardAnswer.length) { i ->
            val ch1 = actualStandardAnswer[i]
            val ch2 = actualUserAnswer[i]
            if (ch1 == ch2) FOType.CORRECT
            else if (ch2 in actualStandardAnswer) FOType.INVALID_POS
            else FOType.INCORRECT
        }
        return FOType.encode(items)
    }

    override fun generateResult(details: GameDetails, record: GameRecord, userAnswer: JsonElement): GameResult {
        val foResult = verifyAnswer(details.answer, userAnswer)
        val isCompleted = FOType.verify(foResult)
        return GameResult(
            isCompleted = isCompleted,
            reward = if (isCompleted) details.reward / details.num else 0,
            rank = if (isCompleted) details.winner.size + 1 else 0,
            info = foResult.toJson()
        )
    }
}