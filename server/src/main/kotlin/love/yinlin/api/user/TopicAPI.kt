package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.data.Data

fun Routing.topicAPI(implMap: ImplMap) {
	// ------  取用户主题  ------
	api(API.User.Topic.GetTopics) { uid ->
		VN.throwId(uid)
		val topics = DB.throwQuerySQL("""
            SELECT tid, user.uid, user.name, title, pics->>'$[0]' AS pic, isTop, coinNum, commentNum
            FROM topic
            LEFT JOIN user
            ON topic.uid = user.uid
            WHERE user.uid = ? AND topic.isDeleted = 0
            ORDER BY isTop DESC, ts DESC
        """, uid)
		Data.Success(topics)
	}
}