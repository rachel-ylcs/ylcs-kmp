package love.yinlin.api

import androidx.core.uri.UriUtils
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.preparePost
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import love.yinlin.data.Data
import love.yinlin.data.RequestError
import love.yinlin.extension.Int
import love.yinlin.extension.Json
import love.yinlin.extension.Object
import love.yinlin.extension.ObjectNull
import love.yinlin.extension.String
import love.yinlin.extension.StringNull
import love.yinlin.extension.makeObject
import love.yinlin.platform.app
import kotlin.coroutines.cancellation.CancellationException

object API {
	const val HOST = "yinlin.love"

	suspend inline fun <reified Response : Any> processResponse(response: HttpResponse): Data<Response> {
		val json = response.body<JsonObject>()
		val code = json["code"].Int
		val msg = json["msg"].StringNull
		val data = json["data"].ObjectNull
		return if (code == APICode.SUCCESS) {
			if (data != null) Data.Success(Json.decodeFromJsonElement(data), msg)
			else Data.Success(Json.decodeFromString("{}"), msg)
		}
		else Data.Error(RequestError.InvalidArgument, msg)
	}

	suspend inline fun <reified Request : Any, reified Response : Any> request(
		route: APIRoute<Request, Response>,
		data: Request
	): Data<Response> {
		val client = if (route.method == APIMethod.Form) app.fileClient else app.client
		return try {
			var url = "https://$HOST${route.path}"
			if (route.method == APIMethod.Get) {
				val args = StringBuilder()
				val argsMap = if (data !is Default.Request) Json.encodeToJsonElement(data).Object else makeObject { }
				for ((key, value) in argsMap) args.append("$key=${UriUtils.encode(value.String)}&")
				if (args.isNotEmpty()) url += "?${args.dropLast(1)}"
			}
			client.prepareRequest(urlString = url) {
				method = when (route.method) {
					APIMethod.Get -> HttpMethod.Get
					APIMethod.Post -> HttpMethod.Post
					APIMethod.Form -> HttpMethod.Post
				}
				if (route.method == APIMethod.Post) setBody(data)
			}.execute { processResponse(it) }
		}
		catch (e: HttpRequestTimeoutException) {
			Data.Error(RequestError.Timeout, "网络连接超时", e)
		}
		catch (e: CancellationException) {
			Data.Error(RequestError.Canceled, "操作取消", e)
		}
		catch (e: Exception) {
			Data.Error(RequestError.ClientError, "未知异常", e)
		}
	}

	class PostFormScope(internal val builder: FormBuilder) {
		infix fun String.to(text: String) = builder.append(
			key = this,
			value = text
		)

		infix fun String.to(file: Path) = builder.append(
			key = this,
			value = InputProvider { SystemFileSystem.source(file).buffered() },
			headers = Headers.build {
				append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
			}
		)
	}

	suspend inline fun <reified Request : Any, reified Response : Any> request(
		route: APIRoute<Request, Response>,
		crossinline init: PostFormScope.() -> Unit
	): Data<Response> = try {
		app.fileClient.preparePost(urlString = route.path) {
			setBody(MultiPartFormDataContent(formData {
				PostFormScope(this).init()
			}))
		}.execute { processResponse(it) }
	}
	catch (e: HttpRequestTimeoutException) {
		Data.Error(RequestError.Timeout, "网络连接超时", e)
	}
	catch (e: CancellationException) {
		Data.Error(RequestError.Canceled, "操作取消", e)
	}
	catch (e: Exception) {
		Data.Error(RequestError.ClientError, "未知异常", e)
	}
}