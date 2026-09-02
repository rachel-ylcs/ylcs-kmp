package love.yinlin.cs

import io.ktor.http.ContentType
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.fromFileExtension
import io.ktor.http.withCharset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.util.AttributeKey
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.buffered
import love.yinlin.fs.File
import love.yinlin.fs.PlatformFileSystem
import kotlin.collections.firstOrNull

private object TailcardSelector : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Success(quality = RouteSelectorEvaluation.qualityTailcard)

    override fun toString(): String = "(static-content)"
}

private val StaticFileLocationProperty: AttributeKey<String> = AttributeKey("StaticFileLocation")

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

private class BoundedRawSource(private val delegate: RawSource, private var remaining: Long) : RawSource {
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        require(byteCount >= 0L) { "byteCount must be >= 0, but was $byteCount" }
        if (remaining <= 0L) return -1L
        val toRead = minOf(byteCount, remaining)
        val bytesRead = delegate.readAtMostTo(sink, toRead)
        if (bytesRead > 0L) remaining -= bytesRead
        return bytesRead
    }
    override fun close() = delegate.close()
}

private class LocalFileContent(val file: File, override val contentType: ContentType) : OutgoingContent.ReadChannelContent() {
    override val contentLength: Long get() = file.fileSizeSync()

    init {
        if (!file.existsSync()) throw IOException("No such file ${file.path}")
    }

    override fun readFrom(): ByteReadChannel = ByteReadChannel(file.bufferedSourceSync())

    override fun readFrom(range: LongRange): ByteReadChannel {
        val source = file.bufferedSourceSync()
        if (range.first > 0) source.skip(range.first)
        val length = range.last - range.first + 1
        return ByteReadChannel(BoundedRawSource(source, length).buffered())
    }
}

private suspend fun respondStaticFile(call: ApplicationCall, requestedFile: File) {
    call.attributes.put(StaticFileLocationProperty, requestedFile.path)

    val contentTypeList = ContentType.fromFileExtension(requestedFile.extension)
    val type = contentTypeList.firstOrNull() ?: ContentType.Application.OctetStream
    val contentType = when {
        type.match(ContentType.Text.Any) -> type.withCharsetUTF8IfNeeded()
        type.match(ContentType.Image.SVG) -> type.withCharsetUTF8IfNeeded()
        type.matchApplicationTypeWithCharset() -> type.withCharsetUTF8IfNeeded()
        else -> type
    }

    if (requestedFile.isFile()) call.respond(LocalFileContent(requestedFile, contentType))
}

internal fun Route.staticFiles(remotePath: String, dir: File): Route = createChild(TailcardSelector).route(remotePath) {
    route("{${pathParameterName}...}") {
        get {
            val relativePath = call.parameters.getAll(pathParameterName)?.joinToString(PlatformFileSystem.PathSeparator.toString()) ?: return@get
            val requestedFile = File(dir, relativePath)
            val actualFile = if (requestedFile.isDirectory()) File(requestedFile, "index.html") else requestedFile
            respondStaticFile(call, actualFile)
        }
    }
}