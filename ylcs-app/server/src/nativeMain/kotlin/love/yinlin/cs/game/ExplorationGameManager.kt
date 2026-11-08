package love.yinlin.cs.game

import io.github.smyrgeorge.sqlx4k.QueryExecutor
import kotlinx.serialization.json.JsonElement
import love.yinlin.cs.FailureException
import love.yinlin.cs.service.throwExecuteSQL
import love.yinlin.cs.service.throwInsertSQLGeneratedKey
import love.yinlin.cs.service.throwQuerySQL
import love.yinlin.cs.service.values
import love.yinlin.data.rachel.game.ExplorationConfig
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameRecord
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.extension.ArrayEmpty
import love.yinlin.extension.Long
import love.yinlin.extension.Object
import love.yinlin.extension.toJson
import love.yinlin.extension.toJsonString

// 探索
abstract class ExplorationGameManager : GameManager() {
    override val config: ExplorationConfig = ExplorationConfig

    abstract fun fetchTryCount(info: JsonElement): Int

    override suspend fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
        val tryCount = fetchTryCount(info)
        require(tryCount in config.minTryCount .. config.maxTryCount)
    }

    private fun checkTryCount(details: GameDetails, count: Int) {
        if (count >= fetchTryCount(details.info)) throw FailureException("重试次数达到上限")
    }

    override fun checkSubmission(details: GameDetails, record: GameRecord) {
        checkTryCount(details, record.answer.ArrayEmpty.size)
    }

    override suspend fun preflight(transaction: QueryExecutor, uid: Int, details: GameDetails): PreflightResult {
        // 检查重试记录
        val records = transaction.throwQuerySQL("""
            SELECT rid, answer, result FROM game_record WHERE uid = ? AND gid = ?
            LIMIT 2
        """, uid, details.gid)
        check(records.size <= 1) { "Duplicate exploration game records" }
        val oldRecord = records.firstOrNull()?.Object
        val oldAnswer = oldRecord?.get("answer").ArrayEmpty
        val oldResult = oldRecord?.get("result").ArrayEmpty
        checkTryCount(details, oldAnswer.size)
        val rid = if (oldRecord == null) {
            // 消费银币
            if (!transaction.consumeCoin(uid, details)) throw FailureException("没有足够的银币参与")
            // 插入游戏记录
            transaction.throwInsertSQLGeneratedKey("""
                INSERT INTO game_record(gid, uid, answer, result) ${values(4)}
            """, details.gid, uid, "[]", "[]")
        }
        else oldRecord["rid"].Long
        return PreflightResult(rid = rid, answer = oldAnswer, result = oldResult)
    }

    override suspend fun QueryExecutor.updateRecord(record: GameRecord, answer: JsonElement, result: GameResult) {
        val newAnswer = record.answer.ArrayEmpty.toMutableList().plus(answer)
        val newResult = record.result.ArrayEmpty.toMutableList().plus(result.toJson())
        throwExecuteSQL("""
            UPDATE game_record
            SET answer = ?, result = ? , ts = CURRENT_TIMESTAMP
            WHERE rid = ?
        """, newAnswer.toJsonString(), newResult.toJsonString(), record.rid)
    }
}
