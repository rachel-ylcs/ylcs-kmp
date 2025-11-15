package love.yinlin.api

import io.ktor.client.call.body
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.serialization.json.JsonArray
import love.yinlin.extension.safeRawSources
import love.yinlin.extension.to
import love.yinlin.extension.toJsonString
import love.yinlin.io.Sources

class ClientAPIFile internal constructor(val value: Any) : APIFile {
    override val files: List<String> = emptyList()
}

fun apiFile(data: ByteArray): APIFile = ClientAPIFile(value = data)
fun apiFile(data: RawSource): APIFile = ClientAPIFile(value = data)
fun apiFile(data: Sources<RawSource>): APIFile = ClientAPIFile(value = data)
fun apiFile(data: List<Path>): APIFile? = if (data.isEmpty()) null else data.safeRawSources()?.let { ClientAPIFile(it) }

class APIFormScope {
    lateinit var builder: FormBuilder
    var index = 0
    val sources = Sources<RawSource>()

    private val headers = Headers.build { append(HttpHeaders.ContentDisposition, "filename=\"file\"") }

    fun internalAddByteArray(key: String, value: ByteArray) = builder.append(key = key, value = value, headers = headers)

    fun internalAddRawSource(key: String, value: RawSource) = builder.append(key = key, value = InputProvider { value.buffered() }, headers = headers)

    inline fun <reified I> add(i: I) {
        if (i is ClientAPIFile?) {
            if (i != null) {
                when (val value = i.value) {
                    is ByteArray -> internalAddByteArray("${index++}", value)
                    is RawSource -> internalAddRawSource("${index++}", value)
                    is Sources<out RawSource> -> value.forEachIndexed { i, source -> internalAddRawSource("${index++}:$i", source) }
                    else -> error("unsupported file type ${value::class.qualifiedName}")
                }
            }
            else builder.append(key = "#${index++}", value = "")
        }
        else builder.append(key = "${index++}", value = i.toJsonString())
    }

    suspend inline fun API<out APIType>.run(crossinline factory: APIFormScope.() -> Unit, crossinline block: suspend (HttpResponse) -> Unit): Throwable? {
        val formParts = formData {
            builder = this
            factory()
        }
        return sources.use {
            internalRequest(
                builder = { setBody(MultiPartFormDataContent(formParts)) },
                uploadFile = true,
                block = block
            )
        }
    }
}

suspend inline fun <reified I1> API10<APIType.Form, I1>
        .request(i1: I1, crossinline block: suspend () -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
    }) {
        block()
    }
}

suspend inline fun <reified I1, reified I2> API20<APIType.Form, I1, I2>
        .request(i1: I1, i2: I2, crossinline block: suspend () -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
    }) {
        block()
    }
}

suspend inline fun <reified I1, reified I2, reified I3> API30<APIType.Form, I1, I2, I3>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend () -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
    }) {
        block()
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified I4> API40<APIType.Form, I1, I2, I3, I4>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend () -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
        add(i4)
    }) {
        block()
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5> API50<APIType.Form, I1, I2, I3, I4, I5>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend () -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
        add(i4)
        add(i5)
    }) {
        block()
    }
}

suspend inline fun <reified I1, reified O1> API11<APIType.Form, I1, O1>
        .request(i1: I1, crossinline block: suspend (O1) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
    }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }
}

suspend inline fun <reified I1, reified I2, reified O1> API21<APIType.Form, I1, I2, O1>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
    }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified O1> API31<APIType.Form, I1, I2, I3, O1>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
    }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1> API41<APIType.Form, I1, I2, I3, I4, O1>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
        add(i4)
    }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1> API51<APIType.Form, I1, I2, I3, I4, I5, O1>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
        add(i4)
        add(i5)
    }) {
        val (o1) = it.body<JsonArray>()
        block(o1.to())
    }
}

suspend inline fun <reified I1, reified O1, reified O2> API12<APIType.Form, I1, O1, O2>
        .request(i1: I1, crossinline block: suspend (O1, O2) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
    }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }
}

suspend inline fun <reified I1, reified I2, reified O1, reified O2> API22<APIType.Form, I1, I2, O1, O2>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1, O2) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
    }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2> API32<APIType.Form, I1, I2, I3, O1, O2>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1, O2) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
    }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2> API42<APIType.Form, I1, I2, I3, I4, O1, O2>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1, O2) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
        add(i4)
    }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2> API52<APIType.Form, I1, I2, I3, I4, I5, O1, O2>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1, O2) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
        add(i4)
        add(i5)
    }) {
        val (o1, o2) = it.body<JsonArray>()
        block(o1.to(), o2.to())
    }
}

suspend inline fun <reified I1, reified O1, reified O2, reified O3> API13<APIType.Form, I1, O1, O2, O3>
        .request(i1: I1, crossinline block: suspend (O1, O2, O3) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
    }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }
}

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3> API23<APIType.Form, I1, I2, O1, O2, O3>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1, O2, O3) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
    }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3> API33<APIType.Form, I1, I2, I3, O1, O2, O3>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1, O2, O3) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
    }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3> API43<APIType.Form, I1, I2, I3, I4, O1, O2, O3>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1, O2, O3) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
        add(i4)
    }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3> API53<APIType.Form, I1, I2, I3, I4, I5, O1, O2, O3>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1, O2, O3) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
        add(i4)
        add(i5)
    }) {
        val (o1, o2, o3) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to())
    }
}

suspend inline fun <reified I1, reified O1, reified O2, reified O3, reified O4> API14<APIType.Form, I1, O1, O2, O3, O4>
        .request(i1: I1, crossinline block: suspend (O1, O2, O3, O4) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
    }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }
}

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4> API24<APIType.Form, I1, I2, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1, O2, O3, O4) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
    }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4> API34<APIType.Form, I1, I2, I3, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1, O2, O3, O4) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
    }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4> API44<APIType.Form, I1, I2, I3, I4, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1, O2, O3, O4) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
        add(i4)
    }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4> API54<APIType.Form, I1, I2, I3, I4, I5, O1, O2, O3, O4>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1, O2, O3, O4) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
        add(i4)
        add(i5)
    }) {
        val (o1, o2, o3, o4) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to())
    }
}

suspend inline fun <reified I1, reified O1, reified O2, reified O3, reified O4, reified O5> API15<APIType.Form, I1, O1, O2, O3, O4, O5>
        .request(i1: I1, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
    }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }
}

suspend inline fun <reified I1, reified I2, reified O1, reified O2, reified O3, reified O4, reified O5> API25<APIType.Form, I1, I2, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
    }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified O1, reified O2, reified O3, reified O4, reified O5> API35<APIType.Form, I1, I2, I3, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, i3: I3, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
    }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified O1, reified O2, reified O3, reified O4, reified O5> API45<APIType.Form, I1, I2, I3, I4, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
        add(i4)
    }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }
}

suspend inline fun <reified I1, reified I2, reified I3, reified I4, reified I5, reified O1, reified O2, reified O3, reified O4, reified O5> API55<APIType.Form, I1, I2, I3, I4, I5, O1, O2, O3, O4, O5>
        .request(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, crossinline block: suspend (O1, O2, O3, O4, O5) -> Unit): Throwable? = with(APIFormScope()) {
    run(factory = {
        add(i1)
        add(i2)
        add(i3)
        add(i4)
        add(i5)
    }) {
        val (o1, o2, o3, o4, o5) = it.body<JsonArray>()
        block(o1.to(), o2.to(), o3.to(), o4.to(), o5.to())
    }
}