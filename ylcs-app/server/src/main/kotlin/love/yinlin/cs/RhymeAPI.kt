package love.yinlin.cs

import love.yinlin.cs.user.*
import love.yinlin.data.rachel.rhyme.RhymeRepository
import love.yinlin.extension.obj
import love.yinlin.extension.to

fun APIScope.rhymeAPI() {
    ApiRhymeGetUserRepository.response { token ->
        val uid = AN.throwExpireToken(token)
        val user = db.throwQuerySQLSingle("SELECT rhyme FROM user WHERE uid = ?", uid)
        result(user.obj("rhyme").to<RhymeRepository>())
    }

    ApiRhymeUploadRecord.response { token, sid, result ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(sid)
    }
}