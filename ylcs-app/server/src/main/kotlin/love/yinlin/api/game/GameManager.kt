package love.yinlin.api.game

import kotlinx.serialization.json.JsonElement
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.*
import love.yinlin.extension.toJsonString
import love.yinlin.server.DB
import love.yinlin.server.throwExecuteSQL
import love.yinlin.server.updateSQL
import java.sql.Connection

internal val Game.manager: GameManager get() = when (this) {
    Game.AnswerQuestion -> AnswerQuestionManager
    Game.BlockText -> BlockTextManager
    Game.FlowersOrder -> FlowersOrderManager
    Game.SearchAll -> SearchAllManager
    Game.Pictionary -> PictionaryManager
    Game.GuessLyrics, Game.Rhyme -> error("Unknown game: $this")
}

sealed class GameManager {
    open val config: GameConfig = GameConfig

    // 检查题目
    abstract fun check(info: JsonElement, question: JsonElement, answer: JsonElement)

    // 消费银币
    fun Connection.consumeCoin(uid: Int, details: GameDetails): Boolean {
        val cost = details.cost
        if (cost > 0) {
            if (updateSQL("UPDATE user SET coin = coin - ?, exp = exp + 1 WHERE uid = ? AND coin >= ?", cost, uid, cost)) {
                // 增加发起者银币
                throwExecuteSQL("UPDATE user SET coin = coin + ? WHERE uid = ?", cost, details.uid)
            }
            else return false
        }
        return true
    }

    // 预检游戏
    abstract fun preflight(uid: Int, details: GameDetails): Data<PreflightResult>

    // 生成结果
    abstract fun generateResult(details: GameDetails, record: GameRecord, userAnswer: JsonElement): GameResult

    // 更新奖励
    fun Connection.updateReward(uid: Int, details: GameDetails, result: GameResult) {
        val perReward = details.reward / details.num
        if (result.isCompleted && perReward in 1 ..GameConfig.maxReward / details.num) {
            throwExecuteSQL("UPDATE user SET coin = coin + ? WHERE uid = ?", perReward, uid)
        }
    }

    // 更新游戏记录
    abstract fun Connection.updateRecord(record: GameRecord, answer: JsonElement, result: GameResult)

    // 更新游戏排行榜
    fun Connection.updateRank(uid: Int, details: GameDetails, isCompleted: Boolean) {
        if (isCompleted && details.winner.size < details.num) {
            val newWinner = details.winner.plus(uid)
            val isGameCompleted = newWinner.size >= details.num
            throwExecuteSQL("""
                UPDATE game
                SET winner = ? , isCompleted = ?
                WHERE gid = ?
            """, newWinner.toJsonString(), isGameCompleted, details.gid)
            if (isGameCompleted) throwExecuteSQL("UPDATE user SET exp = exp + ? WHERE uid = ?", details.reward / 2, details.uid)
        }
    }

    // 验证游戏
    fun verify(uid: Int, details: GameDetails, record: GameRecord, userAnswer: JsonElement): Data<GameResult> {
        val result = generateResult(details, record, userAnswer)
        DB.throwTransaction {
            it.updateReward(uid, details, result)
            it.updateRecord(record, userAnswer, result)
            it.updateRank(uid, details, result.isCompleted)
        }
        return Data.Success(result)
    }
}