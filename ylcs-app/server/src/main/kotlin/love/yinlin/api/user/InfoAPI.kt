package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.api.API2
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failureData
import love.yinlin.api.successData
import love.yinlin.server.DB
import love.yinlin.server.values

fun Routing.infoAPI(implMap: ImplMap) {
	api(API2.User.Info.SendFeedback) { (token, content) ->
		val uid = AN.throwExpireToken(token)
		if (DB.throwInsertSQLDuplicateKey("INSERT INTO feedback(uid, content) ${values(2)}", uid, content))
			"您已提交过反馈, 请耐心等待处理".failureData
		else "提交反馈成功".successData
	}
}