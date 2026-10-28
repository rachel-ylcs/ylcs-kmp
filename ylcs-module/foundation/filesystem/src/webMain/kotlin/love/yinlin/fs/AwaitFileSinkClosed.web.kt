package love.yinlin.fs

import kotlinx.io.RawSink
import love.yinlin.coroutines.Coroutines
import love.yinlin.io.WebFileSink

@PublishedApi
internal actual suspend fun awaitFileSinkClosed(sink: RawSink, failure: Throwable?) {
    try {
        Coroutines.wait {
            (sink as? WebFileSink)?.awaitClosed()
        }
    }
    catch (e: Throwable) {
        if (failure == null) throw e
        if (failure !== e) failure.addSuppressed(e)
    }
}