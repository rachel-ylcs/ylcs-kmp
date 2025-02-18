package love.yinlin.api

import io.ktor.server.routing.Routing

fun Routing.testAPI(implMap: ImplMap) {
	catchAsyncGet("/test") {
		call.success()
	}

	catchAsyncPost("/test") {
		call.success()
	}
}