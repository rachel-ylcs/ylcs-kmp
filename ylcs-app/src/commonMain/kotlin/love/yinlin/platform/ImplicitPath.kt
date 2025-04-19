package love.yinlin.platform

import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

interface ImplicitPath {
    val path: String
    val source: Source
}

open class NormalPath(override val path: String) : ImplicitPath {
    override val source: Source get() = SystemFileSystem.source(Path(path)).buffered()
}