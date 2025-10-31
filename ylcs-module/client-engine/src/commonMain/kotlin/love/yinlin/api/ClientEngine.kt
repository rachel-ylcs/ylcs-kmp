package love.yinlin.api

import io.ktor.client.call.body
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.Data
import love.yinlin.data.RequestError
import love.yinlin.extension.Int
import love.yinlin.extension.Object
import love.yinlin.extension.String
import love.yinlin.extension.StringNull
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.extension.toJsonString
import love.yinlin.io.Sources
import love.yinlin.platform.NetClient
import love.yinlin.platform.safeCall
import love.yinlin.platform.safeCallData
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.jvm.JvmName

open class ClientEngine(val baseUrl: String) {
    suspend inline fun <reified Response : Any> baseProcessResponse(response: HttpResponse): Data<Response> {
        val json = response.body<JsonObject>()
        val code = json["code"].Int
        val msg = json["msg"].StringNull
        return when (code) {
            APICode.SUCCESS -> Data.Success(json["data"]!!.to(), msg)
            APICode.FAILED -> Data.Failure(RequestError.InvalidArgument, msg)
            APICode.UNAUTHORIZED -> Data.Failure(RequestError.Unauthorized, msg)
            else -> Data.Failure(RequestError.ClientError, msg)
        }
    }

    suspend fun baseProcessResponseDefault(response: HttpResponse) : Data<Response.Default> {
        val json = response.body<JsonObject>()
        val code = json["code"].Int
        val msg = json["msg"].StringNull
        return when (code) {
            APICode.SUCCESS -> Data.Success(Response.Default, msg)
            APICode.FAILED -> Data.Failure(RequestError.InvalidArgument, msg)
            APICode.UNAUTHORIZED -> Data.Failure(RequestError.Unauthorized, msg)
            else -> Data.Failure(RequestError.ClientError, msg)
        }
    }

    fun baseBuildGetParameters(argsMap: JsonObject): String = buildString {
        if (argsMap.isEmpty()) return@buildString
        append("?")
        for ((key, value) in argsMap) append("$key=${value.String}&")
        dropLast(1)
    }

    @JvmName("requestGet")
    suspend inline fun <reified Request : Any, reified Response : Any> request(
        route: APIRoute<Request, Response, NoFiles, APIMethod.Get>,
        data: Request
    ): Data<Response> = NetClient.common.safeCallData { client ->
        client.prepareGet(urlString = "$baseUrl$route${baseBuildGetParameters(data.toJson().Object)}")
            .execute { baseProcessResponse<Response>(it) }
    }

    @JvmName("requestGetRequest")
    suspend inline fun <reified Request : Any> request(
        route: APIRoute<Request, Response.Default, NoFiles, APIMethod.Get>,
        data: Request
    ) : Data<Response.Default> = NetClient.common.safeCallData { client ->
        client.prepareGet(urlString = "$baseUrl$route${baseBuildGetParameters(data.toJson().Object)}")
            .execute { baseProcessResponseDefault(it) }
    }

    @JvmName("requestGetResponse")
    suspend inline fun <reified Response : Any> request(
        route: APIRoute<Request.Default, Response, NoFiles, APIMethod.Get>
    ): Data<Response> = NetClient.common.safeCallData { client ->
        client.prepareGet(urlString = "$baseUrl$route")
            .execute { baseProcessResponse<Response>(it) }
    }

    @JvmName("requestPost")
    suspend inline fun <reified Request : Any, reified Response : Any> request(
        route: APIRoute<Request, Response, NoFiles, APIMethod.Post>,
        data: Request
    ): Data<Response> = NetClient.common.safeCallData { client ->
        client.preparePost(urlString = "$baseUrl$route") { setBody(data) }
            .execute { baseProcessResponse<Response>(it) }
    }

    @JvmName("requestPostRequest")
    suspend inline fun <reified Request : Any> request(
        route: APIRoute<Request, Response.Default, NoFiles, APIMethod.Post>,
        data: Request
    ): Data<Response.Default> = NetClient.common.safeCallData { client ->
        client.preparePost(urlString = "$baseUrl$route") { setBody(data) }
            .execute { baseProcessResponseDefault(it) }
    }

    @JvmName("requestPostResponse")
    suspend inline fun <reified Response : Any> request(
        route: APIRoute<Request.Default, Response, NoFiles, APIMethod.Post>
    ): Data<Response> = NetClient.common.safeCallData { client ->
        client.preparePost(urlString = "$baseUrl$route") { setBody(Request.Default) }
            .execute { baseProcessResponse<Response>(it) }
    }

    interface APIFileScope {
        fun file(value: Nothing?): APIFile
        fun file(value: ByteArray?): APIFile
        fun file(value: RawSource?): APIFile
        fun file(values: Sources<RawSource>?): APIFiles
    }

    fun FormBuilder.addByteArrayFile(key: String, file: ByteArray) = this.append(
        key = key,
        value = file,
        headers = Headers.build {
            append(HttpHeaders.ContentDisposition, "filename=\"$key\"")
        }
    )

    fun FormBuilder.addFormFile(key: String, source: RawSource) = this.append(
        key = key,
        value = InputProvider { source.buffered() },
        headers = Headers.build {
            append(HttpHeaders.ContentDisposition, "filename=\"$key\"")
        }
    )

    class ActualAPIFileScope : APIFileScope {
        private var index = 0
        val map = mutableMapOf<String, Any?>()
        val sources = Sources<RawSource>()

        private fun addFile(value: Any?): APIFile {
            val key = "#${index++}#"
            map[key] = value
            return key
        }

        override fun file(value: Nothing?): APIFile = addFile(value)
        override fun file(value: ByteArray?): APIFile = addFile(value)
        override fun file(value: RawSource?): APIFile {
            if (value != null) sources += value
            return addFile(value)
        }
        override fun file(values: Sources<RawSource>?): APIFiles = listOf(addFile(values?.let { v ->
            v.forEach { sources += it }
            if (v.isEmpty()) null else v
        }))
    }

    inline fun <reified Files : Any> buildFormFiles(
        fileScope: ActualAPIFileScope,
        builder: FormBuilder,
        crossinline files: APIFileScope.() -> Files
    ) {
        val ignoreFile = mutableListOf<String>()
        val ignoreFiles = mutableListOf<String>()

        val keys = fileScope.files().toJson().Object
        for ((name, item) in keys) {
            if (item is JsonArray) {
                val value = fileScope.map[item.first().String]
                if (value == null) ignoreFiles += name
                else (value as? Sources<*>)?.forEachIndexed { index, item ->
                    builder.addFormFile("#$name!$index", item)
                }
            }
            else {
                val value = fileScope.map[item.String]
                if (value == null) ignoreFile += name
                else {
                    if (value is RawSource) builder.addFormFile(name, value)
                    else if (value is ByteArray) builder.addByteArrayFile(name, value)
                }
            }
        }

        if (ignoreFile.isNotEmpty()) builder.append(key = "#ignoreFile#", value = ignoreFile.toJsonString())
        if (ignoreFiles.isNotEmpty()) builder.append(key = "#ignoreFiles#", value = ignoreFiles.toJsonString())
    }

    suspend inline fun <reified Request : Any, reified Response : Any, reified Files : Any> buildFormAndClean(
        route: APIRoute<Request, Response, Files, APIMethod.Form>,
        data: Request?,
        crossinline files: APIFileScope.() -> Files,
        crossinline responseBuilder: suspend (HttpResponse) -> Data<Response>
    ): Data<Response> = NetClient.file.safeCallData { client ->
        val fileScope = ActualAPIFileScope()
        fileScope.sources.use {
            val formParts = formData {
                buildFormFiles(fileScope, this, files)
                if (data != null) append(key = "#data#", value = data.toJsonString())
            }
            client.preparePost(urlString = "$baseUrl$route") {
                setBody(MultiPartFormDataContent(formParts))
            }.execute { responseBuilder(it) }
        }
    }

    @JvmName("requestForm")
    suspend inline fun <reified Request : Any, reified Response : Any, reified Files : Any> request(
        route: APIRoute<Request, Response, Files, APIMethod.Form>,
        data: Request,
        crossinline files: APIFileScope.() -> Files
    ): Data<Response> = buildFormAndClean(route = route, data = data, files = files) { baseProcessResponse<Response>(it) }

    @JvmName("requestFormRequest")
    suspend inline fun <reified Request : Any, reified Files : Any> request(
        route: APIRoute<Request, Response.Default, Files, APIMethod.Form>,
        data: Request,
        crossinline files: APIFileScope.() -> Files
    ): Data<Response.Default> = buildFormAndClean(route = route, data = data, files = files) { baseProcessResponseDefault(it) }

    @JvmName("requestFormResponse")
    suspend inline fun <reified Response : Any, reified Files : Any> request(
        route: APIRoute<Request.Default, Response, Files, APIMethod.Form>,
        crossinline files: APIFileScope.() -> Files
    ): Data<Response> = buildFormAndClean(route = route, data = null, files = files) { baseProcessResponse<Response>(it) }

    @JvmName("requestServerResource")
    suspend inline fun <reified Response : Any> request(route: ResNode): Data<Response> = NetClient.common.safeCall { client ->
        client.prepareGet(urlString = "$baseUrl/$route").execute { it.body() }
    }
}