package love.yinlin.picker

import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.data.MimeType
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.io.Sources
import love.yinlin.uri.ImplicitUri

/** 系统选择器。调用方负责关闭返回的流；关闭选择器不会关闭已交付的流。 */
expect class Picker(context: PlatformContextProvider) : AutoCloseable {
    override fun close()

    suspend fun pickPicture(): Source?
    suspend fun pickPicture(maxNum: Int): Sources<Source>?
    suspend fun pickFile(mimeType: List<String> = [], filter: List<String> = []): Source?
    suspend fun pickPath(mimeType: List<String> = [], filter: List<String> = []): ImplicitUri?
    suspend fun savePath(filename: String, mimeType: String = MimeType.ANY, filter: String = "*.*"): ImplicitUri?

    suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>?
    suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>?
    suspend fun actualSave(filename: String, origin: Any, sink: Sink)
    suspend fun cleanSave(origin: Any, result: Boolean)
}
