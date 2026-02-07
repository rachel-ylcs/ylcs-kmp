package love.yinlin.coroutines

import kotlin.coroutines.CoroutineContext

expect val mainContext: CoroutineContext
expect val cpuContext: CoroutineContext
expect val ioContext: CoroutineContext
expect val waitContext: CoroutineContext