package love.yinlin.api.user.game

import kotlinx.serialization.json.JsonElement
import love.yinlin.DB
import love.yinlin.api.failureData
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.ExplorationConfig
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameRecord
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.extension.ArrayEmpty
import love.yinlin.extension.Long
import love.yinlin.extension.toJson
import love.yinlin.extension.toJsonString
import love.yinlin.throwExecuteSQL
import love.yinlin.throwInsertSQLGeneratedKey
import love.yinlin.values
import java.sql.Connection

// 探索
abstract class ExplorationGameManager : GameManager() {
    override val config: ExplorationConfig = ExplorationConfig

    abstract fun fetchTryCount(info: JsonElement): Int

    override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
        val tryCount = fetchTryCount(info)
        require(tryCount in config.minTryCount .. config.maxTryCount)
    }

    override fun preflight(uid: Int, details: GameDetails): Data<PreflightResult> {
        // 检查重试记录
        val oldRecord = DB.querySQLSingle("""
            SELECT rid, answer, result FROM game_record WHERE uid = ? AND gid = ?
        """, uid, details.gid)
        val oldAnswer = oldRecord?.get("answer").ArrayEmpty
        val oldResult = oldRecord?.get("result").ArrayEmpty
        if (oldAnswer.size >= fetchTryCount(details.info)) return "重试次数达到上限".failureData
        return DB.throwTransaction {
            val rid = if (oldRecord == null) {
                // 消费银币
                if (!it.consumeCoin(uid, details)) return@throwTransaction "没有足够的银币参与".failureData
                // 插入游戏记录
                it.throwInsertSQLGeneratedKey("""
                    INSERT INTO game_record(gid, uid, answer, result) ${values(4)}
                """, details.gid, uid, "[]", "[]")
            } else oldRecord["rid"].Long
            Data.Success(PreflightResult(rid = rid, answer = oldAnswer, result = oldResult))
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