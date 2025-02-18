package love.yinlin.api

import io.ktor.server.routing.Routing

fun Routing.testAPI(implMap: ImplMap) {
	catchAsyncGet("/test") {
		call.success()
	}

	catchAsyncPost("/test") {
		val form = call.toForm()
		for ((key, value) in form) {
			println("key: $key, value: ${
				when (value) {
					is FormValue.Binary -> value.data.joinToString(",")
					is FormValue.File -> value
					is FormValue.Text -> value
				}
			}")
		}
		call.success()
	}
}