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
import kotlinx.serialization.json.JsonObject
import love.yinlin.api.common.commonAPI
import love.yinlin.api.test.testAPI
import love.yinlin.api.user.userAPI
import love.yinlin.copy
import love.yinlin.currentUniqueId
import love.yinlin.data.Data
import love.yinlin.data.RequestError
import love.yinlin.extension.makeObject
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.extension.toJsonString
import love.yinlin.logger
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.math.abs
import kotlin.random.Random

typealias ImplFunc = suspend (JsonObject) -> JsonObject
typealias ImplMap = MutableMap<String, ImplFunc>

// Request

inline fun <reified T> RoutingCall.params(): T {
	val map = this.queryParameters.toMap().mapValues { it.value.first() }
	return makeObject {
		for ((key, value) in map) key with value
	}.to()
}

suspend inline fun <reified T : Any> RoutingCall.to(): T = this.receive<T>()

suspend inline fun <reified T> RoutingCall.toForm(): T {
	val multiFiles = mutableMapOf<String, MutableList<ClientFile>>()
	val multipartData = this@toForm.receiveMultipart()
	var index = 0
	val form = makeObject {
		multipartData.forEachPart { part ->
			try {
				val name: String = part.name ?: return@forEachPart
				when (part) {
					is PartData.FormItem -> name with part.value
					is PartData.FileItem -> {
						val filename = "${abs(Random.nextInt(1314520, 5201314))}-${currentUniqueId(index++)}"
						val output = File(System.getProperty("java.io.tmpdir"), filename)
						if (part.provider().copyAndClose(output.writeChannel()) > 0) {
							val file = ClientFile(output.absolutePath)
							val newName = if (name.startsWith('#') && name.contains('!')) {
								val fetchName = name.substringAfter('#').substringBeforeLast('!')
								val fetchIndex = name.substringAfter('!')
								if (fetchName.isNotEmpty() && fetchIndex.isNotEmpty()) fetchName
								else name
							}
							else name
							if (newName == name) newName with file.toJson()
							else {
								val thisFiles = multiFiles[newName]
								val newFiles = if (thisFiles == null) {
									val files = mutableListOf<ClientFile>()
									multiFiles[newName] = files
									files
								} else thisFiles
								newFiles += file
							}
						}
					}
					is PartData.BinaryItem -> name with part.provider().readByteArray().toString(Charsets.UTF_8)
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
		for ((filesName, files) in multiFiles) filesName with files.toJson()
	}
	return form.to()
}

class NineGridProcessor(private val pics: List<ClientFile>) {
	private val actualPics: List<String>

	init {
		if (pics.size > 9) throw error("NineGrid num error")
		actualPics = List(pics.size) { currentUniqueId(it) }
	}

	val jsonString: String get() = actualPics.toJsonString()

	fun copy(callback: (String) -> ResNode): String? {
		repeat(actualPics.size) {
			pics[it].copy(callback(actualPics[it]))
		}
		return actualPics.firstOrNull()
	}
}

// Response

class TokenExpireError(val uid: Int) : Throwable() {
	override val message: String get() = "TokenExpireError $uid"
}

val String.successObject: JsonObject get() = makeObject {
	"code" with APICode.SUCCESS
	"msg" with this@successObject
}
val String.failedObject: JsonObject get() = makeObject {
	"code" with APICode.FAILED
	"msg" with this@failedObject
}
val String.expireObject: JsonObject get() = makeObject {
	"code" with APICode.UNAUTHORIZED
	"msg" with this@expireObject
}
val String.forbiddenObject: JsonObject get() = makeObject {
	"code" with APICode.FORBIDDEN
	"msg" with this@forbiddenObject
}
val EmptySuccessData: Data.Success<Default.Response> get() = Data.Success(Default.Response)
val String.successData: Data.Success<Default.Response> get() = Data.Success(Default.Response, this)
val String.failedData: Data.Error get() = Data.Error(RequestError.InvalidArgument, this)

inline fun <reified Request : Any, reified Response : Any> Route.api(
	route: APIRoute<Request, Response>,
	crossinline body: suspend (Request) -> Data<Response>
): Route = route(path = route.path, method = when (route.method) {
	APIMethod.Get -> HttpMethod.Get
	APIMethod.Post -> HttpMethod.Post
	APIMethod.Form -> HttpMethod.Post
}) {
	handle {
		try {
			val result = withContext(Dispatchers.IO) {
				val requestData: Request = when (route.method) {
					APIMethod.Get -> call.params()
					APIMethod.Post -> call.to()
					APIMethod.Form -> call.toForm()
				}
				body(requestData)
			}
			when (result) {
				is Data.Success -> call.respond(makeObject {
					"code" with APICode.SUCCESS
					result.message?.let { "msg" with it }
					if (result.data !is Default.Response) {
						"data" with result.data.toJson()
					}
				})
				is Data.Error -> when (result.type) {
					RequestError.ClientError -> call.respond("客户端错误: ${result.message}".failedObject)
					RequestError.Timeout -> call.respond("网络连接超时".failedObject)
					RequestError.Canceled -> call.respond("操作取消".failedObject)
					RequestError.Unauthorized -> call.respond("登录信息已过期".expireObject)
					RequestError.InvalidArgument -> call.respond("${result.message}".failedObject)
					else -> call.respond("未知错误: ${result.message}".failedObject)
				}
			}
		}
		catch (_: TokenExpireError) {
			call.respond("登录信息已过期".expireObject)
		}
		catch (err: Throwable) {
			logger.error("CallDie - {}", err.stackTraceToString())
			call.respond("非法操作".forbiddenObject)
		}
	}
}

fun Routing.initAPI() {
	val implMap = mutableMapOf<String, ImplFunc>()
	commonAPI(implMap)
	userAPI(implMap)
	testAPI(implMap)
}