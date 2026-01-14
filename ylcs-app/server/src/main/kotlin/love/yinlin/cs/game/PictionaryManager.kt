package love.yinlin.cs.game

import kotlinx.serialization.json.JsonElement
import love.yinlin.cs.service.Database
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameRecord
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.info.PConfig
import love.yinlin.data.rachel.game.info.PictionaryQuestion
import love.yinlin.extension.String
import love.yinlin.extension.to
import love.yinlin.extension.toJson

// 你画我猜
class PictionaryManager(db: Database) : RankGameManager(db) {
    override val config: PConfig = PConfig

    override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
        val (actualQuestion, length) = question.to<PictionaryQuestion>()
        val actualAnswer = answer.String
        require(actualQuestion.isNotEmpty())
        require(length == actualAnswer.length)
        require(actualAnswer.length in PConfig.minAnswerLength .. PConfig.maxAnswerLength)
    }

    override fun generateResult(details: GameDetails, record: GameRecord, userAnswer: JsonElement): GameResult {
        val isCompleted = details.answer.String == userAnswer.String
        return GameResult(
            isCompleted = isCompleted,
            reward = if (isCompleted) details.reward / details.num else 0,
            rank = if (isCompleted) details.winner.size + 1 else 0,
            info = Unit.toJson()
        )
    }
}