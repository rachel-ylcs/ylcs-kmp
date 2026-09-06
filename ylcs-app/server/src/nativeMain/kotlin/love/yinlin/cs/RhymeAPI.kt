package love.yinlin.cs

import love.yinlin.cs.service.values
import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.RhymeDifficulty
import love.yinlin.data.rachel.rhyme.RhymeRepository
import love.yinlin.extension.*

fun ServerScope.rhymeAPI() {
    ApiRhymeGetUserRepository.response { token ->
        val uid = AN.throwExpireToken(token)
        val user = VN.throwGetUser(uid, "rhyme")
        result(user.obj("rhyme").to<RhymeRepository>())
    }

    ApiRhymeUnlockCharacter.response { token, id ->
        val uid = AN.throwExpireToken(token)
        val user = VN.throwGetUser(uid, "coin, rhyme")
        val coin = user["coin"].Int
        val cost = CharacterInfo.Pool[id]?.cost ?: failure("未知立绘")
        if (coin < cost) failure("你的银币不够哦")
        val repository = user.obj("rhyme").to<RhymeRepository>()
        val characters = repository.characters.toMutableList()
        if (id in characters) failure("你已经解锁该立绘")
        characters += id
        val newRhyme = repository.copy(characters = characters)
        mysql.throwExecuteSQL("""
            UPDATE user SET coin = coin - ? , exp = exp + ? , rhyme = ?
            WHERE uid = ? AND coin >= ?
        """, cost, cost / 2, newRhyme.toJsonString(), uid, cost)
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
        val args = Array<Any?>(RhymeDifficulty.entries.size) { sid }
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