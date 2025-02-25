package love.yinlin.api.test

import io.ktor.server.routing.Routing
import love.yinlin.api.API
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.successData

fun Routing.testAPI(implMap: ImplMap) {
	api(API.Test.Get) {
		"".successData
	}

	api(API.Test.Post) {
		"${it}".successData
	}
}