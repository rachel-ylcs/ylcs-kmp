package love.yinlin.api

import love.yinlin.Local
import love.yinlin.api.user.AN
import love.yinlin.api.user.VN
import love.yinlin.callMap
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.server.ServerStatus
import love.yinlin.server.values

fun APIScope.commonAPI() {
    ApiCommonGetServerStatus.response {
        result(ServerStatus(
            targetVersion = Local.info.version,
            minVersion = Local.info.minVersion,
        ))
    }

    ApiCommonSendFeedback.response { token, content ->
        val uid = AN.throwExpireToken(token)
        if (db.throwInsertSQLDuplicateKey("INSERT INTO feedback(uid, content) ${values(2)}", uid, content)) failure("您已提交过反馈, 请耐心等待处理")
    }

    callMap[Mail.Filter.CoinReward] = {
        val uid = it.uid
        val coin = it.param1?.toIntOrNull() ?: 0
        VN.throwIf(coin <= 0)
        db.throwExecuteSQL("UPDATE user SET coin = coin + ? WHERE uid = ?", coin, uid)
        "领取成功"
    }
}