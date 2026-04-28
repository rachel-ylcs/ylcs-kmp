package love.yinlin.cs

import love.yinlin.cs.service.values
import love.yinlin.cs.user.*
import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.RhymeRepository
import love.yinlin.extension.Int
import love.yinlin.extension.obj
import love.yinlin.extension.to
import love.yinlin.extension.toJsonString

fun APIScope.rhymeAPI() {
    ApiRhymeGetUserRepository.response { token ->
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "rhyme")
        result(user.obj("rhyme").to<RhymeRepository>())
    }

    ApiRhymeUnlockCharacter.response { token, id ->
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "coin, rhyme")
        val coin = user["coin"].Int
        val cost = CharacterInfo.Pool[id]?.cost ?: failure("未知立绘")
        if (coin < cost) failure("你的银币不够哦")
        val repository = user.obj("rhyme").to<RhymeRepository>()
        val characters = repository.characters.toMutableList()
        if (id in characters) failure("你已经解锁该立绘")
        characters += id
        val newRhyme = repository.copy(characters = characters)
        db.throwExecuteSQL("""
            UPDATE user SET coin = coin - ? , exp = exp + ? , rhyme = ?
            WHERE uid = ? AND coin >= ?
        """, cost, cost / 2, newRhyme.toJsonString(), uid, cost)
    }

    ApiRhymeUploadRecord.response { token, sid, result ->
        val uid = AN.throwExpireToken(token)
        if (result.uid != uid || result.sid != sid || !result.valid) failure("数据一致性校验失败")
        db.throwInsertSQLGeneratedKey("INSERT INTO rhyme_record(sid, uid, result) ${values(3)}", sid, uid, result.toJsonString())
    }

    ApiRhymeGetRank.response { token, sid ->
        AN.throwExpireToken(token)
        val rankList = db.throwQuerySQL("""
            SELECT rid, user.uid, name, result
            FROM rhyme_record
            LEFT JOIN user
            ON rhyme_record.uid = user.uid
            WHERE sid = ?
        """, sid)
        result(rankList.to())
    }
}