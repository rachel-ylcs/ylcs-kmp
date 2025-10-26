package love.yinlin.common.uri

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

open class RegularUri(override val path: String) : ImplicitUri {
    override val source: Source get() = SystemFileSystem.source(Path(path)).buffered()
    override val sink: Sink get() = SystemFileSystem.sink(Path(path)).buffered()
}