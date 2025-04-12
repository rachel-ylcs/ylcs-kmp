package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.APICode
import love.yinlin.api.APIConfig.coercePageNum
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failedData
import love.yinlin.api.successData
import love.yinlin.api.successObject
import love.yinlin.data.Data
import love.yinlin.data.rachel.mail.MailEntry
import love.yinlin.extension.Int
import love.yinlin.extension.String
import love.yinlin.extension.to

fun Routing.mailAPI(implMap: ImplMap) {
	api(API.User.Mail.GetMails) { (token, isProcessed, offset, num) ->
		val uid = AN.throwExpireToken(token)
		val mails = DB.throwQuerySQL("""
			SELECT mid, uid, ts, type, processed, title, content
			FROM mail
			WHERE uid = ? AND ${
				if (isProcessed) "processed = 1 AND mid < ?"
				else "((processed = 0 AND mid < ?) OR processed = 1)"
			}
			ORDER BY processed ASC, mid DESC
			LIMIT ?
		""", uid, offset, num.coercePageNum)
		Data.Success(mails.to())
	}

	api(API.User.Mail.ProcessMail) { (token, mid, confirm) ->
		val uid = AN.throwExpireToken(token)
		val mailEntry = DB.throwQuerySQLSingle("""
			SELECT uid, processed, filter, param1, param2, param3, info
			FROM mail
			WHERE mid = ? AND uid = ?
		""", mid, uid).to<MailEntry>()
		if (mailEntry.processed) return@api "此邮件已被处理".failedData
		val ret = if (confirm) {
			val implFunc = implMap[mailEntry.filter]!!
			implFunc(mailEntry)
		} else "已拒绝此邮件".successObject
		// 处理成功后将processed置为 1
		if (ret["code"].Int == APICode.SUCCESS) {
			DB.throwExecuteSQL("UPDATE mail SET processed = 1 WHERE mid = ?", mid)
			ret["msg"].String.successData
		}
		else ret["msg"].String.failedData
	}

	api(API.User.Mail.DeleteMail) { (token, mid) ->
		val uid = AN.throwExpireToken(token)
		DB.throwExecuteSQL("DELETE FROM mail WHERE mid = ? AND uid = ?", mid, uid)
		"删除成功".successData
	}
}