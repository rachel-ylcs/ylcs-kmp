package love.yinlin.foundation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import love.yinlin.extension.cleaning
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ReadOnlyProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestStartupDependencies {
    private class TestMap : StartupMap {
        private val items = mutableMapOf<String, Startup>()
        override fun get(id: String): Startup? = items[id]
        override fun set(id: String, value: Startup) { items[id] = value }
        override fun delegate(id: String): ReadOnlyProperty<Any?, Startup?> = ReadOnlyProperty { _, _ -> items[id] }
    }

    private class TestStartup(pool: StartupPool, private val name: String, private val events: MutableList<String>, private val gate: CompletableDeferred<Unit>?) : AsyncStartup(pool) {
        override suspend fun init() {
            events += "$name:init"
            gate?.await()
        }
        override suspend fun initLater() { events += "$name:later" }
        override fun destroy() { events += "$name:destroy" }
    }

    private class Factory(
        override val id: String,
        override val dependencies: List<String>,
        override val dispatcher: CoroutineContext,
        private val events: MutableList<String>,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : AsyncStartupFactory<TestStartup>() {
        override fun build(pool: StartupPool) = TestStartup(pool, id, events, gate)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun initializationAndLaterWorkRespectDependencies() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        cleaning({
            val events: MutableList<String> = []
            val gate = CompletableDeferred<Unit>()
            val pool = StartupPool(PlatformContext.Instance, TestMap())
            pool.startup(Factory("first", [], dispatcher, events, gate))
            pool.startup(Factory("second", ["first"], dispatcher, events))
            pool.initPool(this)
            runCurrent()
            assertEquals(["first:init"], events)
            pool.initPoolLater(this)
            runCurrent()
            assertEquals(["first:init"], events)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(4, events.size)
            assertTrue(events.indexOf("first:init") < events.indexOf("second:init"))
            assertTrue(events.indexOf("first:init") < events.indexOf("first:later"))
            assertTrue(events.indexOf("second:init") < events.indexOf("second:later"))
            pool.destroyPool()
            assertEquals(["second:destroy", "first:destroy"], events.takeLast(2))
        }, Dispatchers::resetMain)
    }
}