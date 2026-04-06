package love.yinlin.foundation

import kotlin.coroutines.CoroutineContext

abstract class SyncStartupFactory<S : Startup> : StartupFactory<S> {
    override val dependencies: List<String> = emptyList()
    final override val dispatcher: CoroutineContext? = null
}