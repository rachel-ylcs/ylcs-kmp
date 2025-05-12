package love.yinlin.platform

import kotlinx.io.*
import androidx.core.net.toUri

open class ContentPath(override val path: String) : ImplicitPath {
    override val source: Source get() = appNative.context.contentResolver.openInputStream(path.toUri())!!.asSource().buffered()
    override val sink: Sink get() = appNative.context.contentResolver.openOutputStream(path.toUri())!!.asSink().buffered()
}