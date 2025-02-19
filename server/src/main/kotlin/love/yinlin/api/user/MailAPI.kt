package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.APICode
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failedData
import love.yinlin.api.successData
import love.yinlin.api.successObject
import love.yinlin.data.Data
import love.yinlin.extension.Boolean
import love.yinlin.extension.Int
import love.yinlin.extension.ObjectEmpty
import love.yinlin.extension.String
import love.yinlin.extension.makeObject

fun Routing.mailAPI(implMap: ImplMap) {
	// ------  查询邮件  ------
	api(API.User.Mail.GetMails) { (token, offset, num) ->
		val uid = AN.throwExpireToken(token)
		val mails = DB.throwQuerySQL("""
			SELECT mid, uid, ts, type, processed, title, content
			FROM mail
			WHERE uid = ? AND mid < ?
			ORDER BY ts DESC
			LIMIT ?
		""", uid, offset, num.coerceAtLeast(10).coerceAtMost(20))
		Data.Success(mails)
	}

	// ------  处理邮件  ------
	api(API.User.Mail.ProcessMail) { (token, mid, confirm) ->
		val uid = AN.throwExpireToken(token)
		val mail = DB.throwQuerySQLSingle("SELECT * FROM mail WHERE mid = ? AND uid = ?", mid, uid)
		if (mail["processed"].Boolean) return@api "此邮件已被处理".failedData
		var ret = "已拒绝此邮件".successObject
		if (confirm) {
			val filter = mail["filter"].String
			val implFunc = implMap[filter]!!
			val args = makeObject {
				merge(mail["info"].ObjectEmpty)
				"uid" to uid
				"param1" to mail["param1"]
				"param2" to mail["param2"]
				"param3" to mail["param3"]
			}
			ret = implFunc(args)
		}
		// 处理成功后将processed置为 1
		val code = ret["code"].Int
		val msg = ret["msg"].String
		if (code == APICode.SUCCESS) {
			DB.throwExecuteSQL("UPDATE mail SET processed = 1 WHERE mid = ?", mid)
			msg.successData
		}
		else msg.failedData
	}

	// ------  删除邮件  ------
	api(API.User.Mail.DeleteMail) { (token, mid) ->
		val uid = AN.throwExpireToken(token)
		DB.throwExecuteSQL("DELETE FROM mail WHERE mid = ? AND uid = ?", mid, uid)
		"删除成功".successData
	}
}