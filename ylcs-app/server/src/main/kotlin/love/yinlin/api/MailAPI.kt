package love.yinlin.api

import love.yinlin.api.APIConfig.coercePageNum
import love.yinlin.api.user.AN
import love.yinlin.api.user.VN
import love.yinlin.callMap
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.mail.MailEntry
import love.yinlin.extension.to

fun APIScope.mailAPI() {
    ApiMailGetMails.response { token, isProcessed, mid, num ->
        val uid = AN.throwExpireToken(token)
        val mails = db.throwQuerySQL("""
			SELECT mid, uid, ts, type, processed, title, content
			FROM mail
			WHERE uid = ? AND ${
                if (isProcessed) "processed = 1 AND mid < ?"
                else "((processed = 0 AND mid < ?) OR processed = 1)"
            }
			ORDER BY processed ASC, mid DESC
			LIMIT ?
		""", uid, mid, num.coercePageNum)
        result(mails.to())
    }

    ApiMailProcessMail.response { token, mid, confirm ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(mid)
        val mailEntry = db.throwQuerySQLSingle("""
			SELECT uid, processed, filter, param1, param2, param3, info
			FROM mail
			WHERE mid = ? AND uid = ?
		""", mid, uid).to<MailEntry>()
        if (mailEntry.processed) failure("此邮件已被处理")
        val ret = if (confirm) {
            val filter = Mail.Filter.fromValue(mailEntry.filter)!!
            val callback = callMap[filter]!!
            callback(mailEntry)
        }
        else "已拒绝此邮件"
        // 处理成功后将processed置为 1
        db.throwExecuteSQL("UPDATE mail SET processed = 1 WHERE mid = ?", mid)
        result(ret)
    }

    ApiMailDeleteMail.response { token, mid ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(mid)
        db.throwExecuteSQL("DELETE FROM mail WHERE mid = ? AND uid = ?", mid, uid)
    }
}