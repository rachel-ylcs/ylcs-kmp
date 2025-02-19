package love.yinlin.api

import io.ktor.server.routing.Routing
import love.yinlin.data.Data

fun Routing.testAPI(implMap: ImplMap) {
	api(API.Test.Get) {
		Data.Success(it.toString())
	}

	api(API.Test.Post) {
		Data.Success(it.toString())
	}
}