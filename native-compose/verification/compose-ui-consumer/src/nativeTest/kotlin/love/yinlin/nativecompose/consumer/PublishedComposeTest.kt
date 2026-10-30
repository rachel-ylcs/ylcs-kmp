package love.yinlin.nativecompose.consumer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runPublishedComposeTest(block: suspend TestScope.() -> Unit) {
    // Offscreen scenes have no SDL event loop. Lifecycle's synchronous Main probe must run inline.
    Dispatchers.setMain(UnconfinedTestDispatcher())
    try {
        runTest(timeout = 30.seconds, testBody = block)
    }
    finally { Dispatchers.resetMain() }
}
