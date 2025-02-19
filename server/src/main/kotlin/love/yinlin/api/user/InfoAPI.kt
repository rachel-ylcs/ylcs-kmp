package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failedData
import love.yinlin.api.successData

fun Routing.infoAPI(implMap: ImplMap) {
	// ------  用户反馈  ------
	api(API.User.Info.SendFeedback) { (token, content) ->
		val uid = AN.throwExpireToken(token)
		if (DB.throwInsertSQLDuplicateKey("INSERT INTO feedback(uid, content) VALUES(?, ?)", uid, content))
			"您已提交过反馈, 请耐心等待处理".failedData
		else "提交反馈成功".successData
	}
}