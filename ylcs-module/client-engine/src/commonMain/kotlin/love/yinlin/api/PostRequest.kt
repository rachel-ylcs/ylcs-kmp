package love.yinlin.api

import io.ktor.client.call.body
import io.ktor.client.request.setBody
import kotlinx.serialization.json.JsonArray
import love.yinlin.extension.makeArray
import love.yinlin.extension.to
import love.yinlin.extension.toJson

suspend inline fun API00<APIType.Post>
        .request(crossinline block: suspend () -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {

    }) }) {
        block()
    }

suspend inline fun <reified I1> API10<APIType.Post, I1>
        .request(i1: I1, crossinline block: suspend () -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        block()
    }

suspend inline fun <reified I1, reified I2> API20<APIType.Post, I1, I2>
        .request(i1: I1, i2: I2, crossinline block: suspend () -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        block()
    }

suspend inline fun <reified I1, reified I2, reified I3> API30<APIType.Post, I1, I2, I3>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend () -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        block()
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4> API40<APIType.Post, I1, I2, I3, I4>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend () -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        block()
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5> API50<APIType.Post, I1, I2, I3, I4, I5>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend () -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        block()
    }

suspend inline fun <reified O1> API01<APIType.Post, O1>
        .request(crossinline block: suspend (O1) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {

    }) }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }

suspend inline fun <reified I1, reified O1> API11<APIType.Post, I1, O1>
        .request(i1: I1, crossinline block: suspend (O1) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }

suspend inline fun <reified I1, reified I2, reified O1> API21<APIType.Post, I1, I2, O1>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1> API31<APIType.Post, I1, I2, I3, O1>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1> API41<APIType.Post, I1, I2, I3, I4, O1>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1> API51<APIType.Post, I1, I2, I3, I4, I5, O1>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
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
        .request(crossinline block: suspend (O1, O2) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {

    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }

suspend inline fun <reified I1, reified O1, reified O2> API12<APIType.Post, I1, O1, O2>
        .request(i1: I1, crossinline block: suspend (O1, O2) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2> API22<APIType.Post, I1, I2, O1, O2>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1, O2) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2> API32<APIType.Post, I1, I2, I3, O1, O2>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1, O2) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2> API42<APIType.Post, I1, I2, I3, I4, O1, O2>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1, O2) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2> API52<APIType.Post, I1, I2, I3, I4, I5, O1, O2>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1, O2) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
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
        .request(crossinline block: suspend (O1, O2, O3) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {

    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }

suspend inline fun <reified I1, reified O1, reified O2, reified O3> API13<APIType.Post, I1, O1, O2, O3>
        .request(i1: I1, crossinline block: suspend (O1, O2, O3) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3> API23<APIType.Post, I1, I2, O1, O2, O3>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1, O2, O3) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3> API33<APIType.Post, I1, I2, I3, O1, O2, O3>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1, O2, O3) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3> API43<APIType.Post, I1, I2, I3, I4, O1, O2, O3>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1, O2, O3) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3> API53<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1, O2, O3) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
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
        .request(crossinline block: suspend (O1, O2, O3, O4) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {

    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }

suspend inline fun <reified I1, reified O1, reified O2, reified O3, reified O4> API14<APIType.Post, I1, O1, O2, O3, O4>
        .request(i1: I1, crossinline block: suspend (O1, O2, O3, O4) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4> API24<APIType.Post, I1, I2, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1, O2, O3, O4) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4> API34<APIType.Post, I1, I2, I3, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1, O2, O3, O4) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4> API44<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1, O2, O3, O4) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4> API54<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1, O2, O3, O4) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
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
        .request(crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {

    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }

suspend inline fun <reified I1, reified O1, reified O2, reified O3, reified O4, reified O5> API15<APIType.Post, I1, O1, O2, O3, O4, O5>
        .request(i1: I1, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4, reified O5> API25<APIType.Post, I1, I2, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4, reified O5> API35<APIType.Post, I1, I2, I3, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4, reified O5> API45<APIType.Post, I1, I2, I3, I4, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4, reified O5> API55<APIType.Post, I1, I2, I3, I4, I5, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit): Throwable? =
    internalRequest({ setBody(makeArray {
        add(i1.toJson())
        add(i2.toJson())
        add(i3.toJson())
        add(i4.toJson())
        add(i5.toJson())
    }) }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }