package love.yinlin.platform

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

interface ImplicitPath {
    val path: String
    val source: Source
    val sink: Sink
}

open class NormalPath(override val path: String) : ImplicitPath {
    override val source: Source get() = SystemFileSystem.source(Path(path)).buffered()
    override val sink: Sink get() = SystemFileSystem.sink(Path(path)).buffered()
}