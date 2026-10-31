package love.yinlin.compose.screen

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.annotation.LooseTyped
import love.yinlin.reflect.metaClassName
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource

@OptIn(CompatibleRachelApi::class, LooseTyped::class, ExperimentalTime::class)
class ScreenNavigationDebounceTest {
    private open class ProbeScreen : TestScreen() {
        var initializeCount = 0
        var clearCount = 0

        override fun initialize() { ++initializeCount }
        override fun uninitialize() { ++clearCount }
    }

    private class HomeScreen : ProbeScreen()

    private class CounterScreen(initialValue: Int) : ProbeScreen() {
        var value = initialValue
        var resumeCount = 0
        var resumeAction: CounterScreen.() -> Unit = {}

        override fun resume() = withResume { next: Int ->
            value = next
            ++resumeCount
            resumeAction()
        }
    }

    private class RedirectScreen : ProbeScreen() {
        var popResult: Boolean? = null

        override fun initialize() {
            super.initialize()
            navigate(::CounterScreen, 99)
            popResult = pop()
        }
    }

    private val owners: MutableList<ViewModelStore> = []
    private val timeSource = TestTimeSource()
    private var counterFactoryCalls = 0
    private val registry = ScreenRegistry {
        screen(::HomeScreen)
        screen<CounterScreen, Int>({ value ->
            ++counterFactoryCalls
            CounterScreen(value)
        }, key = "counter")
        screen(::RedirectScreen)
    }

    private inline fun <reified S : ScreenModel> key(args: ScreenArgs = ScreenArgs.Empty): ScreenKey =
        ScreenKey(type = metaClassName<S>(), args = args)

    private fun createManager(initial: List<ScreenKey> = [key<HomeScreen>()]): ScreenManager {
        val owner = ViewModelStore().also { owners += it }
        return ViewModelProvider.create(
            store = owner,
            factory = viewModelFactory { addInitializer(ScreenManager::class) { ScreenManager(registry, initial, timeSource) } }
        )[TestManagerID, ScreenManager::class]
    }

    @AfterTest
    fun clearOwners() {
        for (owner in owners.asReversed()) owner.clear()
        owners.clear()
    }

    @Test
    fun navigateIgnoresRequestsUntilExactly200MillisecondsWithoutExtendingTheWindow() {
        val manager = createManager()
        manager.navigate(::CounterScreen, 1)
        val first = manager.requireTopScreen<CounterScreen>()

        timeSource += 100.milliseconds
        manager.navigate(::CounterScreen, 2)
        timeSource += 100.milliseconds - 1.nanoseconds
        manager.navigate(::CounterScreen, 3)

        assertEquals(2, manager.backStack.size)
        assertEquals(1, counterFactoryCalls)
        assertSame(first, manager.requireTopScreen<CounterScreen>())
        assertEquals(1, first.value)
        assertEquals(1, first.initializeCount)

        timeSource += 1.nanoseconds
        assertEquals(2, manager.backStack.size)
        assertEquals(1, counterFactoryCalls)
        manager.navigate(::CounterScreen, 4)

        assertEquals(3, manager.backStack.size)
        assertEquals(2, counterFactoryCalls)
        assertEquals(4, manager.requireTopScreen<CounterScreen>().value)
        assertNotEquals(first.screenKey.id, manager.backStack.last().id)
    }

    @Test
    fun popIgnoresRequestsUntilExactly200MillisecondsWithoutExtendingTheWindow() {
        val manager = createManager([key<HomeScreen>(), key<CounterScreen>(ScreenArgs.build(1)), key<CounterScreen>(ScreenArgs.build(2))])
        val top = manager.requireTopScreen<CounterScreen>()

        assertTrue(top.pop())
        val previous = manager.requireTopScreen<CounterScreen>()
        assertFalse(previous.pop())
        timeSource += 200.milliseconds - 1.nanoseconds
        assertFalse(previous.pop())

        assertEquals(2, manager.backStack.size)
        assertEquals(1, top.clearCount)
        assertEquals(0, previous.clearCount)
        assertSame(previous, manager.store[previous.screenKey.id])

        timeSource += 1.nanoseconds
        assertTrue(previous.pop())

        assertEquals(1, manager.backStack.size)
        assertEquals(1, previous.clearCount)
        assertIs<HomeScreen>(manager.store[manager.backStack.single().id])
    }

    @Test
    fun navigateAndPopShareOneWindowInBothDirections() {
        val manager = createManager()
        val home = manager.requireTopScreen<HomeScreen>()
        home.navigate(::CounterScreen, 1)
        val screen = manager.requireTopScreen<CounterScreen>()

        timeSource += 100.milliseconds
        assertFalse(screen.pop())
        assertEquals(2, manager.backStack.size)
        assertEquals(0, screen.clearCount)
        timeSource += 100.milliseconds
        assertTrue(screen.pop())

        home.navigate(::CounterScreen, 2)
        timeSource += 199.milliseconds
        home.navigate(::CounterScreen, 3)
        assertEquals(1, manager.backStack.size)
        assertEquals(1, counterFactoryCalls)

        timeSource += 1.milliseconds
        home.navigate(::CounterScreen, 4)

        assertEquals(2, manager.backStack.size)
        assertEquals(2, counterFactoryCalls)
        assertEquals(4, manager.requireTopScreen<CounterScreen>().value)
    }

    @Test
    fun typedAndStringNavigationShareOneWindow() {
        val manager = createManager()
        val home = manager.requireTopScreen<HomeScreen>()
        home.navigate("counter", 1)
        home.navigate(::CounterScreen, 2)
        assertEquals(1, manager.requireTopScreen<CounterScreen>().value)

        timeSource += 200.milliseconds
        home.navigate(::CounterScreen, 3)
        home.navigate("counter", 4)
        assertEquals(3, manager.requireTopScreen<CounterScreen>().value)

        timeSource += 200.milliseconds
        home.navigate("counter", 5)

        assertEquals(4, manager.backStack.size)
        assertEquals(3, counterFactoryCalls)
        assertEquals(5, manager.requireTopScreen<CounterScreen>().value)
    }

    @Test
    fun ignoredRequestsDoNotApplyPoliciesOrInvokeFactoriesOrLifecycleCallbacks() {
        val manager = createManager([key<HomeScreen>(), key<CounterScreen>(ScreenArgs.build(1))])
        val home = assertIs<HomeScreen>(manager.store[manager.backStack.first().id])
        val original = manager.requireTopScreen<CounterScreen>()
        manager.navigate(::CounterScreen, 2)
        val latest = manager.requireTopScreen<CounterScreen>()
        val before = manager.backStack.toList()

        for (create in CreatePolicy.entries) {
            for (clear in ClearPolicy.entries) {
                manager.navigate(::CounterScreen, 99, policy = create + clear)
                manager.navigate("counter", 99, policy = create + clear)
                assertFalse(manager.pop())
            }
        }
        timeSource += 200.milliseconds

        assertContentEquals(before, manager.backStack)
        assertEquals(2, counterFactoryCalls)
        assertEquals(1, original.value)
        assertEquals(2, latest.value)
        assertEquals(0, original.resumeCount)
        assertEquals(0, latest.resumeCount)
        for (screen in [home, original, latest]) {
            assertSame(screen, manager.store[screen.screenKey.id])
            assertEquals(1, screen.initializeCount)
            assertEquals(0, screen.clearCount)
        }
    }

    @Test
    fun differentManagersHaveIndependentWindows() {
        val first = createManager()
        val second = createManager([key<HomeScreen>(), key<CounterScreen>(ScreenArgs.build(2))])

        first.navigate(::CounterScreen, 1)
        assertTrue(second.pop())
        assertFalse(first.pop())
        second.navigate(::CounterScreen, 3)

        assertEquals(2, first.backStack.size)
        assertEquals(1, second.backStack.size)

        timeSource += 200.milliseconds
        assertTrue(first.pop())
        second.navigate(::CounterScreen, 4)

        assertEquals(1, first.backStack.size)
        assertEquals(2, second.backStack.size)
        assertEquals(4, second.requireTopScreen<CounterScreen>().value)
    }

    @Test
    fun requestsDuringInitializationAreIgnoredWithinTheSameWindow() {
        val manager = createManager()

        manager.navigate(::RedirectScreen)
        val screen = manager.requireTopScreen<RedirectScreen>()

        assertEquals(2, manager.backStack.size)
        assertEquals(0, counterFactoryCalls)
        assertEquals(1, screen.initializeCount)
        assertEquals(0, screen.clearCount)
        assertEquals(false, screen.popResult)
    }

    @Test
    fun requestsDuringResumeAreIgnoredWithinTheSameWindow() {
        val manager = createManager([key<HomeScreen>(), key<CounterScreen>(ScreenArgs.build(1))])
        val screen = manager.requireTopScreen<CounterScreen>()
        var popResult: Boolean? = null
        screen.resumeAction = {
            navigate(::CounterScreen, 99)
            popResult = pop()
        }

        manager.navigate(::CounterScreen, 2, policy = CreatePolicy.Resume + ClearPolicy.None)

        assertSame(screen, manager.requireTopScreen<CounterScreen>())
        assertEquals(2, manager.backStack.size)
        assertEquals(1, counterFactoryCalls)
        assertEquals(2, screen.value)
        assertEquals(1, screen.resumeCount)
        assertEquals(0, screen.clearCount)
        assertEquals(false, popResult)
    }
}
