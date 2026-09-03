package love.yinlin.cs

import io.ktor.http.*
import io.ktor.server.response.respond
import io.ktor.server.response.respondSource
import io.ktor.server.routing.*
import io.ktor.util.AttributeKey
import io.ktor.utils.io.charsets.Charsets
import love.yinlin.fs.File
import love.yinlin.fs.PlatformFileSystem
import kotlin.collections.firstOrNull

private object TailcardSelector : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Success(quality = RouteSelectorEvaluation.qualityTailcard)

    override fun toString(): String = "(static-content)"
}

private const val pathParameterName = "static-content-path-parameter"

private fun ContentType.withCharsetUTF8IfNeeded(): ContentType = if (charset() != null) this else withCharset(Charsets.UTF_8)

private fun ContentType.matchApplicationTypeWithCharset(): Boolean = match(ContentType.Application.Any) && when {
    match(ContentType.Application.Atom) ||
            match(ContentType.Application.JavaScript) ||
            match(ContentType.Application.Rss) ||
            match(ContentType.Application.Xml) ||
            match(ContentType.Application.Xml_Dtd) -> true
    else -> false
}

internal fun Route.staticFiles(remotePath: String, dir: File): Route = createChild(TailcardSelector).route(remotePath) {
    route("{${pathParameterName}...}") {
        get {
            val relativePath = call.parameters.getAll(pathParameterName)?.joinToString(PlatformFileSystem.PathSeparator.toString())
            if (relativePath != null) {
                val file = File(dir, relativePath)
                val requestedFile = if (file.isDirectory()) File(file, "index.html") else file
                if (requestedFile.exists()) {
                    val contentTypeList = ContentType.fromFileExtension(requestedFile.extension)
                    val type = contentTypeList.firstOrNull() ?: ContentType.Application.OctetStream
                    val contentType = when {
                        type.match(ContentType.Text.Any) -> type.withCharsetUTF8IfNeeded()
                        type.match(ContentType.Image.SVG) -> type.withCharsetUTF8IfNeeded()
                        type.matchApplicationTypeWithCharset() -> type.withCharsetUTF8IfNeeded()
                        else -> type
                    }

                    call.attributes.put(AttributeKey("StaticFileLocation"), requestedFile.path)
                    requestedFile.rawSource().use { source ->
                        call.respondSource(source, contentType, contentLength = requestedFile.fileSize())
                    }
                    return@get
                }
            }
            call.respond(HttpStatusCode.NotFound)
        }
    }
}