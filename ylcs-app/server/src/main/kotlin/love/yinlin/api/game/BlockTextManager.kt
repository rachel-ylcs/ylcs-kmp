package love.yinlin.api.game

import kotlinx.serialization.json.JsonElement
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameRecord
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.info.BTConfig
import love.yinlin.data.rachel.game.info.BTResult
import love.yinlin.extension.String
import love.yinlin.extension.toJson
import love.yinlin.server.Database
import kotlin.math.sqrt

// 网格填词
class BlockTextManager(db: Database) : RankGameManager(db) {
    override val config: BTConfig = BTConfig

    override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
        val actualQuestion = question.String
        val actualAnswer = answer.String
        val blockSize = sqrt(actualQuestion.length.toFloat()).toInt()
        // 网格大小是完全平方数
        require(blockSize in config.minBlockSize .. config.maxBlockSize)
        require(blockSize * blockSize == actualQuestion.length)
        // 题目与答案大小相同
        require(actualQuestion.length == actualAnswer.length)
        // 题目中至少包含一个方格
        require(actualQuestion.contains(BTConfig.CHAR_BLOCK))
        // 答案中不能包含方格且至少包含一个非空
        require(!actualAnswer.contains(BTConfig.CHAR_BLOCK) && !actualAnswer.all { it == BTConfig.CHAR_EMPTY })
        for (index in actualQuestion.indices) {
            val q = actualQuestion[index]
            val a = actualAnswer[index]
            when (q) {
                // 题目空则答案空
                BTConfig.CHAR_EMPTY -> require(a == BTConfig.CHAR_EMPTY)
                // 题目方格则答案非空
                BTConfig.CHAR_BLOCK -> require(a != BTConfig.CHAR_EMPTY)
                // 否则题目和答案一致
                else -> require(a == q)
            }
        }
    }

    private fun verifyAnswer(standardAnswer: JsonElement, userAnswer: JsonElement): BTResult {
        val actualStandardAnswer = standardAnswer.String
        val actualUserAnswer = userAnswer.String
        require(actualStandardAnswer.length == actualUserAnswer.length)
        var correctCount = 0
        var totalCount = 0
        for (i in actualStandardAnswer.indices) {
            val ch1 = actualStandardAnswer[i]
            val ch2 = actualUserAnswer[i]
            if (ch2 != BTConfig.CHAR_EMPTY) {
                ++totalCount
                if (ch1 == ch2) ++correctCount
            }
        }
        return BTResult(correctCount = correctCount, totalCount = totalCount)
    }

    override fun generateResult(details: GameDetails, record: GameRecord, userAnswer: JsonElement): GameResult {
        val btResult = verifyAnswer(details.answer, userAnswer)
        val isCompleted = btResult.correctCount == btResult.totalCount
        return GameResult(
            isCompleted = isCompleted,
            reward = if (isCompleted) details.reward / details.num else 0,
            rank = if (isCompleted) details.winner.size + 1 else 0,
            info = btResult.toJson()
        )
    }
}