package love.yinlin.compose.screen

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.annotation.LooseTyped
import love.yinlin.reflect.metaClassName
import kotlin.test.*

@OptIn(CompatibleRachelApi::class, LooseTyped::class)
class ScreenKeyNavigationTest {
    private open class ProbeScreen : TestScreen() {
        var initializeCount = 0
        var clearCount = 0

        override fun initialize() { ++initializeCount }
        override fun uninitialize() { ++clearCount }
    }

    private class Home : ProbeScreen()
    private class Cover : ProbeScreen()
    private class Screen0 : ProbeScreen()
    private class Screen1(val initial: Int) : ProbeScreen() {
        var value = initial
        var resumeCount = 0

        override fun resume() = withResume { next: Int ->
            value = next
            ++resumeCount
        }
    }
    private class Screen2(val first: Int, val second: String?) : ProbeScreen()
    private class Screen3(val first: Int, val second: String?, val third: Boolean) : ProbeScreen()
    private class Screen4(val first: Int, val second: String?, val third: Boolean, val fourth: Payload) : ProbeScreen()
    private class Screen5(val first: Long, val second: String?, val third: Boolean, val fourth: Payload?, val fifth: List<Long>?) : ProbeScreen()
    private class Missing : ProbeScreen()
    private class Custom404 : ProbeScreen() {
        var resumeCount = 0
        override fun resume() { ++resumeCount }
    }

    @Serializable
    private data class Payload(val title: String, val ids: List<Int>)

    private val owners: MutableList<ViewModelStore> = []

    private fun defaultRegistry(): ScreenRegistry = ScreenRegistry {
        screen(::Home)
        screen(::Cover)
        screen(::Screen0, "zero")
        screen(::Screen1, "one")
        screen(::Screen2, "two")
        screen(::Screen3, "three")
        screen(::Screen4, "four")
        screen(::Screen5, "five")
        screen404(::Custom404)
    }

    private fun createManager(
        registry: ScreenRegistry = defaultRegistry(),
        initial: List<ScreenKey> = [ScreenKey(metaClassName<Home>())]
    ): ScreenManager {
        val owner = ViewModelStore().also { owners += it }
        return ViewModelProvider.create(
            store = owner,
            factory = viewModelFactory { addInitializer(ScreenManager::class) { ScreenManager(registry, initial) } }
        )[TestManagerID, ScreenManager::class]
    }

    @AfterTest
    fun clearOwners() {
        for (owner in owners.asReversed()) owner.clear()
        owners.clear()
    }

    @Test
    fun publicStringNavigationSupportsZeroToFiveArguments() {
        val manager = createManager()
        val home = manager.requireTopScreen<Home>()
        val payload = Payload("中文 | ?", [1, 2])
        val ids: List<Long> = [Long.MIN_VALUE, Long.MAX_VALUE]

        val result: Unit = home.navigate("zero")
        assertEquals(Unit, result)
        assertIs<Screen0>(manager.requireTopScreen<Screen0>())

        home.navigate("one", 1)
        assertEquals(1, manager.requireTopScreen<Screen1>().initial)

        home.navigate<Int, String?>("two", 2, null)
        manager.requireTopScreen<Screen2>().let {
            assertEquals(2, it.first)
            assertNull(it.second)
        }

        home.navigate("three", 3, "three", true)
        manager.requireTopScreen<Screen3>().let {
            assertEquals(3, it.first)
            assertEquals("three", it.second)
            assertTrue(it.third)
        }

        home.navigate<Int, String?, Boolean, Payload>("four", 4, null, false, payload)
        manager.requireTopScreen<Screen4>().let {
            assertEquals(4, it.first)
            assertNull(it.second)
            assertFalse(it.third)
            assertEquals(payload, it.fourth)
        }

        home.navigate<Long, String?, Boolean, Payload?, List<Long>?>("five", Long.MAX_VALUE, null, true, payload, ids)
        manager.requireTopScreen<Screen5>().let {
            assertEquals(Long.MAX_VALUE, it.first)
            assertNull(it.second)
            assertTrue(it.third)
            assertEquals(payload, it.fourth)
            assertEquals(ids, it.fifth)
        }

        assertEquals([
            metaClassName<Home>(), metaClassName<Screen0>(), metaClassName<Screen1>(), metaClassName<Screen2>(),
            metaClassName<Screen3>(), metaClassName<Screen4>(), metaClassName<Screen5>(),
        ], manager.backStack.map { it.type })
        assertTrue(manager.backStack.none { it.isUnboundKey })
    }

    @Test
    fun typedAndStringRequestsShareAllCreateAndClearStrategies() {
        for (create in CreatePolicy.entries) for (clear in ClearPolicy.entries) for (stringFirst in [false, true]) {
            val manager = createManager()
            val home = manager.requireTopScreen<Home>()
            if (stringFirst) home.navigate("one", 1) else home.navigate(::Screen1, 1)
            val first = manager.requireTopScreen<Screen1>()
            home.navigate(::Cover)
            if (stringFirst) home.navigate("one", 2) else home.navigate(::Screen1, 2)
            val target = manager.requireTopScreen<Screen1>()
            home.navigate(::Cover)
            val cover = manager.requireTopScreen<Cover>()
            val before = manager.backStack.toList()

            if (stringFirst) home.navigate(::Screen1, 99, create + clear)
            else home.navigate("one", 99, create + clear)
            val top = manager.requireTopScreen<Screen1>()

            val remaining = when {
                create == CreatePolicy.New -> before
                clear == ClearPolicy.Clear -> before.take(3)
                else -> before.filter { it.id != target.screenKey.id }
            }
            assertEquals(remaining + top.screenKey, manager.backStack, "$create / $clear / $stringFirst")
            assertSame(first, manager.store[first.screenKey.id])
            assertEquals(0, first.clearCount)
            assertEquals(1, top.initializeCount)
            assertFalse(top.screenKey.isUnboundKey)

            if (create == CreatePolicy.New || create == CreatePolicy.Replace) {
                assertNotSame(target, top)
                assertEquals(99, top.initial)
                assertEquals(0, top.resumeCount)
            } else {
                assertSame(target, top)
                assertEquals(2, top.initial)
                assertEquals(2, top.screenKey.args.get<Int>(0))
                assertEquals(if (create == CreatePolicy.Resume) 99 else 2, top.value)
                assertEquals(if (create == CreatePolicy.Resume) 1 else 0, top.resumeCount)
            }
            assertEquals(if (create == CreatePolicy.Replace) 1 else 0, target.clearCount)
            assertEquals(if (create != CreatePolicy.New && clear == ClearPolicy.Clear) 1 else 0, cover.clearCount)
        }
    }

    @Test
    fun unboundStringEqualToRegisteredClassNameNeverSelectsItsInstance() {
        for (create in CreatePolicy.entries) for (clear in ClearPolicy.entries) {
            val manager = createManager()
            val home = manager.requireTopScreen<Home>()
            home.navigate(::Screen1, 7)
            val real = manager.requireTopScreen<Screen1>()
            val before = manager.backStack.toList()
            val name = metaClassName<Screen1>()

            home.navigate(name, "not an Int", create + clear)
            val missing = manager.requireTopScreen<Custom404>()

            assertEquals(before + missing.screenKey, manager.backStack)
            assertTrue(missing.screenKey.isUnboundKey)
            assertEquals(name, missing.screenKey.type)
            assertEquals(0, missing.resumeCount)
            assertEquals(7, real.value)
            assertEquals(0, real.clearCount)
            assertSame(real, manager.store[real.screenKey.id])

            missing.navigate("one", 99, CreatePolicy.Resume + ClearPolicy.None)
            assertSame(real, manager.requireTopScreen<Screen1>())
            assertEquals(99, real.value)
            assertEquals(1, real.resumeCount)
            assertSame(missing, manager.store[missing.screenKey.id])
        }
    }

    @Test
    fun boundKeyMayEqualAnotherClassNameWithoutChangingTypedLookup() {
        val registry = ScreenRegistry {
            screen(::Home)
            screen(::Screen0, metaClassName<Missing>())
            screen(::Screen1, metaClassName<Screen0>())
            screen404(::Custom404)
        }
        val manager = createManager(registry)
        val home = manager.requireTopScreen<Home>()

        home.navigate(::Screen0)
        val zero = manager.requireTopScreen<Screen0>()
        home.navigate(metaClassName<Screen0>(), 17)
        assertEquals(17, manager.requireTopScreen<Screen1>().initial)
        home.navigate(metaClassName<Missing>())
        assertIs<Screen0>(manager.requireTopScreen<Screen0>())
        home.navigate(::Missing)
        val missing = manager.requireTopScreen<Custom404>()

        assertFalse(missing.screenKey.isUnboundKey)
        assertEquals(metaClassName<Missing>(), missing.screenKey.type)
        assertSame(zero, manager.store[zero.screenKey.id])
    }

    @Test
    fun invalidArgumentsAndFactoryErrorsPreserveTheStackAndDoNotUse404() {
        var calls = 0
        var fallbackCalls = 0
        var shouldFail = true
        val failure = IllegalStateException("factory")
        val registry = ScreenRegistry {
            screen(::Home)
            screen<Screen1, Int>({ value ->
                ++calls
                if (shouldFail) throw failure
                Screen1(value)
            }, "one")
            screen404 { ++fallbackCalls; Custom404() }
        }
        val manager = createManager(registry)
        val home = manager.requireTopScreen<Home>()
        val before = manager.backStack.toList()

        assertFailsWith<IllegalArgumentException> { home.navigate("one") }
        assertFailsWith<SerializationException> { home.navigate("one", "wrong type") }
        assertFailsWith<SerializationException> { home.navigate<Int?>("one", null) }
        assertEquals(0, calls)
        assertSame(failure, assertFailsWith<IllegalStateException> { home.navigate("one", 7) })
        assertEquals(before, manager.backStack)
        assertEquals(0, fallbackCalls)

        shouldFail = false
        home.navigate("one", 7)
        assertEquals(7, manager.requireTopScreen<Screen1>().initial)
        assertEquals(2, calls)
        assertEquals(0, fallbackCalls)
    }

    @Test
    fun identicalKeysResolveWithinTheirOwnManager() {
        val first = createManager(ScreenRegistry {
            screen(::Home)
            screen(::Screen0, "shared")
        })
        val second = createManager(ScreenRegistry {
            screen(::Home)
            screen(::Cover, "shared")
        })

        first.requireTopScreen<Home>().navigate("shared")
        second.requireTopScreen<Home>().navigate("shared")
        val zero = first.requireTopScreen<Screen0>()
        val cover = second.requireTopScreen<Cover>()

        assertSame(first, zero.manager)
        assertSame(second, cover.manager)
        assertNull(first.store[cover.screenKey.id])
        assertNull(second.store[zero.screenKey.id])
        assertTrue(zero.pop())
        assertEquals(1, zero.clearCount)
        assertSame(cover, second.requireTopScreen<Cover>())
        assertEquals(0, cover.clearCount)
    }

    @Test
    fun restoredKeysKeepResolvedTypesAndUnboundLookupIdentity() {
        val original = createManager()
        val home = original.requireTopScreen<Home>()
        home.navigate("one", 7)
        val one = original.requireTopScreen<Screen1>()
        home.navigate(metaClassName<Screen1>(), "ignored")
        val missing = original.requireTopScreen<Custom404>()
        val restoredKeys = original.backStack.map { ScreenKey.parse(it.toString()) }
        val restored = createManager(ScreenRegistry {
            screen(::Home)
            screen(::Screen1)
            screen404(::Custom404)
        }, restoredKeys)

        assertEquals(original.backStack.toList(), restored.backStack.toList())
        val restoredOne = assertIs<Screen1>(restored.store[one.screenKey.id])
        val restoredMissing = restored.requireTopScreen<Custom404>()
        assertEquals(7, restoredOne.initial)
        assertNotSame(one, restoredOne)
        assertNotSame(missing, restoredMissing)
        assertTrue(restoredMissing.screenKey.isUnboundKey)
        assertSame(restoredMissing.navEntry, restored.navEntry(restoredMissing.screenKey))
        assertFailsWith<IllegalArgumentException> { restored.navEntry(restoredMissing.screenKey.copy(isUnboundKey = false)) }
        assertSame(restoredMissing, restored.requireTopScreen<Custom404>())
    }

    @Test
    fun missingKeysUseTheirOwnStrategiesAndReleaseRemoved404Instances() {
        val manager = createManager()
        val home = manager.requireTopScreen<Home>()
        home.navigate("missing.a", 1)
        val first = manager.requireTopScreen<Custom404>()
        home.navigate("missing.b")
        val second = manager.requireTopScreen<Custom404>()

        home.navigate("missing.a", 99, CreatePolicy.Resume + ClearPolicy.Clear)

        assertSame(first, manager.requireTopScreen<Custom404>())
        assertEquals(1, first.resumeCount)
        assertEquals(1, first.screenKey.args.get<Int>(0))
        assertEquals(1, second.clearCount)
        assertNull(manager.store[second.screenKey.id])
        assertTrue(first.pop())
        assertEquals(1, first.clearCount)
        assertSame(home, manager.requireTopScreen<Home>())
        assertFailsWith<IllegalArgumentException> { first.navigate("zero") }
    }

    @Test
    fun stringRequestsUseDefault404AndRejectBlankKeysWithoutChangingTheStack() {
        val manager = createManager(ScreenRegistry {
            screen(::Home)
            screen(::Screen0)
        })
        val home = manager.requireTopScreen<Home>()
        home.navigate(metaClassName<Screen0>())
        val missing = manager.requireTopScreen<Screen404>()
        val before = manager.backStack.toList()

        assertTrue(missing.screenKey.isUnboundKey)
        for (key in ["", " ", "\t\n"]) {
            assertFailsWith<IllegalArgumentException> { home.navigate(key) }
            assertEquals(before, manager.backStack)
        }
        assertTrue(missing.pop())
        home.navigate(::Screen0)
        assertIs<Screen0>(manager.requireTopScreen<Screen0>())
    }
}
