package love.yinlin.api.test

import io.ktor.server.routing.Routing
import love.yinlin.api.API2
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.successData

fun Routing.testAPI(implMap: ImplMap) {
	api(API2.Test.Get) {
		"".successData
	}

	api(API2.Test.Post) { (a, b), (c) ->
		"$a $b $c".successData
	}
}