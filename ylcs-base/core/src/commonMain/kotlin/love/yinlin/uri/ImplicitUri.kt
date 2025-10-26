package love.yinlin.uri

import kotlinx.io.Sink
import kotlinx.io.Source

interface ImplicitUri {
    val path: String
    val source: Source
    val sink: Sink
}