package love.yinlin.api.user.game

import kotlinx.serialization.json.JsonElement
import love.yinlin.api.failureData
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameRecord
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.SpeedConfig
import love.yinlin.extension.toJsonString
import love.yinlin.server.DB
import love.yinlin.server.throwExecuteSQL
import love.yinlin.server.throwInsertSQLGeneratedKey
import love.yinlin.server.values
import java.sql.Connection

// 竞速
abstract class SpeedGameManager : GameManager() {
    override val config: SpeedConfig = SpeedConfig

    abstract fun fetchTimeLimit(info: JsonElement): Int

    override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
        val timeLimit = fetchTimeLimit(info)
        require(timeLimit in config.minTimeLimit .. config.maxTimeLimit)
    }

    override fun preflight(uid: Int, details: GameDetails): Data<PreflightResult> = DB.throwTransaction {
        // 消费银币
        if (!it.consumeCoin(uid, details)) return@throwTransaction "没有足够的银币参与".failureData
        // 插入游戏记录
        val rid = it.throwInsertSQLGeneratedKey("""
            INSERT INTO game_record(gid, uid) ${values(2)}
        """, details.gid, uid)
        Data.Success(PreflightResult(rid))
    }

    override fun Connection.updateRecord(record: GameRecord, answer: JsonElement, result: GameResult) {
        throwExecuteSQL("""
            UPDATE game_record
            SET answer = ?, result = ? , ts = CURRENT_TIMESTAMP
            WHERE rid = ?
        """, answer.toJsonString(), result.toJsonString(), record.rid)
    }
}