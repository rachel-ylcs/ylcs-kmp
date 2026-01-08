package love.yinlin.api

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import love.yinlin.api.sockets.SocketsManager
import love.yinlin.extension.catchingError
import love.yinlin.extension.makeArray
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.platform.Coroutines
import love.yinlin.server.Database
import love.yinlin.server.Redis
import love.yinlin.server.currentUniqueId
import org.slf4j.Logger
import java.io.File
import kotlin.random.Random

abstract class APIScope internal constructor(
    private val routing: Routing,
    val logger: Logger,
) {
    lateinit var db: Database
        internal set

    lateinit var redis: Redis
        internal set

    fun API<out APIType>.internalResponse(block: suspend (RoutingCall) -> JsonElement) {
        routing.route(path = route, method = HttpMethod.Post) {
            handle {
                catchingError {
                    Coroutines.io {
                        call.respond(status = HttpStatusCode.OK, message = block(call))
                    }
                }?.let { err ->
                    logger.error("CallDie - {}", err.stackTraceToString())
                    when (err) {
                        is UnauthorizedException -> call.respond(status = HttpStatusCode.Unauthorized, message = makeArray { })
                        is FailureException -> call.respondText(status = HttpStatusCode.Accepted, text = err.message ?: "未知错误")
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

    @JvmName("responsePost00")
    inline fun API00<APIType.Post>
            .response(crossinline block: suspend APIResponseScope.() -> Unit) = internalResponsePost {
        APIResponseScope().block()
        makeArray {

        }
    }

    @JvmName("responsePost10")
    inline fun <reified I1> API10<APIType.Post, I1>
            .response(crossinline block: suspend APIResponseScope.(I1) -> Unit) = internalResponsePost {
        val (i1) = it
        APIResponseScope().block(i1.to())
        makeArray {

        }
    }

    @JvmName("responsePost20")
    inline fun <reified I1, reified I2> API20<APIType.Post, I1, I2>
            .response(crossinline block: suspend APIResponseScope.(I1, I2) -> Unit) = internalResponsePost {
        val (i1, i2) = it
        APIResponseScope().block(i1.to(), i2.to())
        makeArray {

        }
    }

    @JvmName("responsePost30")
    inline fun <reified I1, reified I2, reified I3> API30<APIType.Post, I1, I2, I3>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3) -> Unit) = internalResponsePost {
        val (i1, i2, i3) = it
        APIResponseScope().block(i1.to(), i2.to(), i3.to())
        makeArray {

        }
    }

    @JvmName("responsePost40")
    inline fun <reified I1, reified I2, reified I3, reified I4> API40<APIType.Post, I1, I2, I3, I4>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3, I4) -> Unit) = internalResponsePost {
        val (i1, i2, i3, i4) = it
        APIResponseScope().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {

        }
    }

    @JvmName("responsePost50")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5> API50<APIType.Post, I1, I2, I3, I4, I5>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3, I4, I5) -> Unit) = internalResponsePost {
        val (i1, i2, i3, i4, i5) = it
        APIResponseScope().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {

        }
    }

    @JvmName("responsePost01")
    inline fun <reified O1> API01<APIType.Post, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.() -> APIResult1<O1>) = internalResponsePost {
        val (o1) = APIResultScope1<O1>().block()
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("responsePost11")
    inline fun <reified I1, reified O1> API11<APIType.Post, I1, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1) -> APIResult1<O1>) = internalResponsePost {
        val (i1) = it
        val (o1) = APIResultScope1<O1>().block(i1.to())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("responsePost21")
    inline fun <reified I1, reified I2, reified O1> API21<APIType.Post, I1, I2, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2) -> APIResult1<O1>) = internalResponsePost {
        val (i1, i2) = it
        val (o1) = APIResultScope1<O1>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("responsePost31")
    inline fun <reified I1, reified I2, reified I3, reified O1> API31<APIType.Post, I1, I2, I3, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3) -> APIResult1<O1>) = internalResponsePost {
        val (i1, i2, i3) = it
        val (o1) = APIResultScope1<O1>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("responsePost41")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1> API41<APIType.Post, I1, I2, I3, I4, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3, I4) -> APIResult1<O1>) = internalResponsePost {
        val (i1, i2, i3, i4) = it
        val (o1) = APIResultScope1<O1>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("responsePost51")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1> API51<APIType.Post, I1, I2, I3, I4, I5, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3, I4, I5) -> APIResult1<O1>) = internalResponsePost {
        val (i1, i2, i3, i4, i5) = it
        val (o1) = APIResultScope1<O1>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("responsePost02")
    inline fun <reified O1, reified O2> API02<APIType.Post, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.() -> APIResult2<O1, O2>) = internalResponsePost {
        val (o1, o2) = APIResultScope2<O1, O2>().block()
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("responsePost12")
    inline fun <reified I1, reified O1, reified O2> API12<APIType.Post, I1, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1) -> APIResult2<O1, O2>) = internalResponsePost {
        val (i1) = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("responsePost22")
    inline fun <reified I1, reified I2, reified O1, reified O2> API22<APIType.Post, I1, I2, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2) -> APIResult2<O1, O2>) = internalResponsePost {
        val (i1, i2) = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("responsePost32")
    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2> API32<APIType.Post, I1, I2, I3, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3) -> APIResult2<O1, O2>) = internalResponsePost {
        val (i1, i2, i3) = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("responsePost42")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2> API42<APIType.Post, I1, I2, I3, I4, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3, I4) -> APIResult2<O1, O2>) = internalResponsePost {
        val (i1, i2, i3, i4) = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("responsePost52")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2> API52<APIType.Post, I1, I2, I3, I4, I5, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3, I4, I5) -> APIResult2<O1, O2>) = internalResponsePost {
        val (i1, i2, i3, i4, i5) = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("responsePost03")
    inline fun <reified O1, reified O2, reified O3> API03<APIType.Post, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.() -> APIResult3<O1, O2, O3>) = internalResponsePost {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block()
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("responsePost13")
    inline fun <reified I1, reified O1, reified O2, reified O3> API13<APIType.Post, I1, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1) -> APIResult3<O1, O2, O3>) = internalResponsePost {
        val (i1) = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("responsePost23")
    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3> API23<APIType.Post, I1, I2, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2) -> APIResult3<O1, O2, O3>) = internalResponsePost {
        val (i1, i2) = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("responsePost33")
    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3> API33<APIType.Post, I1, I2, I3, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3) -> APIResult3<O1, O2, O3>) = internalResponsePost {
        val (i1, i2, i3) = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("responsePost43")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3> API43<APIType.Post, I1, I2, I3, I4, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3, I4) -> APIResult3<O1, O2, O3>) = internalResponsePost {
        val (i1, i2, i3, i4) = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("responsePost53")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3> API53<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3, I4, I5) -> APIResult3<O1, O2, O3>) = internalResponsePost {
        val (i1, i2, i3, i4, i5) = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("responsePost04")
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

    @JvmName("responsePost14")
    inline fun <reified I1, reified O1, reified O2, reified O3, reified O4> API14<APIType.Post, I1, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1) -> APIResult4<O1, O2, O3, O4>) = internalResponsePost {
        val (i1) = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    @JvmName("responsePost24")
    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4> API24<APIType.Post, I1, I2, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2) -> APIResult4<O1, O2, O3, O4>) = internalResponsePost {
        val (i1, i2) = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    @JvmName("responsePost34")
    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4> API34<APIType.Post, I1, I2, I3, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2, I3) -> APIResult4<O1, O2, O3, O4>) = internalResponsePost {
        val (i1, i2, i3) = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    @JvmName("responsePost44")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4> API44<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2, I3, I4) -> APIResult4<O1, O2, O3, O4>) = internalResponsePost {
        val (i1, i2, i3, i4) = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    @JvmName("responsePost54")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4> API54<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2, I3, I4, I5) -> APIResult4<O1, O2, O3, O4>) = internalResponsePost {
        val (i1, i2, i3, i4, i5) = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    @JvmName("responsePost05")
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

    @JvmName("responsePost15")
    inline fun <reified I1, reified O1, reified O2, reified O3, reified O4, reified O5> API15<APIType.Post, I1, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponsePost {
        val (i1) = it
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(i1.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    @JvmName("responsePost25")
    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4, reified O5> API25<APIType.Post, I1, I2, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponsePost {
        val (i1, i2) = it
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    @JvmName("responsePost35")
    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4, reified O5> API35<APIType.Post, I1, I2, I3, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2, I3) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponsePost {
        val (i1, i2, i3) = it
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    @JvmName("responsePost45")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4, reified O5> API45<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2, I3, I4) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponsePost {
        val (i1, i2, i3, i4) = it
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    @JvmName("responsePost55")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4, reified O5> API55<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2, I3, I4, I5) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponsePost {
        val (i1, i2, i3, i4, i5) = it
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

    class FormResult(private val multipartData: MultiPartData) {
        private val tempDir = System.getProperty("java.io.tmpdir")
        var dataIndex = 0
        var fileIndex = 0
        var dataList = emptyList<String>()
        var fileList = emptyList<APIFile?>()

        suspend fun parse() {
            val dataItems = mutableListOf<Pair<Int, String>>()
            val fileItems = mutableMapOf<Int, MutableList<Pair<Int, String>>>()
            multipartData.forEachPart { part ->
                catchingError {
                    val name: String = part.name ?: return@catchingError
                    when (part) {
                        is PartData.FormItem -> {
                            if (name.startsWith('#')) fileItems[name.removePrefix("#").toInt()] = mutableListOf() // APIFile?
                            else dataItems += name.toInt() to part.value // Normal body data
                        }
                        is PartData.FileItem -> {
                            val keys = name.split(":")
                            val index = keys[0].toInt()
                            val fileIndex = keys.getOrNull(1)?.toInt() ?: 0

                            val tempFilename = "${Random.nextInt(1314520, 5201314)}-$index-${currentUniqueId(fileIndex)}"
                            val tempFile = File(tempDir, tempFilename)
                            if (part.provider().copyAndClose(tempFile.writeChannel()) > 0) {
                                val oldItems = fileItems.getOrPut(index) { mutableListOf() }
                                oldItems += fileIndex to tempFile.absolutePath
                            }
                        }
                        else -> { }
                    }
                }
                part.dispose()
            }
            dataList = dataItems.sortedBy { it.first }.map { it.second }
            fileList = fileItems.toList().sortedBy { it.first }.map { (_, items) ->
                items.sortedBy { it.first }.map { it.second }.ifEmpty { null }?.let(::ServerAPIFile)
            }
        }

        inline operator fun <reified I> invoke(): I = if (I::class == APIFile::class) fileList[fileIndex++] as I else dataList[dataIndex++].parseJsonValue()
    }

    inline fun API<out APIType>.internalResponseForm(crossinline block: suspend (FormResult) -> JsonElement) {
        internalResponse {
            val formResult = FormResult(it.receiveMultipart())
            formResult.parse()
            block(formResult)
        }
    }

    @JvmName("responseForm10")
    inline fun <reified I1> API10<APIType.Form, I1>
            .response(crossinline block: suspend APIResponseScope.(I1) -> Unit) = internalResponseForm {
        APIResponseScope().block(it())
        makeArray {

        }
    }

    @JvmName("responseForm20")
    inline fun <reified I1, reified I2> API20<APIType.Form, I1, I2>
            .response(crossinline block: suspend APIResponseScope.(I1, I2) -> Unit) = internalResponseForm {
        APIResponseScope().block(it(), it())
        makeArray {

        }
    }

    @JvmName("responseForm30")
    inline fun <reified I1, reified I2, reified I3> API30<APIType.Form, I1, I2, I3>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3) -> Unit) = internalResponseForm {
        APIResponseScope().block(it(), it(), it())
        makeArray {

        }
    }

    @JvmName("responseForm40")
    inline fun <reified I1, reified I2, reified I3, reified I4> API40<APIType.Form, I1, I2, I3, I4>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3, I4) -> Unit) = internalResponseForm {
        APIResponseScope().block(it(), it(), it(), it())
        makeArray {

        }
    }

    @JvmName("responseForm50")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5> API50<APIType.Form, I1, I2, I3, I4, I5>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3, I4, I5) -> Unit) = internalResponseForm {
        APIResponseScope().block(it(), it(), it(), it(), it())
        makeArray {

        }
    }

    @JvmName("responseForm11")
    inline fun <reified I1, reified O1> API11<APIType.Form, I1, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1) -> APIResult1<O1>) = internalResponseForm {
        val (o1) = APIResultScope1<O1>().block(it())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("responseForm21")
    inline fun <reified I1, reified I2, reified O1> API21<APIType.Form, I1, I2, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2) -> APIResult1<O1>) = internalResponseForm {
        val (o1) = APIResultScope1<O1>().block(it(), it())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("responseForm31")
    inline fun <reified I1, reified I2, reified I3, reified O1> API31<APIType.Form, I1, I2, I3, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3) -> APIResult1<O1>) = internalResponseForm {
        val (o1) = APIResultScope1<O1>().block(it(), it(), it())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("responseForm41")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1> API41<APIType.Form, I1, I2, I3, I4, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3, I4) -> APIResult1<O1>) = internalResponseForm {
        val (o1) = APIResultScope1<O1>().block(it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("responseForm51")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1> API51<APIType.Form, I1, I2, I3, I4, I5, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3, I4, I5) -> APIResult1<O1>) = internalResponseForm {
        val (o1) = APIResultScope1<O1>().block(it(), it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("responseForm12")
    inline fun <reified I1, reified O1, reified O2> API12<APIType.Form, I1, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1) -> APIResult2<O1, O2>) = internalResponseForm {
        val (o1, o2) = APIResultScope2<O1, O2>().block(it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("responseForm22")
    inline fun <reified I1, reified I2, reified O1, reified O2> API22<APIType.Form, I1, I2, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2) -> APIResult2<O1, O2>) = internalResponseForm {
        val (o1, o2) = APIResultScope2<O1, O2>().block(it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("responseForm32")
    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2> API32<APIType.Form, I1, I2, I3, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3) -> APIResult2<O1, O2>) = internalResponseForm {
        val (o1, o2) = APIResultScope2<O1, O2>().block(it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("responseForm42")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2> API42<APIType.Form, I1, I2, I3, I4, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3, I4) -> APIResult2<O1, O2>) = internalResponseForm {
        val (o1, o2) = APIResultScope2<O1, O2>().block(it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("responseForm52")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2> API52<APIType.Form, I1, I2, I3, I4, I5, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3, I4, I5) -> APIResult2<O1, O2>) = internalResponseForm {
        val (o1, o2) = APIResultScope2<O1, O2>().block(it(), it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("responseForm13")
    inline fun <reified I1, reified O1, reified O2, reified O3> API13<APIType.Form, I1, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1) -> APIResult3<O1, O2, O3>) = internalResponseForm {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("responseForm23")
    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3> API23<APIType.Form, I1, I2, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2) -> APIResult3<O1, O2, O3>) = internalResponseForm {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("responseForm33")
    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3> API33<APIType.Form, I1, I2, I3, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3) -> APIResult3<O1, O2, O3>) = internalResponseForm {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("responseForm43")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3> API43<APIType.Form, I1, I2, I3, I4, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3, I4) -> APIResult3<O1, O2, O3>) = internalResponseForm {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("responseForm53")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3> API53<APIType.Form, I1, I2, I3, I4, I5, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3, I4, I5) -> APIResult3<O1, O2, O3>) = internalResponseForm {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(it(), it(), it(), it(), it())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("responseForm14")
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

    @JvmName("responseForm24")
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

    @JvmName("responseForm34")
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

    @JvmName("responseForm44")
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

    @JvmName("responseForm54")
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

    @JvmName("responseForm15")
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

    @JvmName("responseForm25")
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

    @JvmName("responseForm35")
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

    @JvmName("responseForm45")
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

    @JvmName("responseForm55")
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
        routing.webSocket(this.path) {
            val manager = factory(this)
            catchingError {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    manager.onMessage(frame.readText())
                }
            }?.let { manager.onError(it) }
            manager.onClose()
            this.close(CloseReason(CloseReason.Codes.NORMAL, "websockets closed"))
        }
    }
}