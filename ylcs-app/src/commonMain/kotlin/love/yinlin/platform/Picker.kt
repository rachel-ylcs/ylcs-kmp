package love.yinlin.platform

import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.extension.Sources

expect object PicturePicker {
    suspend fun pick(): Source?
    suspend fun pick(maxNum: Int): Sources<Source>?

    suspend fun prepareSave(filename: String): Pair<Any, Sink>?
    suspend fun actualSave(filename: String, origin: Any, sink: Sink)
    suspend fun cleanSave(origin: Any, result: Boolean)
}