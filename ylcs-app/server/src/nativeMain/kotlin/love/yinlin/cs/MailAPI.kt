package love.yinlin.cs

import love.yinlin.cs.APIConfig.coercePageNum
import love.yinlin.cs.service.throwExecuteSQL
import love.yinlin.cs.service.throwQuerySQLSingle
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.mail.MailEntry
import love.yinlin.extension.enum
import love.yinlin.extension.to

fun ServerScope.mailAPI() {
    ApiMailGetMails.response { token, isProcessed, mid, num ->
        val uid = AN.throwExpireToken(token)
        val mails = mysql.throwQuerySQL("""
			SELECT mid, uid, ts, type, processed, title, content
			FROM mail
			WHERE uid = ? AND ${
                if (isProcessed) "processed = 1 AND mid < ?"
                else "((processed = 0 AND mid < ?) OR processed = 1)"
            }
			ORDER BY processed, mid DESC
			LIMIT ?
		""", uid, mid, num.coercePageNum)
        result(mails.to())
    }

    ApiMailProcessMail.response { token, mid, confirm ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(mid)
        val ret = mysql.throwTransaction { transaction ->
            val mailEntry = transaction.throwQuerySQLSingle("""
                SELECT uid, processed, filter, param1, param2, param3, info
                FROM mail
                WHERE mid = ? AND uid = ?
                FOR UPDATE
            """, mid, uid).to<MailEntry>()

            if (mailEntry.processed) failure("此邮件已被处理")

            val message = if (confirm) {
                val filter: Mail.Filter = enum(mailEntry.filter) { it.toString() }
                val callback = callMap[filter] ?: failure("不支持的邮件处理类型")
                callback(transaction, mailEntry)
            }
            else "已拒绝此邮件"

            // 处理成功后将processed置为 1
            transaction.throwExecuteSQL(
                "UPDATE mail SET processed = 1 WHERE mid = ? AND uid = ? AND processed = 0",
                mid, uid
            )
            message
        }
        result(ret)
    }

    ApiMailDeleteMail.response { token, mid ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(mid)
        mysql.throwExecuteSQL("DELETE FROM mail WHERE mid = ? AND uid = ?", mid, uid)
    }
}