package love.yinlin.api.user.game

import kotlinx.serialization.json.JsonElement
import love.yinlin.DB
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.*
import love.yinlin.extension.toJsonString
import love.yinlin.throwExecuteSQL
import love.yinlin.updateSQL
import java.sql.Connection

internal val Game.manager: GameManager get() = when (this) {
    Game.AnswerQuestion -> Game1Manager
    Game.BlockText -> Game2Manager
    Game.FlowersOrder -> Game3Manager
    Game.SearchAll -> Game4Manager
}

sealed class GameManager {
    open val config: GameConfig get() = GameConfig

    // 检查题目
    abstract fun check(info: JsonElement, question: JsonElement, answer: JsonElement)

    // 消费银币
    fun Connection.consumeCoin(uid: Int, details: GameDetails): Boolean {
        val cost = details.cost
        if (cost > 0) {
            if (updateSQL("UPDATE user SET coin = coin - ? WHERE uid = ? AND coin >= ?", cost, uid, cost)) {
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

    // 更新游戏记录
    abstract fun Connection.updateRecord(record: GameRecord, answer: JsonElement, result: GameResult)

    // 更新游戏排行榜
    fun Connection.updateRank(uid: Int, details: GameDetails, isCompleted: Boolean) {
        if (isCompleted) {
            val isGameCompleted = details.winner.size + 1 >= details.num
            throwExecuteSQL("""
                UPDATE game
                SET winner = ? , isCompleted = ?
                WHERE gid = ?
            """, details.winner.plus(uid.toString()).toJsonString(), isGameCompleted, details.gid)
        }
    }

    // 验证游戏
    fun verify(uid: Int, details: GameDetails, record: GameRecord, userAnswer: JsonElement): Data<GameResult> {
        val result = generateResult(details, record, userAnswer)
        DB.throwTransaction {
            it.consumeCoin(uid, details)
            it.updateRecord(record, userAnswer, result)
            it.updateRank(uid, details, result.isCompleted)
        }
        return Data.Success(result)
    }
}