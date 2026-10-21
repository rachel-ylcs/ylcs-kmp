package love.yinlin.fs

import kotlinx.io.RawSink

/**
 * [RawSink.close] 是同步接口；浏览器文件提交必须额外等待其异步关闭结果。
 */
@PublishedApi
internal expect suspend fun awaitFileSinkClosed(sink: RawSink, failure: Throwable?)