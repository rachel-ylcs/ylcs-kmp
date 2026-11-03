package love.yinlin.foundation.picker

import kotlinx.io.RawSource
import love.yinlin.extension.catching
import love.yinlin.extension.cleaning
import love.yinlin.fs.File

internal class TemporaryPictureSource(private val file: File, private val delegate: RawSource) : RawSource by delegate {
    override fun close() {
        cleaning({ delegate.close() }) {
            catching { file.deleteSync() }
        }
    }
}