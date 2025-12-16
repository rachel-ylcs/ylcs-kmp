package love.yinlin.api

import love.yinlin.api.APIConfig.coercePageNum
import love.yinlin.api.user.AN
import love.yinlin.api.user.VN
import love.yinlin.extension.String
import love.yinlin.extension.to
import love.yinlin.server.throwExecuteSQL
import love.yinlin.server.throwQuerySQLSingle

fun APIScope.rhymeAPI() {
    ApiRhymeUploadRecord.response { token, sid, difficulty, score ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(sid)
        db.throwTransaction {
            it.throwExecuteSQL(
                """
                INSERT INTO rhyme_record(sid, uid, difficulty, score)
                VALUES(?, ?, ?, ?)
            """.trimIndent(),
                sid, uid, difficulty, score
            )

            val user = it.throwQuerySQLSingle(
                "SELECT name FROM user WHERE uid = ?",
                uid
            )
            val name = user["name"].String

            // 按 (sid, difficulty, uid) 索引维护最高分
            it.throwExecuteSQL(
                """
                INSERT INTO rhyme_rank(sid, difficulty, uid, name, score)
                VALUES(?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    score = GREATEST(score, VALUES(score))
            """.trimIndent(),
                sid, difficulty, uid, name, score
            )
        }
    }

    // 基于 score+uid 游标分页
    ApiRhymeGetSongRank.response { sid, difficulty, score, uid, num ->
        VN.throwId(sid)

        val ranks = db.throwQuerySQL(
            """
            SELECT uid, name, score
            FROM rhyme_rank
            WHERE sid = ? AND difficulty = ?
              AND (score < ? OR (score = ? AND uid > ?))
            ORDER BY score DESC, uid ASC
            LIMIT ?
        """.trimIndent(),
            sid, difficulty, score, score, uid, num.coercePageNum
        )
        result(ranks.to())
    }
}


