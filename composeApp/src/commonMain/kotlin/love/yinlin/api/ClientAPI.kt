package love.yinlin.api

import com.eygraber.uri.UriCodec
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.Local
import love.yinlin.data.Data
import love.yinlin.data.Failed
import love.yinlin.extension.*
import love.yinlin.platform.app
import love.yinlin.platform.safeCall
import love.yinlin.platform.safeCallData
import kotlin.jvm.JvmName

object ClientAPI {
	@JvmName("processResponse")
	suspend inline fun <reified Response : Any> processResponse(response: HttpResponse): Data<Response> {
		val json = response.body<JsonObject>()
		val code = json["code"].Int
		val msg = json["msg"].StringNull
		return if (code == APICode.SUCCESS) Data.Success(json["data"]!!.to(), msg)
			else Data.Error(Failed.RequestError.InvalidArgument, msg)
	}

	@JvmName("processResponseDefault")
	suspend fun processResponse(response: HttpResponse) : Data<Response.Default> {
		val json = response.body<JsonObject>()
		val code = json["code"].Int
		val msg = json["msg"].StringNull
		return if (code == APICode.SUCCESS) Data.Success(Response.Default, msg)
			else Data.Error(Failed.RequestError.InvalidArgument, msg)
	}

	fun buildGetParameters(argsMap: JsonObject): String {
		val args = StringBuilder()
		for ((key, value) in argsMap) args.append("$key=${UriCodec.encode(value.String)}&")
		return if (args.isNotEmpty()) "?${args.dropLast(1)}" else ""
	}

	@JvmName("requestGet")
	suspend inline fun <reified Request : Any, reified Response : Any> request(
		route: APIRoute<Request, Response, NoFiles, APIMethod.Get>,
		data: Request
	): Data<Response> = app.client.safeCallData { client ->
		client.prepareGet(urlString = "${Local.ClientUrl}$route${buildGetParameters(data.toJson().Object)}")
			.execute { processResponse<Response>(it) }
	}

	@JvmName("requestGetRequest")
	suspend inline fun <reified Request : Any> request(
		route: APIRoute<Request, Response.Default, NoFiles, APIMethod.Get>,
		data: Request
	) : Data<Response.Default> = app.client.safeCallData { client ->
		client.prepareGet(urlString = "${Local.ClientUrl}$route${buildGetParameters(data.toJson().Object)}")
			.execute { processResponse(it) }
	}

	@JvmName("requestGetResponse")
	suspend inline fun <reified Response : Any> request(
		route: APIRoute<Request.Default, Response, NoFiles, APIMethod.Get>
	): Data<Response> = app.client.safeCallData { client ->
		client.prepareGet(urlString = "${Local.ClientUrl}$route")
			.execute { processResponse<Response>(it) }
	}

	@JvmName("requestPost")
	suspend inline fun <reified Request : Any, reified Response : Any> request(
		route: APIRoute<Request, Response, NoFiles, APIMethod.Post>,
		data: Request
	): Data<Response> = app.client.safeCallData { client ->
		client.preparePost(urlString = "${Local.ClientUrl}$route") { setBody(data) }
			.execute { processResponse<Response>(it) }
	}

	@JvmName("requestPostRequest")
	suspend inline fun <reified Request : Any> request(
		route: APIRoute<Request, Response.Default, NoFiles, APIMethod.Post>,
		data: Request
	): Data<Response.Default> = app.client.safeCallData { client ->
		client.preparePost(urlString = "${Local.ClientUrl}$route") { setBody(data) }
			.execute { processResponse(it) }
	}

	@JvmName("requestPostResponse")
	suspend inline fun <reified Response : Any> request(
		route: APIRoute<Request.Default, Response, NoFiles, APIMethod.Post>
	): Data<Response> = app.client.safeCallData { client ->
		client.preparePost(urlString = "${Local.ClientUrl}$route") { setBody(Request.Default) }
			.execute { processResponse<Response>(it) }
	}

	interface APIFileScope {
		fun file(value: Path): APIFile
		fun optionFile(value: Path?): APIFile?
		fun file(values: List<Path>?): APIFiles
	}

	fun FormBuilder.addFormFile(key: String, file: Path) = this.append(
		key = key,
		value = InputProvider { SystemFileSystem.source(file).buffered() },
		headers = Headers.build {
			append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
		}
	)

	inline fun <reified Files : Any> buildFormFiles(
		builder: FormBuilder,
		crossinline files: APIFileScope.() -> Files
	) {
		val ignoreFile = mutableListOf<String>()
		val ignoreFiles = mutableListOf<String>()

		val scope = object : APIFileScope {
			private var index = 0
			val map = mutableMapOf<String, Any?>()

			private fun buildFile(value: Path?): APIFile {
				val key = "#${index++}#"
				map[key] = value
				return APIFile(key)
			}

			override fun file(value: Path): APIFile = buildFile(value)
			override fun optionFile(value: Path?): APIFile? = buildFile(value)
			override fun file(values: List<Path>?): APIFiles = buildList {
				val key = "#${index++}#"
				map[key] = values?.ifEmpty { null }
				add(APIFile(key))
			}
		}

		val keys = scope.files().toJson().Object
		for ((name, item) in keys) {
			if (item is JsonArray) {
				val value = scope.map[item.first().String]
				if (value == null) ignoreFiles += name
				else (value as List<*>).forEachIndexed { index, item ->
					builder.addFormFile("#$name!$index", item as Path)
				}
			}
			else {
				val value = scope.map[item.String]
				if (value == null) ignoreFile += name
				else builder.addFormFile(name, value as Path)
			}
		}

		if (ignoreFile.isNotEmpty()) builder.append(key = "#ignoreFile#", value = ignoreFile.toJsonString())
		if (ignoreFiles.isNotEmpty()) builder.append(key = "#ignoreFiles#", value = ignoreFiles.toJsonString())
	}

	@JvmName("requestForm")
	suspend inline fun <reified Request : Any, reified Response : Any, reified Files : Any> request(
		route: APIRoute<Request, Response, Files, APIMethod.Form>,
		data: Request,
		crossinline files: APIFileScope.() -> Files
	): Data<Response> = app.fileClient.safeCallData { client ->
		client.preparePost(urlString = "${Local.ClientUrl}$route") {
			setBody(MultiPartFormDataContent(formData {
				buildFormFiles(this, files)
				append(key = "#data#", value = data.toJsonString())
			}))
		}.execute { processResponse<Response>(it) }
	}

	@JvmName("requestFormRequest")
	suspend inline fun <reified Request : Any, reified Files : Any> request(
		route: APIRoute<Request, Response.Default, Files, APIMethod.Form>,
		data: Request,
		crossinline files: APIFileScope.() -> Files
	): Data<Response.Default> = app.fileClient.safeCallData { client ->
		client.preparePost(urlString = "${Local.ClientUrl}$route") {
			setBody(MultiPartFormDataContent(formData {
				buildFormFiles(this, files)
				append(key = "#data#", value = data.toJsonString())
			}))
		}.execute { processResponse(it) }
	}

	@JvmName("requestFormResponse")
	suspend inline fun <reified Response : Any, reified Files : Any> request(
		route: APIRoute<Request.Default, Response, Files, APIMethod.Form>,
		crossinline files: APIFileScope.() -> Files
	): Data<Response> = app.fileClient.safeCallData { client ->
		client.preparePost(urlString = "${Local.ClientUrl}$route") {
			setBody(MultiPartFormDataContent(formData {
				buildFormFiles(this, files)
			}))
		}.execute { processResponse<Response>(it) }
	}

	@JvmName("requestServerResource")
	suspend inline fun <reified Response : Any> request(route: ResNode): Data<Response> = app.client.safeCall { client ->
		client.prepareGet(urlString = "${Local.ClientUrl}/$route").execute { it.body() }
	}
}