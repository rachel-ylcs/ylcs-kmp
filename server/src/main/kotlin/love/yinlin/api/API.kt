package love.yinlin.api

import io.ktor.http.HttpMethod
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.RoutingHandler
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import love.yinlin.extension.makeObject
import love.yinlin.logger

typealias ImplFunc = suspend RoutingContext.(Map<String, Any?>) -> Map<String, Any?>
typealias ImplMap = MutableMap<String, ImplFunc>

object Code {
	const val SUCCESS = 0
	const val FORBIDDEN = 1
	const val UNAUTHORIZED = 2
	const val FAILED = 3
}

class TokenExpireError(uid: Int) : Throwable() {
	override val message: String? = "TokenExpireError $uid"
}

// Request

suspend inline fun <reified T : Any> RoutingCall.to(): T = this.receive<T>()

// Response

suspend fun RoutingCall.data(value: JsonElement) = this.respond(value)
suspend fun RoutingCall.success() = this.respond(makeObject {
	"code" to Code.SUCCESS
})
suspend fun RoutingCall.success(msg: String) = this.respond(makeObject {
	"code" to Code.SUCCESS
	"msg" to msg
})
suspend fun RoutingCall.success(data: JsonElement) = this.respond(makeObject {
	"code" to Code.SUCCESS
	"data" to data
})
suspend fun RoutingCall.success(msg: String, data: JsonElement) = this.respond(makeObject {
	"code" to Code.SUCCESS
	"msg" to msg
	"data" to data
})
suspend fun RoutingCall.expire() = this.respond(makeObject {
	"code" to Code.UNAUTHORIZED
	"msg" to "登录信息已过期"
})
suspend fun RoutingCall.die(err: Throwable) {
	logger.error("CallDie - {}", err.stackTraceToString())
	this.respond(makeObject {
		"code" to Code.FORBIDDEN
		"msg" to "网络异常"
	})
}

inline fun Route.catchAsyncRequest(path: String, method: HttpMethod, crossinline body: RoutingHandler): Route {
	return route(path, method) {
		handle {
			try {
				withContext(Dispatchers.IO) {
					body()
				}
			}
			catch (_: TokenExpireError) { call.expire() }
			catch (err: Throwable) { call.die(err) }
		}
	}
}

inline fun Route.catchAsyncGet(path: String, crossinline body: RoutingHandler): Route = catchAsyncRequest(path, HttpMethod.Get, body)
inline fun Route.catchAsyncPost(path: String, crossinline body: RoutingHandler): Route = catchAsyncRequest(path, HttpMethod.Post, body)