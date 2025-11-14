package love.yinlin.api

import io.ktor.http.HttpMethod
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.route
import io.ktor.util.cio.writeChannel
import io.ktor.util.toMap
import io.ktor.utils.io.copyAndClose
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.Data
import love.yinlin.data.RequestError
import love.yinlin.extension.catchingError
import love.yinlin.extension.makeObject
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.platform.Coroutines
import love.yinlin.server.currentUniqueId
import love.yinlin.server.logger
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

val String.successObject: JsonObject get() = makeObject {
    "code" with APICode2.SUCCESS
    "msg" with this@successObject
}

val String.failedObject: JsonObject get() = makeObject {
    "code" with APICode2.FAILED
    "msg" with this@failedObject
}

val String.expireObject: JsonObject get() = makeObject {
    "code" with APICode2.UNAUTHORIZED
    "msg" with this@expireObject
}

val String.forbiddenObject: JsonObject get() = makeObject {
    "code" with APICode2.FORBIDDEN
    "msg" with this@forbiddenObject
}

val EmptySuccessData: Data.Success<Response.Default> get() = Data.Success(Response.Default)

val String.successData: Data.Success<Response.Default> get() = Data.Success(Response.Default, this)

val String.failureData: Data.Failure get() = Data.Failure(RequestError.InvalidArgument, this)

inline fun <reified Response: Any> Route.safeAPI(
    method: HttpMethod,
    path: String,
    crossinline body: suspend (RoutingCall) -> Data<Response>
): Route = route(path = path, method = method) {
    handle {
        try {
            when (val result = Coroutines.io { body(call) }) {
                is Data.Success -> call.respond(makeObject {
                    "code" with APICode2.SUCCESS
                    result.message?.let { "msg" with it }
                    "data" with result.data.toJson()
                })
                is Data.Failure -> when (result.type) {
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

fun RoutingCall.params() = makeObject {
    val map = this@params.queryParameters.toMap().mapValues { it.value.first() }
    for ((key, value) in map) key with value
}

@JvmName("apiGet")
inline fun <reified Request : Any, reified Response : Any> Route.api(
    route: APIRoute2<Request, Response, NoFiles, APIMethod2.Get>,
    crossinline body: suspend (Request) -> Data<Response>
): Route = safeAPI(HttpMethod.Get, route.toString()) { body(it.params().to()) }

@JvmName("apiGetRequest")
inline fun <reified Request : Any> Route.api(
    route: APIRoute2<Request, Response.Default, NoFiles, APIMethod2.Get>,
    crossinline body: suspend (Request) -> Data<Response.Default>
): Route = safeAPI(HttpMethod.Get, route.toString()) { body(it.params().to()) }

@JvmName("apiGetResponse")
inline fun <reified Response : Any> Route.api(
    route: APIRoute2<Request.Default, Response, NoFiles, APIMethod2.Get>,
    crossinline body: suspend () -> Data<Response>
): Route = safeAPI(HttpMethod.Get, route.toString()) { body() }

@JvmName("apiPost")
inline fun <reified Request : Any, reified Response : Any> Route.api(
    route: APIRoute2<Request, Response, NoFiles, APIMethod2.Post>,
    crossinline body: suspend (Request) -> Data<Response>
): Route = safeAPI(HttpMethod.Post, route.toString()) { body(it.receive()) }

@JvmName("apiPostRequest")
inline fun <reified Request : Any> Route.api(
    route: APIRoute2<Request, Response.Default, NoFiles, APIMethod2.Post>,
    crossinline body: suspend (Request) -> Data<Response.Default>
): Route = safeAPI(HttpMethod.Post, route.toString()) { body(it.receive()) }

@JvmName("apiPostResponse")
inline fun <reified Response : Any> Route.api(
    route: APIRoute2<Request.Default, Response, NoFiles, APIMethod2.Post>,
    crossinline body: suspend () -> Data<Response>
): Route = safeAPI(HttpMethod.Post, route.toString()) { body() }

suspend fun RoutingCall.toForm(): Pair<String?, JsonObject> {
    val multipartData = this.receiveMultipart()
    var dataString: String? = null
    val form = makeObject {
        val tmpDir = System.getProperty("java.io.tmpdir")

        val multiFiles = mutableMapOf<String, MutableList<APIFile2>>()
        var index = 0
        multipartData.forEachPart { part ->
            catchingError {
                val name: String = part.name ?: return@forEachPart
                when (part) {
                    is PartData.FormItem -> {
                        when (name) {
                            "#data#" -> dataString = part.value
                            "#ignoreFile#" -> {
                                val ignoreFile = part.value.parseJsonValue<List<String>>()
                                for (file in ignoreFile) file with null
                            }
                            "#ignoreFiles#" -> {
                                val ignoreFiles = part.value.parseJsonValue<List<String>>()
                                for (file in ignoreFiles) multiFiles[file] = mutableListOf()
                            }
                        }
                    }
                    is PartData.FileItem -> {
                        val filename = "${abs(Random.nextInt(1314520, 5201314))}-${currentUniqueId(index++)}"
                        val output = File(tmpDir, filename)
                        if (part.provider().copyAndClose(output.writeChannel()) > 0) {
                            val file: APIFile2 = output.absolutePath
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
                                    val files = mutableListOf<APIFile2>()
                                    multiFiles[newName] = files
                                    files
                                } else thisFiles
                                newFiles += file
                            }
                        }
                    }
                    else -> { }
                }
            }?.let { err ->
                logger.error("RoutingCall.toForm - {}", err.stackTraceToString())
            }
            part.dispose()
        }
        for ((filesName, files) in multiFiles) filesName with files.toJson()
    }
    return dataString to form
}

@JvmName("apiForm")
inline fun <reified Request : Any, reified Response : Any, reified Files : Any> Route.api(
    route: APIRoute2<Request, Response, Files, APIMethod2.Form>,
    crossinline body: suspend (Request, Files) -> Data<Response>
): Route = safeAPI(HttpMethod.Post, route.toString()) {
    val (dataString, files) = it.toForm()
    body(dataString!!.parseJsonValue(), files.to())
}

@JvmName("apiFormRequest")
inline fun <reified Request : Any, reified Files : Any> Route.api(
    route: APIRoute2<Request, Response.Default, Files, APIMethod2.Form>,
    crossinline body: suspend (Request, Files) -> Data<Response.Default>
): Route = safeAPI(HttpMethod.Post, route.toString()) {
    val (dataString, files) = it.toForm()
    body(dataString!!.parseJsonValue(), files.to())
}

@JvmName("apiFormResponse")
inline fun <reified Response : Any, reified Files : Any> Route.api(
    route: APIRoute2<Request.Default, Response, Files, APIMethod2.Form>,
    crossinline body: suspend (Files) -> Data<Response>
): Route = safeAPI(HttpMethod.Post, route.toString()) {
    val (_, files) = it.toForm()
    body(files.to())
}