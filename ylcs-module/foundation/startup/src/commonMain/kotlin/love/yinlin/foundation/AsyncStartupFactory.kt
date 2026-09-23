package love.yinlin.foundation

import love.yinlin.coroutines.cpuContext
import kotlin.coroutines.CoroutineContext

abstract class AsyncStartupFactory<S : Startup> : StartupFactory<S> {
    override val dependencies: List<String> = []
    override val dispatcher: CoroutineContext = cpuContext
}