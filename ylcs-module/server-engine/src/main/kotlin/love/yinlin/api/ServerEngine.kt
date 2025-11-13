package love.yinlin.api

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import love.yinlin.extension.catchingError
import love.yinlin.extension.makeArray
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.platform.Coroutines
import love.yinlin.server.logger

@ConsistentCopyVisibility
data class APIResult1<O1> internal constructor(val o1: O1)
@ConsistentCopyVisibility
data class APIResult2<O1, O2> internal constructor(val o1: O1, val o2: O2)
@ConsistentCopyVisibility
data class APIResult3<O1, O2, O3> internal constructor(val o1: O1, val o2: O2, val o3: O3)
@ConsistentCopyVisibility
data class APIResult4<O1, O2, O3, O4> internal constructor(val o1: O1, val o2: O2, val o3: O3, val o4: O4)
@ConsistentCopyVisibility
data class APIResult5<O1, O2, O3, O4, O5> internal constructor(val o1: O1, val o2: O2, val o3: O3, val o4: O4, val o5: O5)

open class APIResponseScope {
    fun expire(): Nothing = throw UnauthorizedException(null)
    fun failure(message: String? = null): Nothing = throw FailureException(message)
}

class APIResultScope1<O1> : APIResponseScope() {
    fun result(o1: O1) = APIResult1(o1)
}

class APIResultScope2<O1, O2> : APIResponseScope() {
    fun result(o1: O1, o2: O2) = APIResult2(o1, o2)
}

class APIResultScope3<O1, O2, O3> : APIResponseScope() {
    fun result(o1: O1, o2: O2, o3: O3) = APIResult3(o1, o2, o3)
}

class APIResultScope4<O1, O2, O3, O4> : APIResponseScope() {
    fun result(o1: O1, o2: O2, o3: O3, o4: O4) = APIResult4(o1, o2, o3, o4)
}

class APIResultScope5<O1, O2, O3, O4, O5> : APIResponseScope() {
    fun result(o1: O1, o2: O2, o3: O3, o4: O4, o5: O5) = APIResult5(o1, o2, o3, o4, o5)
}

data class APIScope(val routing: Routing) {
    inline fun API<out APIType>.internalResponse(crossinline block: suspend (JsonArray) -> JsonElement) {
        routing.route(path = route, method = HttpMethod.Post) {
            handle {
                catchingError {
                    Coroutines.io {
                        val array = call.receive<JsonArray>()
                        val result = block(array)
                        call.respond(status = HttpStatusCode.OK, message = result)
                    }
                }?.let { err ->
                    logger.error("CallDie - {}", err.stackTraceToString())
                    when (err) {
                        is UnauthorizedException -> call.respond(status = HttpStatusCode.Unauthorized, message = makeArray { })
                        is FailureException -> call.respondText(status = HttpStatusCode(1211, ""), text = err.message ?: "未知错误")
                        else -> call.respond(status = HttpStatusCode.Forbidden, message = makeArray { })
                    }
                }
            }
        }
    }

    @JvmName("response00")
    inline fun API00<APIType.Post>
            .response(crossinline block: suspend APIResponseScope.() -> Unit) = internalResponse {
        APIResponseScope().block()
        makeArray {  }
    }

    @JvmName("response10")
    inline fun <reified I1> API10<APIType.Post, I1>
            .response(crossinline block: suspend APIResponseScope.(I1) -> Unit) = internalResponse {
        val (i1) = it
        APIResponseScope().block(i1.to())
        makeArray {  }
    }

    @JvmName("response20")
    inline fun <reified I1, reified I2> API20<APIType.Post, I1, I2>
            .response(crossinline block: suspend APIResponseScope.(I1, I2) -> Unit) = internalResponse {
        val (i1, i2) = it
        APIResponseScope().block(i1.to(), i2.to())
        makeArray {  }
    }

    @JvmName("response30")
    inline fun <reified I1, reified I2, reified I3> API30<APIType.Post, I1, I2, I3>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3) -> Unit) = internalResponse {
        val (i1, i2, i3) = it
        APIResponseScope().block(i1.to(), i2.to(), i3.to())
        makeArray {  }
    }

    @JvmName("response40")
    inline fun <reified I1, reified I2, reified I3, reified I4> API40<APIType.Post, I1, I2, I3, I4>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3, I4) -> Unit) = internalResponse {
        val (i1, i2, i3, i4) = it
        APIResponseScope().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {  }
    }

    @JvmName("response50")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5> API50<APIType.Post, I1, I2, I3, I4, I5>
            .response(crossinline block: suspend APIResponseScope.(I1, I2, I3, I4, I5) -> Unit) = internalResponse {
        val (i1, i2, i3, i4, i5) = it
        APIResponseScope().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {  }
    }

    @JvmName("response01")
    inline fun <reified O1> API01<APIType.Post, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.() -> APIResult1<O1>) = internalResponse {
        val (o1) = APIResultScope1<O1>().block()
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("response11")
    inline fun <reified I1, reified O1> API11<APIType.Post, I1, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1) -> APIResult1<O1>) = internalResponse {
        val (i1) = it
        val (o1) = APIResultScope1<O1>().block(i1.to())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("response21")
    inline fun <reified I1, reified I2, reified O1> API21<APIType.Post, I1, I2, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2) -> APIResult1<O1>) = internalResponse {
        val (i1, i2) = it
        val (o1) = APIResultScope1<O1>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("response31")
    inline fun <reified I1, reified I2, reified I3, reified O1> API31<APIType.Post, I1, I2, I3, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3) -> APIResult1<O1>) = internalResponse {
        val (i1, i2, i3) = it
        val (o1) = APIResultScope1<O1>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("response41")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1> API41<APIType.Post, I1, I2, I3, I4, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3, I4) -> APIResult1<O1>) = internalResponse {
        val (i1, i2, i3, i4) = it
        val (o1) = APIResultScope1<O1>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("response51")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1> API51<APIType.Post, I1, I2, I3, I4, I5, O1>
            .response(crossinline block: suspend APIResultScope1<O1>.(I1, I2, I3, I4, I5) -> APIResult1<O1>) = internalResponse {
        val (i1, i2, i3, i4, i5) = it
        val (o1) = APIResultScope1<O1>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
        }
    }

    @JvmName("response02")
    inline fun <reified O1, reified O2> API02<APIType.Post, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.() -> APIResult2<O1, O2>) = internalResponse {
        val (o1, o2) = APIResultScope2<O1, O2>().block()
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("response12")
    inline fun <reified I1, reified O1, reified O2> API12<APIType.Post, I1, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1) -> APIResult2<O1, O2>) = internalResponse {
        val (i1) = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("response22")
    inline fun <reified I1, reified I2, reified O1, reified O2> API22<APIType.Post, I1, I2, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2) -> APIResult2<O1, O2>) = internalResponse {
        val (i1, i2) = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("response32")
    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2> API32<APIType.Post, I1, I2, I3, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3) -> APIResult2<O1, O2>) = internalResponse {
        val (i1, i2, i3) = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("response42")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2> API42<APIType.Post, I1, I2, I3, I4, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3, I4) -> APIResult2<O1, O2>) = internalResponse {
        val (i1, i2, i3, i4) = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("response52")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2> API52<APIType.Post, I1, I2, I3, I4, I5, O1, O2>
            .response(crossinline block: suspend APIResultScope2<O1, O2>.(I1, I2, I3, I4, I5) -> APIResult2<O1, O2>) = internalResponse {
        val (i1, i2, i3, i4, i5) = it
        val (o1, o2) = APIResultScope2<O1, O2>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
        }
    }

    @JvmName("response03")
    inline fun <reified O1, reified O2, reified O3> API03<APIType.Post, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.() -> APIResult3<O1, O2, O3>) = internalResponse {
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block()
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("response13")
    inline fun <reified I1, reified O1, reified O2, reified O3> API13<APIType.Post, I1, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1) -> APIResult3<O1, O2, O3>) = internalResponse {
        val (i1) = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("response23")
    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3> API23<APIType.Post, I1, I2, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2) -> APIResult3<O1, O2, O3>) = internalResponse {
        val (i1, i2) = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("response33")
    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3> API33<APIType.Post, I1, I2, I3, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3) -> APIResult3<O1, O2, O3>) = internalResponse {
        val (i1, i2, i3) = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("response43")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3> API43<APIType.Post, I1, I2, I3, I4, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3, I4) -> APIResult3<O1, O2, O3>) = internalResponse {
        val (i1, i2, i3, i4) = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("response53")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3> API53<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3>
            .response(crossinline block: suspend APIResultScope3<O1, O2, O3>.(I1, I2, I3, I4, I5) -> APIResult3<O1, O2, O3>) = internalResponse {
        val (i1, i2, i3, i4, i5) = it
        val (o1, o2, o3) = APIResultScope3<O1, O2, O3>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
        }
    }

    @JvmName("response04")
    inline fun <reified O1, reified O2, reified O3, reified O4> API04<APIType.Post, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.() -> APIResult4<O1, O2, O3, O4>) = internalResponse {
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block()
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    @JvmName("response14")
    inline fun <reified I1, reified O1, reified O2, reified O3, reified O4> API14<APIType.Post, I1, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1) -> APIResult4<O1, O2, O3, O4>) = internalResponse {
        val (i1) = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    @JvmName("response24")
    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4> API24<APIType.Post, I1, I2, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2) -> APIResult4<O1, O2, O3, O4>) = internalResponse {
        val (i1, i2) = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to(), i2.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    @JvmName("response34")
    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4> API34<APIType.Post, I1, I2, I3, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2, I3) -> APIResult4<O1, O2, O3, O4>) = internalResponse {
        val (i1, i2, i3) = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to(), i2.to(), i3.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    @JvmName("response44")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4> API44<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2, I3, I4) -> APIResult4<O1, O2, O3, O4>) = internalResponse {
        val (i1, i2, i3, i4) = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to(), i2.to(), i3.to(), i4.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    @JvmName("response54")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4> API54<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4>
            .response(crossinline block: suspend APIResultScope4<O1, O2, O3, O4>.(I1, I2, I3, I4, I5) -> APIResult4<O1, O2, O3, O4>) = internalResponse {
        val (i1, i2, i3, i4, i5) = it
        val (o1, o2, o3, o4) = APIResultScope4<O1, O2, O3, O4>().block(i1.to(), i2.to(), i3.to(), i4.to(), i5.to())
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
        }
    }

    @JvmName("response05")
    inline fun <reified O1, reified O2, reified O3, reified O4, reified O5> API05<APIType.Post, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.() -> APIResult5<O1, O2, O3, O4, O5>) = internalResponse {
        val (o1, o2, o3, o4, o5) = APIResultScope5<O1, O2, O3, O4, O5>().block()
        makeArray {
            add(o1.toJson())
            add(o2.toJson())
            add(o3.toJson())
            add(o4.toJson())
            add(o5.toJson())
        }
    }

    @JvmName("response15")
    inline fun <reified I1, reified O1, reified O2, reified O3, reified O4, reified O5> API15<APIType.Post, I1, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponse {
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

    @JvmName("response25")
    inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4, reified O5> API25<APIType.Post, I1, I2, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponse {
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

    @JvmName("response35")
    inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4, reified O5> API35<APIType.Post, I1, I2, I3, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2, I3) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponse {
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

    @JvmName("response45")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4, reified O5> API45<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2, I3, I4) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponse {
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

    @JvmName("response55")
    inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4, reified O5> API55<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4, O5>
            .response(crossinline block: suspend APIResultScope5<O1, O2, O3, O4, O5>.(I1, I2, I3, I4, I5) -> APIResult5<O1, O2, O3, O4, O5>) = internalResponse {
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
}