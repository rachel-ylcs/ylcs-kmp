package love.yinlin.fs

import kotlinx.io.RawSink

@PublishedApi
internal actual suspend fun awaitFileSinkClosed(sink: RawSink, failure: Throwable?) { }