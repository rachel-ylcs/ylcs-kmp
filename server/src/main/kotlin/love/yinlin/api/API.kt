package love.yinlin.api

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import love.yinlin.currentUniqueId
import love.yinlin.extension.Json
import love.yinlin.extension.makeObject
import love.yinlin.logger
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.math.abs
import kotlin.random.Random

typealias ImplFunc = suspend RoutingContext.(JsonObject) -> JsonObject
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

inline fun <reified T> RoutingCall.params(): T {
	val map = this.queryParameters.toMap().mapValues { it.value.first() }
	val json = makeObject {
		for ((key, value) in map) key to value
	}
	return Json.decodeFromJsonElement(json)
}

suspend inline fun <reified T : Any> RoutingCall.to(): T = this.receive<T>()

suspend fun RoutingCall.toForm(): Map<String, String> {
	val map = mutableMapOf<String, String>()
	var index = 0
	val multipartData = this.receiveMultipart()
	multipartData.forEachPart { part ->
		try {
			val name: String = part.name ?: return@forEachPart
			when (part) {
				is PartData.FormItem -> map[name] = part.value
				is PartData.FileItem -> {
					val filename = "${abs(Random.nextInt(1314520, 5201314))}-${currentUniqueId(index++)}"
					val output = File(System.getProperty("java.io.tmpdir"), filename)
					if (part.provider().copyAndClose(output.writeChannel()) > 0)
						map[name] = output.absolutePath
				}
				is PartData.BinaryItem -> map[name] = part.provider().readByteArray().toString(Charsets.UTF_8)
				else -> { }
			}
		}
		catch (err: Throwable) {
			logger.error("RoutingCall.toForm - {}", err.stackTraceToString())
		}
		finally {
			part.dispose()
		}
	}
	return map
}

suspend inline fun <reified T> RoutingCall.toFormObject(): T {
	val form = this.toForm()
	val json = makeObject {
		for ((key, value) in form) key to value
	}
	return Json.decodeFromJsonElement(json)
}

// Response

val String.successObject: JsonObject get() = makeObject {
	"code" to Code.SUCCESS
	"msg" to this
}
val String.failedObject: JsonObject get() = makeObject {
	"code" to Code.FAILED
	"msg" to this
}

suspend fun RoutingCall.data(value: JsonElement) = this.respond(value)
suspend fun RoutingCall.success() = this.respond(makeObject {
	"code" to Code.SUCCESS
})
suspend fun RoutingCall.success(msg: String) = this.respond(msg.successObject)
suspend fun RoutingCall.success(data: JsonElement) = this.respond(makeObject {
	"code" to Code.SUCCESS
	"data" to data
})
suspend fun RoutingCall.success(msg: String, data: JsonElement) = this.respond(makeObject {
	"code" to Code.SUCCESS
	"msg" to msg
	"data" to data
})
suspend fun RoutingCall.failed(msg: String) = this.respond(msg.failedObject)
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