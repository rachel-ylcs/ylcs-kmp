package love.yinlin.cs

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import love.yinlin.extension.makeArray
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.then
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File
import love.yinlin.fs.PlatformFileSystem
import kotlin.random.Random

abstract class APIScope(val engine: ServerEngine) {
    /**
     * 日志输出器
     */
    val logger: ServerLogger = engine.logger

    /**
     * 接口
     */
    protected abstract fun api()

    /**
     * 服务
     */
    open val services: List<ServerService> = []

    private var routing: Routing? = null

    internal fun setupAPI(routing: Routing) {
        this.routing = routing
        api()
    }

    // 接口请求

    fun API<out APIType>.internalResponse(block: suspend (RoutingCall) -> JsonElement) {
        routing?.route(path = route, method = HttpMethod.Post) {
            handle {
                catchingError {
                    Coroutines.io {
                        call.respond(status = HttpStatusCode.OK, message = block(call))
                    }
                }?.then { err ->
                    logger.error("CallError: ${call.request.uri}", err)
                    when (err) {
                        is UnauthorizedException -> call.respond(status = HttpStatusCode.Unauthorized, message = makeArray { })
                        is FailureException -> call.respondText(status = HttpStatusCode.Accepted, text = err.message ?: "Unknown Error")
                        else -> call.respond(status = HttpStatusCode.Forbidden, message = makeArray { })
                    }
                }
            }
        }
    }

    // Post

    inline fun API<out APIType>.internalResponsePost(crossinline block: suspend (JsonArray) -> JsonElement) {
        internalResponse {
            block(it.receive<JsonArray>())
        }
    }

    inline fun API00<APIType.Post>
            .response(crossinline block: suspend APIResponseScope.() -> Unit) = internalResponsePost {
        APIResponseScope().block()
        makeArray {

        }
    }

    inline fun <reified I1> API10<APIType.Post, I1>
            .response(crossinline block: suspend APIResponseScope.(I1) -> Unit) = internalResponsePost {
        val [i1] = it
        APIResponseScope().block(i1.to())
        makeArray {

        }
    }

    inline fun <reified I1, reified I2> API20<APIType.Post, I1, I2>
            .response(crossinline block: suspend APIResponseScope.(I1, I2) -> Unit) = internalResponsePost {
        val [i1, i2] = it
        APIResponseScope().block(i1.to(), i2.to())
        makeArray {

        }
    }

    inline fun <reified I1, reified I2, reified I3> API30<APIType.Post, I1, I2, I3>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3) -> Unit) = internalResponsePost {
        val [i1, i2, i3] = it
        APIResponseScope().block(i1.to(), i2.to(), i3.to())
        makeArray {

        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4> API40<APIType.Post, I1, I2, I3, I4>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3, I4) -> Unit) = internalResponsePost {
        val [i1, i2, i3, i4] = it
        APIResponseScope().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {

        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5> API50<APIType.Post, I1, I2, I3, I4, I5>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3, I4, I5) -> Unit) = internalResponsePost {
        val [i1, i2, i3, i4, i5] = it
        APIResponseScope().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {

        }
    }

    inline fun <reified O1> API01<APIType.Post, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.() -> APIResult1<O1>) = internalResponsePost {
        val (o1) = APIResultScope1<O1>().block()
        makeArray {
            add(o1.toJson())
        }
    }

    inline fun <reified I1, reified O1> API11<APIType.Post, I1, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1) -> APIResult1<O1>) = internalResponsePost {
        val [i1] = it
        val (o1) = APIResultScope1<O1>().block(i1.to())
        makeArray {
            add(o1.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified O1> API21<APIType.Post, I1, I2, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2) -> APIResult1<O1>) = internalResponsePost {
        val [i1, i2] = it
        val (o1) = APIResultScope1<O1>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified O1> API31<APIType.Post, I1, I2, I3, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3) -> APIResult1<O1>) = internalResponsePost {
        val [i1, i2, i3] = it
        val (o1) = APIResultScope1<O1>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1> API41<APIType.Post, I1, I2, I3, I4, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3, I4) -> APIResult1<O1>) = internalResponsePost {
        val [i1, i2, i3, i4] = it
        val (o1) = APIResultScope1<O1>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1> API51<APIType.Post, I1, I2, I3, I4, I5, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3, I4, I5) -> APIResult1<O1>) = internalResponsePost {
        val [i1, i2, i3, i4, i5] = it
        val (o1) = APIResultScope1<O1>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
        }
    }

    inline fun <reified O1, reified O2> API02<APIType.Post, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.() -> APIResult2<O1, O2>) = internalResponsePost {
        val (o1, o2) = APIResultScope2<O1, O2>().block()
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    inline fun <reified I1, reified O1, reified O2> API12<APIType.Post, I1, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1) -> APIResult2<O1, O2>) = internalResponsePost {
        val [i1] = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified O1, reified O2> API22<APIType.Post, I1, I2, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2) -> APIResult2<O1, O2>) = internalResponsePost {
        val [i1, i2] = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2> API32<APIType.Post, I1, I2, I3, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3) -> APIResult2<O1, O2>) = internalResponsePost {
        val [i1, i2, i3] = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2> API42<APIType.Post, I1, I2, I3, I4, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3, I4) -> APIResult2<O1, O2>) = internalResponsePost {
        val [i1, i2, i3, i4] = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2> API52<APIType.Post, I1, I2, I3, I4, I5, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3, I4, I5) -> APIResult2<O1, O2>) = internalResponsePost {
        val [i1, i2, i3, i4, i5] = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    inline fun <reified O1, reified O2, reified O3> API03<APIType.Post, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.() -> APIResult3<O1, O2, O3>) = internalResponsePost {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block()
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    inline fun <reified I1, reified O1, reified O2, reified O3> API13<APIType.Post, I1, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1) -> APIResult3<O1, O2, O3>) = internalResponsePost {
        val [i1] = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3> API23<APIType.Post, I1, I2, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2) -> APIResult3<O1, O2, O3>) = internalResponsePost {
        val [i1, i2] = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3> API33<APIType.Post, I1, I2, I3, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3) -> APIResult3<O1, O2, O3>) = internalResponsePost {
        val [i1, i2, i3] = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3> API43<APIType.Post, I1, I2, I3, I4, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3, I4) -> APIResult3<O1, O2, O3>) = internalResponsePost {
        val [i1, i2, i3, i4] = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3> API53<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3, I4, I5) -> APIResult3<O1, O2, O3>) = internalResponsePost {
        val [i1, i2, i3, i4, i5] = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    inline fun <reified O1, reified O2, reified O3, reified O4> API04<APIType.Post, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.() -> APIResult4<O1, O2, O3, O4>) = internalResponsePost {
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block()
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    inline fun <reified I1, reified O1, reified O2, reified O3, reified O4> API14<APIType.Post, I1, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1) -> APIResult4<O1, O2, O3, O4>) = internalResponsePost {
        val [i1] = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4> API24<APIType.Post, I1, I2, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2) -> APIResult4<O1, O2, O3, O4>) = internalResponsePost {
        val [i1, i2] = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4> API34<APIType.Post, I1, I2, I3, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2, I3) -> APIResult4<O1, O2, O3, O4>) = internalResponsePost {
        val [i1, i2, i3] = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4> API44<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2, I3, I4) -> APIResult4<O1, O2, O3, O4>) = internalResponsePost {
        val [i1, i2, i3, i4] = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4> API54<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2, I3, I4, I5) -> APIResult4<O1, O2, O3, O4>) = internalResponsePost {
        val [i1, i2, i3, i4, i5] = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    inline fun <reified O1, reified O2, reified O3, reified O4, reified O5> API05<APIType.Post, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.() -> APIResult5<O1, O2, O3, O4, O5>) = internalResponsePost {
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block()
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    inline fun <reified I1, reified O1, reified O2, reified O3, reified O4, reified O5> API15<APIType.Post, I1, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponsePost {
        val [i1] = it
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(i1.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4, reified O5> API25<APIType.Post, I1, I2, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponsePost {
        val [i1, i2] = it
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4, reified O5> API35<APIType.Post, I1, I2, I3, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2, I3) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponsePost {
        val [i1, i2, i3] = it
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4, reified O5> API45<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2, I3, I4) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponsePost {
        val [i1, i2, i3, i4] = it
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4, reified O5> API55<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2, I3, I4, I5) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponsePost {
        val [i1, i2, i3, i4, i5] = it
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    // Form

    class FormResult(private val multipartData: MultiPartData) : AutoCloseable {
        // TODO: 后期考虑自身目录，因为非root写tmp可能会存在权限问题
        private val tempDir = PlatformFileSystem.cachePath(PlatformContext.Instance, "ServerNative")
        private val tempFiles: MutableList<File> = []
        @PublishedApi
        internal var index = 0
        @PublishedApi
        internal var dataItems: Map<Int, JsonElement> = emptyMap()
        @PublishedApi
        internal var fileItems: Map<Int, APIFile> = emptyMap()

        suspend fun parse() {
            val dataItems = mutableMapOf<Int, JsonElement>()
            val fileItems = mutableMapOf<Int, MutableList<String>>()
            multipartData.forEachPart { part ->
                cleaning({
                    val name = requireNotNull(part.name) { "part.name == null" }
                    val parameterIndex = requireNotNull(name.toIntOrNull()) { "Invalid form key: $name" }
                    require(parameterIndex >= 0 && name == parameterIndex.toString()) { "Invalid form key: $name" }

                    when (part) {
                        is PartData.FormItem -> {
                            require(parameterIndex !in dataItems && parameterIndex !in fileItems) { "Duplicate or mixed form parameter $parameterIndex" }
                            dataItems[parameterIndex] = part.value.parseJsonValue<JsonElement>()
                        }
                        is PartData.FileItem -> {
                            require(parameterIndex !in dataItems) { "Mixed JSON and file parameter $parameterIndex" }
                            val paths = fileItems.getOrPut(parameterIndex, ::mutableListOf)
                            val fileIndex = paths.size
                            val tempFilename = "${Random.nextInt(1314520, 5201314)}-$parameterIndex-${DateEx.uniqueTimeId(fileIndex)}"

                            if (!tempDir.exists()) tempDir.mkdir()
                            val tempFile = File(tempDir, tempFilename)
                            tempFiles += tempFile

                            val size = tempFile.bufferedSink().use { sink ->
                                part.provider().copyAndClose(sink.asByteWriteChannel())
                            }
                            require(size > 0L) { "Empty upload file at parameter $parameterIndex, file $fileIndex" }
                            paths += tempFile.path
                        }
                        else -> error("Unsupported form part at parameter $parameterIndex")
                    }
                    currentCoroutineContext().ensureActive()
                }) {
                    part.release()
                }
            }

            val parameterCount = dataItems.size + fileItems.size
            require((0 ..< parameterCount).all { it in dataItems || it in fileItems }) { "Form parameter indices must start at 0 and be continuous" }

            this.dataItems = dataItems.toMap()
            this.fileItems = fileItems.mapValues { [_, paths] -> ServerAPIFile(paths.toList()) }
        }

        inline operator fun <reified I> invoke(): I {
            val parameterIndex = index++
            if (I::class == APIFile::class) {
                fileItems[parameterIndex]?.let { return it as I }
                require(dataItems[parameterIndex] == JsonNull) { "Expected a file or null at parameter $parameterIndex" }
                require(null is I) { "File parameter $parameterIndex is not nullable" }
                return null as I
            }

            require(parameterIndex !in fileItems) { "Expected JSON at parameter $parameterIndex" }

            return requireNotNull(dataItems[parameterIndex]) { "Missing form parameter $parameterIndex"}.to()
        }

        override fun close() {
            var failure: Throwable? = null
            for (file in tempFiles) {
                catchingError {
                    if (file.existsSync()) file.deleteSync()
                }?.let { err ->
                    val previous = failure
                    if (previous == null) failure = err
                    else previous.addSuppressed(err)
                }
            }
            tempFiles.clear()
            failure?.let { throw it }
        }
    }

    inline fun API<out APIType>.internalResponseForm(crossinline block: suspend (FormResult) -> JsonElement) {
        internalResponse {
            FormResult(it.receiveMultipart()).use { formResult ->
                formResult.parse()
                block(formResult)
            }
        }
    }

    inline fun <reified I1> API10<APIType.Form, I1>
            .response(crossinline block: suspend APIResponseScope.(I1) -> Unit) = internalResponseForm {
        APIResponseScope().block(it())
        makeArray {

        }
    }

    inline fun <reified I1, reified I2> API20<APIType.Form, I1, I2>
            .response(crossinline block: suspend APIResponseScope.(I1, I2) -> Unit) = internalResponseForm {
        APIResponseScope().block(it(), it())
        makeArray {

        }
    }

    inline fun <reified I1, reified I2, reified I3> API30<APIType.Form, I1, I2, I3>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3) -> Unit) = internalResponseForm {
        APIResponseScope().block(it(), it(), it())
        makeArray {

        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4> API40<APIType.Form, I1, I2, I3, I4>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3, I4) -> Unit) = internalResponseForm {
        APIResponseScope().block(it(), it(), it(), it())
        makeArray {

        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5> API50<APIType.Form, I1, I2, I3, I4, I5>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3, I4, I5) -> Unit) = internalResponseForm {
        APIResponseScope().block(it(), it(), it(), it(), it())
        makeArray {

        }
    }

    inline fun <reified I1, reified O1> API11<APIType.Form, I1, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1) -> APIResult1<O1>) = internalResponseForm {
        val (o1) = APIResultScope1<O1>().block(it())
        makeArray {
            add(o1.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified O1> API21<APIType.Form, I1, I2, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2) -> APIResult1<O1>) = internalResponseForm {
        val (o1) = APIResultScope1<O1>().block(it(), it())
        makeArray {
            add(o1.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified O1> API31<APIType.Form, I1, I2, I3, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3) -> APIResult1<O1>) = internalResponseForm {
        val (o1) = APIResultScope1<O1>().block(it(), it(), it())
        makeArray {
            add(o1.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1> API41<APIType.Form, I1, I2, I3, I4, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3, I4) -> APIResult1<O1>) = internalResponseForm {
        val (o1) = APIResultScope1<O1>().block(it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1> API51<APIType.Form, I1, I2, I3, I4, I5, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3, I4, I5) -> APIResult1<O1>) = internalResponseForm {
        val (o1) = APIResultScope1<O1>().block(it(), it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
        }
    }

    inline fun <reified I1, reified O1, reified O2> API12<APIType.Form, I1, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1) -> APIResult2<O1, O2>) = internalResponseForm {
        val (o1, o2) = APIResultScope2<O1, O2>().block(it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified O1, reified O2> API22<APIType.Form, I1, I2, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2) -> APIResult2<O1, O2>) = internalResponseForm {
        val (o1, o2) = APIResultScope2<O1, O2>().block(it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2> API32<APIType.Form, I1, I2, I3, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3) -> APIResult2<O1, O2>) = internalResponseForm {
        val (o1, o2) = APIResultScope2<O1, O2>().block(it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2> API42<APIType.Form, I1, I2, I3, I4, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3, I4) -> APIResult2<O1, O2>) = internalResponseForm {
        val (o1, o2) = APIResultScope2<O1, O2>().block(it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2> API52<APIType.Form, I1, I2, I3, I4, I5, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3, I4, I5) -> APIResult2<O1, O2>) = internalResponseForm {
        val (o1, o2) = APIResultScope2<O1, O2>().block(it(), it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    inline fun <reified I1, reified O1, reified O2, reified O3> API13<APIType.Form, I1, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1) -> APIResult3<O1, O2, O3>) = internalResponseForm {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3> API23<APIType.Form, I1, I2, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2) -> APIResult3<O1, O2, O3>) = internalResponseForm {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3> API33<APIType.Form, I1, I2, I3, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3) -> APIResult3<O1, O2, O3>) = internalResponseForm {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3> API43<APIType.Form, I1, I2, I3, I4, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3, I4) -> APIResult3<O1, O2, O3>) = internalResponseForm {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3> API53<APIType.Form, I1, I2, I3, I4, I5, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3, I4, I5) -> APIResult3<O1, O2, O3>) = internalResponseForm {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(it(), it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    inline fun <reified I1, reified O1, reified O2, reified O3, reified O4> API14<APIType.Form, I1, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1) -> APIResult4<O1, O2, O3, O4>) = internalResponseForm {
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4> API24<APIType.Form, I1, I2, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2) -> APIResult4<O1, O2, O3, O4>) = internalResponseForm {
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4> API34<APIType.Form, I1, I2, I3, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2, I3) -> APIResult4<O1, O2, O3, O4>) = internalResponseForm {
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4> API44<APIType.Form, I1, I2, I3, I4, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2, I3, I4) -> APIResult4<O1, O2, O3, O4>) = internalResponseForm {
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4> API54<APIType.Form, I1, I2, I3, I4, I5, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2, I3, I4, I5) -> APIResult4<O1, O2, O3, O4>) = internalResponseForm {
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(it(), it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    inline fun <reified I1, reified O1, reified O2, reified O3, reified O4, reified O5> API15<APIType.Form, I1, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponseForm {
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4, reified O5> API25<APIType.Form, I1, I2, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponseForm {
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4, reified O5> API35<APIType.Form, I1, I2, I3, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2, I3) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponseForm {
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4, reified O5> API45<APIType.Form, I1, I2, I3, I4, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2, I3, I4) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponseForm {
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4, reified O5> API55<APIType.Form, I1, I2, I3, I4, I5, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2, I3, I4, I5) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponseForm {
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(it(), it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    // WebSockets

    fun Sockets.connect(factory: (Any) -> SocketsManager) {
        routing?.webSocket(this.path) {
            val manager = factory(this)
            cleaning({
                catchingError {
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        manager.onMessage(frame.readText())
                    }
                }?.then { manager.onError(it) }
            }) {
                Coroutines.with(NonCancellable) {
                    cleaning({
                        manager.onClose()
                    }) {
                        this@webSocket.close(CloseReason(CloseReason.Codes.NORMAL, "websockets closed"))
                    }
                }
            }
        }
    }
}