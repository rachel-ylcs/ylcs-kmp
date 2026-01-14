package love.yinlin.cs

import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.JsonArray
import love.yinlin.data.Data
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingError
import love.yinlin.extension.catchingNull
import love.yinlin.extension.makeArray
import love.yinlin.extension.to
import love.yinlin.extension.toJson

suspend inline fun API<APIType.Post>.internalRequestCallback(
    noinline builder: HttpRequestBuilder.() -> Unit,
    crossinline block: suspend (HttpResponse) -> Unit
) = catchingError { internalRequest(builder, block) }

suspend inline fun <reified R : Any> API<APIType.Post>.internalRequestReturn(
    noinline builder: HttpRequestBuilder.() -> Unit,
    crossinline block: suspend (HttpResponse) -> R
) = catchingDefault({ Data.Failure(it) }) { Data.Success(internalRequest(builder, block)) }

suspend inline fun <reified R : Any> API<APIType.Post>.internalRequestReturnNull(
    noinline builder: HttpRequestBuilder.() -> Unit,
    crossinline block: suspend (HttpResponse) -> R
) = catchingNull { internalRequest(builder, block) }

// 回调版

suspend inline fun API00<APIType.Post>
        .request(crossinline block: suspend () -> Unit) =
    internalRequestCallback({ setBody(makeArray {

    }) }) {
        block()
    }

suspend inline fun <reified I1> API10<APIType.Post, I1>
        .request(i1: I1, crossinline block: suspend () -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        block()
    }

suspend inline fun <reified I1, reified I2> API20<APIType.Post, I1, I2>
        .request(i1: I1, i2: I2, crossinline block: suspend () -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        block()
    }

suspend inline fun <reified I1, reified I2, reified I3> API30<APIType.Post, I1, I2, I3>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend () -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        block()
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4> API40<APIType.Post, I1, I2, I3, I4>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend () -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        block()
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5> API50<APIType.Post, I1, I2, I3, I4, I5>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend () -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        block()
    }

suspend inline fun <reified O1> API01<APIType.Post, O1>
        .request(crossinline block: suspend (O1) -> Unit) =
    internalRequestCallback({ setBody(makeArray {

    }) }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }

suspend inline fun <reified I1, reified O1> API11<APIType.Post, I1, O1>
        .request(i1: I1, crossinline block: suspend (O1) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }

suspend inline fun <reified I1, reified I2, reified O1> API21<APIType.Post, I1, I2, O1>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1> API31<APIType.Post, I1, I2, I3, O1>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1> API41<APIType.Post, I1, I2, I3, I4, O1>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1> API51<APIType.Post, I1, I2, I3, I4, I5, O1>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }

suspend inline fun <reified O1, reified O2> API02<APIType.Post, O1, O2>
        .request(crossinline block: suspend (O1, O2) -> Unit) =
    internalRequestCallback({ setBody(makeArray {

    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }

suspend inline fun <reified I1, reified O1, reified O2> API12<APIType.Post, I1, O1, O2>
        .request(i1: I1, crossinline block: suspend (O1, O2) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2> API22<APIType.Post, I1, I2, O1, O2>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1, O2) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2> API32<APIType.Post, I1, I2, I3, O1, O2>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1, O2) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2> API42<APIType.Post, I1, I2, I3, I4, O1, O2>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1, O2) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2> API52<APIType.Post, I1, I2, I3, I4, I5, O1, O2>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1, O2) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }

suspend inline fun <reified O1, reified O2, reified O3> API03<APIType.Post, O1, O2, O3>
        .request(crossinline block: suspend (O1, O2, O3) -> Unit) =
    internalRequestCallback({ setBody(makeArray {

    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }

suspend inline fun <reified I1, reified O1, reified O2, reified O3> API13<APIType.Post, I1, O1, O2, O3>
        .request(i1: I1, crossinline block: suspend (O1, O2, O3) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3> API23<APIType.Post, I1, I2, O1, O2, O3>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1, O2, O3) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3> API33<APIType.Post, I1, I2, I3, O1, O2, O3>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1, O2, O3) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3> API43<APIType.Post, I1, I2, I3, I4, O1, O2, O3>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1, O2, O3) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3> API53<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1, O2, O3) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }

suspend inline fun <reified O1, reified O2, reified O3, reified O4> API04<APIType.Post, O1, O2, O3, O4>
        .request(crossinline block: suspend (O1, O2, O3, O4) -> Unit) =
    internalRequestCallback({ setBody(makeArray {

    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }

suspend inline fun <reified I1, reified O1, reified O2, reified O3, reified O4> API14<APIType.Post, I1, O1, O2, O3, O4>
        .request(i1: I1, crossinline block: suspend (O1, O2, O3, O4) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4> API24<APIType.Post, I1, I2, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1, O2, O3, O4) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4> API34<APIType.Post, I1, I2, I3, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1, O2, O3, O4) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4> API44<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1, O2, O3, O4) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4> API54<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1, O2, O3, O4) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }

suspend inline fun <reified O1, reified O2, reified O3, reified O4, reified O5> API05<APIType.Post, O1, O2, O3, O4, O5>
        .request(crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit) =
    internalRequestCallback({ setBody(makeArray {

    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }

suspend inline fun <reified I1, reified O1, reified O2, reified O3, reified O4, reified O5> API15<APIType.Post, I1, O1, O2, O3, O4, O5>
        .request(i1: I1, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4, reified O5> API25<APIType.Post, I1, I2, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4, reified O5> API35<APIType.Post, I1, I2, I3, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4, reified O5> API45<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4, reified O5> API55<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit) =
    internalRequestCallback({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }


// 返回版


suspend inline fun API00<APIType.Post>
        .request() =
    internalRequestReturn({ setBody(makeArray {

    }) }) { }

suspend inline fun <reified I1> API10<APIType.Post, I1>
        .request(i1: I1) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
    }) }) { }

suspend inline fun <reified I1, reified I2> API20<APIType.Post, I1, I2>
        .request(i1: I1, i2: I2) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) { }

suspend inline fun <reified I1, reified I2, reified I3> API30<APIType.Post, I1, I2, I3>
        .request(i1: I1, i2: I2, i3: I3) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) { }

suspend inline fun <reified I1, reified I2, reified I3, reified I4> API40<APIType.Post, I1, I2, I3, I4>
        .request(i1: I1, i2: I2, i3: I3, i4: I4) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) { }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5> API50<APIType.Post, I1, I2, I3, I4, I5>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) { }

suspend inline fun <reified O1> API01<APIType.Post, O1>
        .request() =
    internalRequestReturn({ setBody(makeArray {

    }) }) {
        val (o1) = it.body<JsonArray>()
        APIResult1(o1.to<O1>())
    }

suspend inline fun <reified I1, reified O1> API11<APIType.Post, I1, O1>
        .request(i1: I1) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        APIResult1(o1.to<O1>())
    }

suspend inline fun <reified I1, reified I2, reified O1> API21<APIType.Post, I1, I2, O1>
        .request(i1: I1, i2: I2) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        APIResult1(o1.to<O1>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1> API31<APIType.Post, I1, I2, I3, O1>
        .request(i1: I1, i2: I2, i3: I3) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        APIResult1(o1.to<O1>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1> API41<APIType.Post, I1, I2, I3, I4, O1>
        .request(i1: I1, i2: I2, i3: I3, i4: I4) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        APIResult1(o1.to<O1>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1> API51<APIType.Post, I1, I2, I3, I4, I5, O1>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        APIResult1(o1.to<O1>())
    }

suspend inline fun <reified O1, reified O2> API02<APIType.Post, O1, O2>
        .request() =
    internalRequestReturn({ setBody(makeArray {

    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        APIResult2(o1.to<O1>(), o2.to<O2>())
    }

suspend inline fun <reified I1, reified O1, reified O2> API12<APIType.Post, I1, O1, O2>
        .request(i1: I1) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        APIResult2(o1.to<O1>(), o2.to<O2>())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2> API22<APIType.Post, I1, I2, O1, O2>
        .request(i1: I1, i2: I2) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        APIResult2(o1.to<O1>(), o2.to<O2>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2> API32<APIType.Post, I1, I2, I3, O1, O2>
        .request(i1: I1, i2: I2, i3: I3) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        APIResult2(o1.to<O1>(), o2.to<O2>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2> API42<APIType.Post, I1, I2, I3, I4, O1, O2>
        .request(i1: I1, i2: I2, i3: I3, i4: I4) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        APIResult2(o1.to<O1>(), o2.to<O2>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2> API52<APIType.Post, I1, I2, I3, I4, I5, O1, O2>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        APIResult2(o1.to<O1>(), o2.to<O2>())
    }

suspend inline fun <reified O1, reified O2, reified O3> API03<APIType.Post, O1, O2, O3>
        .request() =
    internalRequestReturn({ setBody(makeArray {

    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        APIResult3(o1.to<O1>(), o2.to<O2>(), o3.to<O3>())
    }

suspend inline fun <reified I1, reified O1, reified O2, reified O3> API13<APIType.Post, I1, O1, O2, O3>
        .request(i1: I1) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        APIResult3(o1.to<O1>(), o2.to<O2>(), o3.to<O3>())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3> API23<APIType.Post, I1, I2, O1, O2, O3>
        .request(i1: I1, i2: I2) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        APIResult3(o1.to<O1>(), o2.to<O2>(), o3.to<O3>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3> API33<APIType.Post, I1, I2, I3, O1, O2, O3>
        .request(i1: I1, i2: I2, i3: I3) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        APIResult3(o1.to<O1>(), o2.to<O2>(), o3.to<O3>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3> API43<APIType.Post, I1, I2, I3, I4, O1, O2, O3>
        .request(i1: I1, i2: I2, i3: I3, i4: I4) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        APIResult3(o1.to<O1>(), o2.to<O2>(), o3.to<O3>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3> API53<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        APIResult3(o1.to<O1>(), o2.to<O2>(), o3.to<O3>())
    }

suspend inline fun <reified O1, reified O2, reified O3, reified O4> API04<APIType.Post, O1, O2, O3, O4>
        .request() =
    internalRequestReturn({ setBody(makeArray {

    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        APIResult4(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>())
    }

suspend inline fun <reified I1, reified O1, reified O2, reified O3, reified O4> API14<APIType.Post, I1, O1, O2, O3, O4>
        .request(i1: I1) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        APIResult4(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4> API24<APIType.Post, I1, I2, O1, O2, O3, O4>
        .request(i1: I1, i2: I2) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        APIResult4(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4> API34<APIType.Post, I1, I2, I3, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, i3: I3) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        APIResult4(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4> API44<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, i3: I3, i4: I4) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        APIResult4(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4> API54<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        APIResult4(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>())
    }

suspend inline fun <reified O1, reified O2, reified O3, reified O4, reified O5> API05<APIType.Post, O1, O2, O3, O4, O5>
        .request() =
    internalRequestReturn({ setBody(makeArray {

    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        APIResult5(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>(), o5.to<O5>())
    }

suspend inline fun <reified I1, reified O1, reified O2, reified O3, reified O4, reified O5> API15<APIType.Post, I1, O1, O2, O3, O4, O5>
        .request(i1: I1) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        APIResult5(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>(), o5.to<O5>())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4, reified O5> API25<APIType.Post, I1, I2, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        APIResult5(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>(), o5.to<O5>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4, reified O5> API35<APIType.Post, I1, I2, I3, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, i3: I3) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        APIResult5(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>(), o5.to<O5>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4, reified O5> API45<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, i3: I3, i4: I4) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        APIResult5(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>(), o5.to<O5>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4, reified O5> API55<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) =
    internalRequestReturn({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        APIResult5(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>(), o5.to<O5>())
    }


// 返回 Null 版


suspend inline fun API00<APIType.Post>
        .requestNull() =
    internalRequestReturnNull({ setBody(makeArray {

    }) }) { }

suspend inline fun <reified I1> API10<APIType.Post, I1>
        .requestNull(i1: I1) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
    }) }) { }

suspend inline fun <reified I1, reified I2> API20<APIType.Post, I1, I2>
        .requestNull(i1: I1, i2: I2) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) { }

suspend inline fun <reified I1, reified I2, reified I3> API30<APIType.Post, I1, I2, I3>
        .requestNull(i1: I1, i2: I2, i3: I3) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) { }

suspend inline fun <reified I1, reified I2, reified I3, reified I4> API40<APIType.Post, I1, I2, I3, I4>
        .requestNull(i1: I1, i2: I2, i3: I3, i4: I4) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) { }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5> API50<APIType.Post, I1, I2, I3, I4, I5>
        .requestNull(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) { }

suspend inline fun <reified O1> API01<APIType.Post, O1>
        .requestNull() =
    internalRequestReturnNull({ setBody(makeArray {

    }) }) {
        val (o1) = it.body<JsonArray>()
        APIResult1(o1.to<O1>())
    }

suspend inline fun <reified I1, reified O1> API11<APIType.Post, I1, O1>
        .requestNull(i1: I1) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        APIResult1(o1.to<O1>())
    }

suspend inline fun <reified I1, reified I2, reified O1> API21<APIType.Post, I1, I2, O1>
        .requestNull(i1: I1, i2: I2) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        APIResult1(o1.to<O1>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1> API31<APIType.Post, I1, I2, I3, O1>
        .requestNull(i1: I1, i2: I2, i3: I3) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        APIResult1(o1.to<O1>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1> API41<APIType.Post, I1, I2, I3, I4, O1>
        .requestNull(i1: I1, i2: I2, i3: I3, i4: I4) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        APIResult1(o1.to<O1>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1> API51<APIType.Post, I1, I2, I3, I4, I5, O1>
        .requestNull(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        APIResult1(o1.to<O1>())
    }

suspend inline fun <reified O1, reified O2> API02<APIType.Post, O1, O2>
        .requestNull() =
    internalRequestReturnNull({ setBody(makeArray {

    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        APIResult2(o1.to<O1>(), o2.to<O2>())
    }

suspend inline fun <reified I1, reified O1, reified O2> API12<APIType.Post, I1, O1, O2>
        .requestNull(i1: I1) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        APIResult2(o1.to<O1>(), o2.to<O2>())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2> API22<APIType.Post, I1, I2, O1, O2>
        .requestNull(i1: I1, i2: I2) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        APIResult2(o1.to<O1>(), o2.to<O2>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2> API32<APIType.Post, I1, I2, I3, O1, O2>
        .requestNull(i1: I1, i2: I2, i3: I3) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        APIResult2(o1.to<O1>(), o2.to<O2>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2> API42<APIType.Post, I1, I2, I3, I4, O1, O2>
        .requestNull(i1: I1, i2: I2, i3: I3, i4: I4) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        APIResult2(o1.to<O1>(), o2.to<O2>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2> API52<APIType.Post, I1, I2, I3, I4, I5, O1, O2>
        .requestNull(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        APIResult2(o1.to<O1>(), o2.to<O2>())
    }

suspend inline fun <reified O1, reified O2, reified O3> API03<APIType.Post, O1, O2, O3>
        .requestNull() =
    internalRequestReturnNull({ setBody(makeArray {

    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        APIResult3(o1.to<O1>(), o2.to<O2>(), o3.to<O3>())
    }

suspend inline fun <reified I1, reified O1, reified O2, reified O3> API13<APIType.Post, I1, O1, O2, O3>
        .requestNull(i1: I1) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        APIResult3(o1.to<O1>(), o2.to<O2>(), o3.to<O3>())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3> API23<APIType.Post, I1, I2, O1, O2, O3>
        .requestNull(i1: I1, i2: I2) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        APIResult3(o1.to<O1>(), o2.to<O2>(), o3.to<O3>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3> API33<APIType.Post, I1, I2, I3, O1, O2, O3>
        .requestNull(i1: I1, i2: I2, i3: I3) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        APIResult3(o1.to<O1>(), o2.to<O2>(), o3.to<O3>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3> API43<APIType.Post, I1, I2, I3, I4, O1, O2, O3>
        .requestNull(i1: I1, i2: I2, i3: I3, i4: I4) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        APIResult3(o1.to<O1>(), o2.to<O2>(), o3.to<O3>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3> API53<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3>
        .requestNull(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        APIResult3(o1.to<O1>(), o2.to<O2>(), o3.to<O3>())
    }

suspend inline fun <reified O1, reified O2, reified O3, reified O4> API04<APIType.Post, O1, O2, O3, O4>
        .requestNull() =
    internalRequestReturnNull({ setBody(makeArray {

    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        APIResult4(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>())
    }

suspend inline fun <reified I1, reified O1, reified O2, reified O3, reified O4> API14<APIType.Post, I1, O1, O2, O3, O4>
        .requestNull(i1: I1) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        APIResult4(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4> API24<APIType.Post, I1, I2, O1, O2, O3, O4>
        .requestNull(i1: I1, i2: I2) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        APIResult4(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4> API34<APIType.Post, I1, I2, I3, O1, O2, O3, O4>
        .requestNull(i1: I1, i2: I2, i3: I3) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        APIResult4(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4> API44<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4>
        .requestNull(i1: I1, i2: I2, i3: I3, i4: I4) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        APIResult4(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4> API54<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4>
        .requestNull(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        APIResult4(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>())
    }

suspend inline fun <reified O1, reified O2, reified O3, reified O4, reified O5> API05<APIType.Post, O1, O2, O3, O4, O5>
        .requestNull() =
    internalRequestReturnNull({ setBody(makeArray {

    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        APIResult5(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>(), o5.to<O5>())
    }

suspend inline fun <reified I1, reified O1, reified O2, reified O3, reified O4, reified O5> API15<APIType.Post, I1, O1, O2, O3, O4, O5>
        .requestNull(i1: I1) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        APIResult5(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>(), o5.to<O5>())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4, reified O5> API25<APIType.Post, I1, I2, O1, O2, O3, O4, O5>
        .requestNull(i1: I1, i2: I2) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        APIResult5(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>(), o5.to<O5>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4, reified O5> API35<APIType.Post, I1, I2, I3, O1, O2, O3, O4, O5>
        .requestNull(i1: I1, i2: I2, i3: I3) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        APIResult5(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>(), o5.to<O5>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4, reified O5> API45<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4, O5>
        .requestNull(i1: I1, i2: I2, i3: I3, i4: I4) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        APIResult5(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>(), o5.to<O5>())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4, reified O5> API55<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4, O5>
        .requestNull(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) =
    internalRequestReturnNull({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        APIResult5(o1.to<O1>(), o2.to<O2>(), o3.to<O3>(), o4.to<O4>(), o5.to<O5>())
    }