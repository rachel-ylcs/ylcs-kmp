package love.yinlin.startup

import kotlinx.coroutines.CoroutineScope
import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.data.MimeType
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.io.Sources
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.SyncStartup
import love.yinlin.uri.ImplicitUri

expect class StartupPicker(context: PlatformContextProvider) : SyncStartup {
    override fun init(scope: CoroutineScope, args: StartupArgs)

    suspend fun pickPicture(): Source?
    suspend fun pickPicture(maxNum: Int): Sources<Source>?
    suspend fun pickFile(mimeType: List<String> = emptyList(), filter: List<String> = emptyList()): Source?
    suspend fun pickPath(mimeType: List<String> = emptyList(), filter: List<String> = emptyList()): ImplicitUri?
    suspend fun savePath(filename: String, mimeType: String = MimeType.ANY, filter: String = "*.*"): ImplicitUri?

    suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>?
    suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>?
    suspend fun actualSave(filename: String, origin: Any, sink: Sink)
    suspend fun cleanSave(origin: Any, result: Boolean)
}