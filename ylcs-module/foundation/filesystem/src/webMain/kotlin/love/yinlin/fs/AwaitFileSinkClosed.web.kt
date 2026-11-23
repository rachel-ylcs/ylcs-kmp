package love.yinlin.fs

import kotlinx.io.RawSink
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingError
import love.yinlin.extension.then
import love.yinlin.io.WebFileSink

@PublishedApi
internal actual suspend fun awaitFileSinkClosed(sink: RawSink, failure: Throwable?) {
    if (sink !is WebFileSink) return

    catchingError {
        Coroutines.wait {
            if (failure == null) sink.commit()
            else sink.abort()
            sink.awaitClosed()
        }
    }?.then { e ->
        if (failure == null) throw e
        if (failure !== e) failure.addSuppressed(e)
    }
}

@PublishedApi
internal actual fun manageFileSink(sink: RawSink) {
    (sink as? WebFileSink)?.deferCommit()
}
