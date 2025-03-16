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
import kotlinx.serialization.json.JsonObject
import love.yinlin.api.common.commonAPI
import love.yinlin.api.test.testAPI
import love.yinlin.api.user.userAPI
import love.yinlin.copy
import love.yinlin.currentUniqueId
import love.yinlin.data.Data
import love.yinlin.data.Failed
import love.yinlin.data.rachel.MailEntry
import love.yinlin.extension.*
import love.yinlin.logger
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

typealias ImplFunc = suspend (MailEntry) -> JsonObject
typealias ImplMap = MutableMap<String, ImplFunc>

class TokenExpireError(uid: Int) : Throwable() {
	override val message: String = "TokenExpireError $uid"
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

val EmptySuccessData: Data.Success<Response.Default> get() = Data.Success(Response.Default)

val String.successData: Data.Success<Response.Default> get() = Data.Success(Response.Default, this)

val String.failedData: Data.Error get() = Data.Error(Failed.RequestError.InvalidArgument, this)

class NineGridProcessor(pics: APIFiles?) {
	val sourcePics = pics ?: emptyList()
	val actualPics: List<String>
	val jsonString: String

	init {
		if (sourcePics.size > 9) throw error("NineGrid num error")
		actualPics = List(sourcePics.size) { currentUniqueId(it) }
		jsonString = actualPics.toJsonString()
	}

	inline fun copy(callback: (String) -> ResNode): String? {
		repeat(actualPics.size) {
			sourcePics[it].copy(callback(actualPics[it]))
		}
		return actualPics.firstOrNull()
	}
}

inline fun <reified Response: Any> Route.safeAPI(
	method: HttpMethod,
	path: String,
	crossinline body: suspend (RoutingCall) -> Data<Response>
): Route = route(path = path, method = method) {
	handle {
		try {
			val result = withContext(Dispatchers.IO) { body(call) }
			when (result) {
				is Data.Success -> call.respond(makeObject {
					"code" with APICode.SUCCESS
					result.message?.let { "msg" with it }
					"data" with result.data.toJson()
				})
				is Data.Error -> when (result.type) {
					Failed.RequestError.ClientError -> call.respond("客户端错误: ${result.message}".failedObject)
					Failed.RequestError.Timeout -> call.respond("网络连接超时".failedObject)
					Failed.RequestError.Canceled -> call.respond("操作取消".failedObject)
					Failed.RequestError.Unauthorized -> call.respond("登录信息已过期".expireObject)
					Failed.RequestError.InvalidArgument -> call.respond("${result.message}".failedObject)
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

fun RoutingCall.params() = makeObject {
	val map = this@params.queryParameters.toMap().mapValues { it.value.first() }
	for ((key, value) in map) key with value
}

@JvmName("apiGet")
inline fun <reified Request : Any, reified Response : Any> Route.api(
	route: APIRoute<Request, Response, NoFiles, APIMethod.Get>,
	crossinline body: suspend (Request) -> Data<Response>
): Route = safeAPI(HttpMethod.Get, route.toString()) { body(it.params().to()) }

@JvmName("apiGetRequest")
inline fun <reified Request : Any> Route.api(
	route: APIRoute<Request, Response.Default, NoFiles, APIMethod.Get>,
	crossinline body: suspend (Request) -> Data<Response.Default>
): Route = safeAPI(HttpMethod.Get, route.toString()) { body(it.params().to()) }

@JvmName("apiGetResponse")
inline fun <reified Response : Any> Route.api(
	route: APIRoute<Request.Default, Response, NoFiles, APIMethod.Get>,
	crossinline body: suspend () -> Data<Response>
): Route = safeAPI(HttpMethod.Get, route.toString()) { body() }

@JvmName("apiPost")
inline fun <reified Request : Any, reified Response : Any> Route.api(
	route: APIRoute<Request, Response, NoFiles, APIMethod.Post>,
	crossinline body: suspend (Request) -> Data<Response>
): Route = safeAPI(HttpMethod.Post, route.toString()) { body(it.receive()) }

@JvmName("apiPostRequest")
inline fun <reified Request : Any> Route.api(
	route: APIRoute<Request, Response.Default, NoFiles, APIMethod.Post>,
	crossinline body: suspend (Request) -> Data<Response.Default>
): Route = safeAPI(HttpMethod.Post, route.toString()) { body(it.receive()) }

@JvmName("apiPostResponse")
inline fun <reified Response : Any> Route.api(
	route: APIRoute<Request.Default, Response, NoFiles, APIMethod.Post>,
	crossinline body: suspend () -> Data<Response>
): Route = safeAPI(HttpMethod.Post, route.toString()) { body() }

suspend fun RoutingCall.toForm(): Pair<String?, JsonObject> {
	val multipartData = this.receiveMultipart()
	var dataString: String? = null
	val form = makeObject {
		val tmpDir = System.getProperty("java.io.tmpdir")

		val multiFiles = mutableMapOf<String, MutableList<APIFile>>()
		var index = 0
		multipartData.forEachPart { part ->
			try {
				val name: String = part.name ?: return@forEachPart
				when (part) {
					is PartData.FormItem -> {
						if (name == "#data#") dataString = part.value
						else if (name == "#ignoreFiles#") {
							val ignoreFiles = part.value.parseJsonValue<List<String>>()!!
							for (ignoreFile in ignoreFiles) ignoreFile with null
						}
					}
					is PartData.FileItem -> {
						val filename = "${abs(Random.nextInt(1314520, 5201314))}-${currentUniqueId(index++)}"
						val output = File(tmpDir, filename)
						if (part.provider().copyAndClose(output.writeChannel()) > 0) {
							val file = APIFile(output.absolutePath)
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
									val files = mutableListOf<APIFile>()
									multiFiles[newName] = files
									files
								} else thisFiles
								newFiles += file
							}
						}
					}
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
	return dataString to form
}

@JvmName("apiForm")
inline fun <reified Request : Any, reified Response : Any, reified Files : Any> Route.api(
	route: APIRoute<Request, Response, Files, APIMethod.Form>,
	crossinline body: suspend (Request, Files) -> Data<Response>
): Route = safeAPI(HttpMethod.Post, route.toString()) {
	val (dataString, files) = it.toForm()
	body(dataString.parseJsonValue()!!, files.to())
}

@JvmName("apiFormRequest")
inline fun <reified Request : Any, reified Files : Any> Route.api(
	route: APIRoute<Request, Response.Default, Files, APIMethod.Form>,
	crossinline body: suspend (Request, Files) -> Data<Response.Default>
): Route = safeAPI(HttpMethod.Post, route.toString()) {
	val (dataString, files) = it.toForm()
	body(dataString.parseJsonValue()!!, files.to())
}

@JvmName("apiFormResponse")
inline fun <reified Response : Any, reified Files : Any> Route.api(
	route: APIRoute<Request.Default, Response, Files, APIMethod.Form>,
	crossinline body: suspend (Files) -> Data<Response>
): Route = safeAPI(HttpMethod.Post, route.toString()) {
	val (_, files) = it.toForm()
	body(files.to())
}

fun Routing.initAPI() {
	val implMap = mutableMapOf<String, ImplFunc>()
	commonAPI(implMap)
	userAPI(implMap)
	testAPI(implMap)
}