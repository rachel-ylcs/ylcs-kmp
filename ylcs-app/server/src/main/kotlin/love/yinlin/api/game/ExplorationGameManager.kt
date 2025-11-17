package love.yinlin.api.game

import kotlinx.serialization.json.JsonElement
import love.yinlin.api.FailureException
import love.yinlin.data.rachel.game.ExplorationConfig
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameRecord
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.extension.ArrayEmpty
import love.yinlin.extension.Long
import love.yinlin.extension.toJson
import love.yinlin.extension.toJsonString
import love.yinlin.server.Database
import love.yinlin.server.throwExecuteSQL
import love.yinlin.server.throwInsertSQLGeneratedKey
import love.yinlin.server.values
import java.sql.Connection

// 探索
abstract class ExplorationGameManager(db: Database) : GameManager(db) {
    override val config: ExplorationConfig = ExplorationConfig

    abstract fun fetchTryCount(info: JsonElement): Int

    override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
        val tryCount = fetchTryCount(info)
        require(tryCount in config.minTryCount .. config.maxTryCount)
    }

    override fun preflight(uid: Int, details: GameDetails): PreflightResult {
        // 检查重试记录
        val oldRecord = db.querySQLSingle("""
            SELECT rid, answer, result FROM game_record WHERE uid = ? AND gid = ?
        """, uid, details.gid)
        val oldAnswer = oldRecord?.get("answer").ArrayEmpty
        val oldResult = oldRecord?.get("result").ArrayEmpty
        if (oldAnswer.size >= fetchTryCount(details.info)) throw FailureException("重试次数达到上限")
        return db.throwTransaction {
            val rid = if (oldRecord == null) {
                // 消费银币
                if (!it.consumeCoin(uid, details)) throw FailureException("没有足够的银币参与")
                // 插入游戏记录
                it.throwInsertSQLGeneratedKey("""
                    INSERT INTO game_record(gid, uid, answer, result) ${values(4)}
                """, details.gid, uid, "[]", "[]")
            } else oldRecord["rid"].Long
            PreflightResult(rid = rid, answer = oldAnswer, result = oldResult)
        }
    }

    override fun Connection.updateRecord(record: GameRecord, answer: JsonElement, result: GameResult) {
        val newAnswer = record.answer.ArrayEmpty.toMutableList().plus(answer)
        val newResult = record.result.ArrayEmpty.toMutableList().plus(result.toJson())
        throwExecuteSQL("""
            UPDATE game_record
            SET answer = ?, result = ? , ts = CURRENT_TIMESTAMP
            WHERE rid = ?
        """, newAnswer.toJsonString(), newResult.toJsonString(), record.rid)
    }
}