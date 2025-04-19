package love.yinlin.platform

import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import androidx.core.net.toUri

open class ContentPath(override val path: String) : ImplicitPath {
    override val source: Source get() = appNative.context.contentResolver.openInputStream(path.toUri())!!.asSource().buffered()
}