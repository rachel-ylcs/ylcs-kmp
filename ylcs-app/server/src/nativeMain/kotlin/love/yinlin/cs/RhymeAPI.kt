package love.yinlin.cs

import love.yinlin.cs.service.throwExecuteSQL
import love.yinlin.cs.service.throwQuerySQLSingle
import love.yinlin.cs.service.values
import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.RhymeDifficulty
import love.yinlin.data.rachel.rhyme.RhymeRepository
import love.yinlin.extension.*

fun ServerScope.rhymeAPI() {
    ApiRhymeGetUserRepository.response { token ->
        val uid = AN.throwExpireToken(token)
        val userData = mysql.throwQuerySQLSingle("SELECT rhyme FROM user_data WHERE uid = ?", uid)
        result(userData.obj("rhyme").to<RhymeRepository>())
    }

    ApiRhymeUnlockCharacter.response { token, id ->
        val uid = AN.throwExpireToken(token)
        mysql.throwTransaction { transaction ->
            val coin = transaction.throwQuerySQLSingle("SELECT coin FROM user WHERE uid = ? FOR UPDATE", uid)["coin"].Int
            val cost = CharacterInfo.Pool[id]?.cost ?: failure("未知立绘")
            if (coin < cost) failure("你的银币不够哦")
            val userData = transaction.throwQuerySQLSingle("SELECT rhyme FROM user_data WHERE uid = ? FOR UPDATE", uid)
            val repository = userData.obj("rhyme").to<RhymeRepository>()
            if (id in repository.characters) failure("你已经解锁该立绘")
            val newRhyme = repository.copy(characters = repository.characters + id)
            if (cost > 0) {
                transaction.throwExecuteSQL("""
                    UPDATE user SET coin = coin - ? , exp = exp + ?
                    WHERE uid = ? AND coin >= ?
                """, cost, cost / 2, uid, cost)
            }
            transaction.throwExecuteSQL("UPDATE user_data SET rhyme = ? WHERE uid = ?", newRhyme.toJsonString(), uid)
        }
    }

    ApiRhymeUploadRecord.response { token, sid, result ->
        val uid = AN.throwExpireToken(token)
        if (result.uid != uid || result.sid != sid || !result.valid) failure("数据一致性校验失败")
        mysql.throwInsertSQLGeneratedKey("""
            INSERT INTO rhyme_record(sid, uid, difficulty, score, result) ${values(5)}
        """, sid, uid, result.difficulty, result.score, result.toJsonString())
    }

    ApiRhymeGetRank.response { token, sid ->
        AN.throwExpireToken(token)
        val sql = RhymeDifficulty.entries.joinToString("\nUNION ALL") {
            "(SELECT rid, uid, result FROM rhyme_record WHERE sid = ? AND difficulty = ${it.ordinal} ORDER BY score DESC, rid ASC LIMIT 10)"
        }
        val args = Array<Any?>(enumSize<RhymeDifficulty>()) { sid }
        val rankList = mysql.throwQuerySQL("""
            SELECT t.rid, t.uid, t.result, u.name 
            FROM (
                $sql
            ) AS t
            LEFT JOIN user u ON t.uid = u.uid
        """, *args)
        result(rankList.to())
    }
}
