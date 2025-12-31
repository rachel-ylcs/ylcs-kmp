package love.yinlin.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlin.coroutines.CoroutineContext

actual val mainContext: CoroutineContext get() = Dispatchers.Main
actual val cpuContext: CoroutineContext get() = Dispatchers.Default
actual val ioContext: CoroutineContext get() = Dispatchers.IO
actual val waitContext: CoroutineContext get() = NonCancellable