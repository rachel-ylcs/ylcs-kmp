package love.yinlin.compose.screen

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.extension.cleaning
import love.yinlin.reflect.metaClassName
import kotlin.test.*

@OptIn(CompatibleRachelApi::class)
@Suppress("unused")
class Screen404Test {
    private class HomeScreen : TestScreen()
    private class Missing0 : TestScreen()
    private class Missing1(val first: Int) : TestScreen()
    private class Missing2(val first: Int, val second: String?) : TestScreen()
    private class Missing3(val first: Int, val second: String?, val third: Boolean) : TestScreen()
    private class Missing4(val first: Int, val second: String?, val third: Boolean, val fourth: List<Int>) : TestScreen()
    private class Missing5(val first: Int, val second: String?, val third: Boolean, val fourth: List<Int>, val fifth: Long) : TestScreen()

    private class Custom404 : TestScreen() {
        var initializeCount = 0
        var clearCount = 0
        var resumeCount = 0
        var foundSelfDuringInitialize = false

        override fun initialize() {
            initializeCount++
            foundSelfDuringInitialize = manager.store[screenKey.id] === this && screenKey in manager.backStack
        }

        override fun uninitialize() { clearCount++ }

        override fun resume() = withResume { resumeCount++ }
    }

    private val owners: MutableList<ViewModelStore> = []

    private inline fun <reified S : ScreenModel> key(args: ScreenArgs = ScreenArgs.Empty): ScreenKey =
        ScreenKey(type = metaClassName<S>(), args = args)

    private fun createManager(registry: ScreenRegistry, initial: List<ScreenKey>, owner: ViewModelStore = newOwner()): ScreenManager =
        ViewModelProvider.create(
            store = owner,
            factory = viewModelFactory { addInitializer(ScreenManager::class) { ScreenManager(registry, initial) } },
        )[ScreenID().toString(), ScreenManager::class]

    private fun newOwner(): ViewModelStore = ViewModelStore().also { owners += it }

    @AfterTest
    fun clearOwners() {
        var failure: Throwable? = null
        for (owner in owners.asReversed()) {
            val error = runCatching { owner.clear() }.exceptionOrNull()
            failure = failure ?: error
        }
        owners.clear()
        failure?.let { throw it }
    }

    @Test
    fun missingInitialPageUsesDefault404WithItsOriginalKeyAndContentId() {
        val missing = ScreenKey("missing.page", ScreenArgs.build(42, "ignored"))
        val manager = createManager(ScreenRegistry { }, [missing])
        val screen = assertIs<Screen404>(manager.store[missing.id])
        val entry = manager.navEntry(missing)

        assertSame(manager, screen.manager)
        assertSame(missing, screen.screenKey)
        assertSame(missing.args, screen.screenKey.args)
        assertEquals([missing], manager.backStack)
        assertEquals(missing.id.toString(), entry.contentKey)
        assertSame(screen.navEntry, entry)
        repeat(3) { assertSame(entry, manager.navEntry(missing)) }
        assertFalse(screen.pop())
    }

    @Test
    fun differentMissingEntriesHaveIndependentCustom404Instances() {
        var factoryCalls = 0
        val registry = ScreenRegistry { screen404 { factoryCalls++; Custom404() } }
        val firstKey = ScreenKey("missing.page", ScreenArgs.build(7))
        val secondKey = ScreenKey("missing.page", ScreenArgs.build(7))
        val manager = createManager(registry, [firstKey, secondKey])
        val first = assertIs<Custom404>(manager.store[firstKey.id])
        val second = assertIs<Custom404>(manager.store[secondKey.id])

        assertNotSame(first, second)
        assertEquals(2, factoryCalls)
        assertEquals(1, first.initializeCount)
        assertEquals(1, second.initializeCount)
        assertTrue(first.foundSelfDuringInitialize)
        assertTrue(second.foundSelfDuringInitialize)
        assertNotEquals(manager.navEntry(firstKey).contentKey, manager.navEntry(secondKey).contentKey)

        assertTrue(second.pop())
        assertEquals([firstKey], manager.backStack)
        assertEquals(0, first.clearCount)
        assertEquals(1, second.clearCount)
        assertFalse(first.pop())
    }

    @Test
    fun publicNavigationWithZeroToFiveArgumentsReturnsUnitAndUsesDefault404() {
        val manager = createManager(ScreenRegistry { screen(::HomeScreen) }, [key<HomeScreen>()])
        val source = manager.requireTopScreen<HomeScreen>()
        var constructorCalls = 0
        val results: List<Unit> = [
            source.navigate<Missing0>({ constructorCalls++; error("Must not execute the navigation constructor.") }),
            source.navigate<Missing1, Int>({ _ -> constructorCalls++; error("Must not execute the navigation constructor.") }, 7),
            source.navigate<Missing2, Int, String?>({ _, _ -> constructorCalls++; error("Must not execute the navigation constructor.") }, 7, null),
            source.navigate<Missing3, Int, String?, Boolean>({ _, _, _ -> constructorCalls++; error("Must not execute the navigation constructor.") }, 7, null, true),
            source.navigate<Missing4, Int, String?, Boolean, List<Int>>({ _, _, _, _ -> constructorCalls++; error("Must not execute the navigation constructor.") }, 7, null, true, [1, 2]),
            source.navigate<Missing5, Int, String?, Boolean, List<Int>, Long>({ _, _, _, _, _ -> constructorCalls++; error("Must not execute the navigation constructor.") }, 7, null, true, [1, 2], Long.MAX_VALUE),
        ]

        val types: List<String> = [metaClassName<Missing0>(), metaClassName<Missing1>(), metaClassName<Missing2>(),
            metaClassName<Missing3>(), metaClassName<Missing4>(), metaClassName<Missing5>()]

        assertEquals(6, results.size)
        assertEquals(0, constructorCalls)
        assertEquals(7, manager.backStack.size)
        for (index in types.indices) {
            val entryKey = manager.backStack[index + 1]
            val fallback = assertIs<Screen404>(manager.store[entryKey.id])
            assertEquals(types[index], entryKey.type)
            assertEquals(index, entryKey.args.size)
            assertSame(entryKey, fallback.screenKey)
            assertSame(manager, fallback.manager)
            assertEquals(entryKey.id.toString(), manager.navEntry(entryKey).contentKey)
        }
        assertEquals(7, manager.backStack.map { it.id }.toSet().size)
        assertEquals(Long.MAX_VALUE, manager.backStack.last().args.get<Long>(4))

        repeat(6) { assertTrue(manager.pop()) }
        assertSame(source, manager.requireTopScreen<HomeScreen>())
        assertFalse(source.pop())

    }

    @Test
    fun custom404UsesTheRequestedTypeForAllNavigationPolicies() {
        for (create in CreatePolicy.entries) {
            for (clear in ClearPolicy.entries) {
                var factoryCalls = 0
                val registry = ScreenRegistry {
                    screen(::HomeScreen)
                    screen404 { factoryCalls++; Custom404() }
                }
                val home = key<HomeScreen>()
                val firstKey = ScreenKey("missing.A", ScreenArgs.build(1))
                val middle = ScreenKey("missing.B")
                val targetKey = ScreenKey("missing.A", ScreenArgs.build(2))
                val upper = key<HomeScreen>()
                val manager = createManager(registry, [home, firstKey, middle, targetKey, upper])
                val first = assertIs<Custom404>(manager.store[firstKey.id])
                val target = assertIs<Custom404>(manager.store[targetKey.id])
                val otherType = assertIs<Custom404>(manager.store[middle.id])

                val result: Unit = manager.navigateTo("missing.A", ScreenArgs.build(99), create + clear)
                val selected = manager.requireTopScreen<Custom404>()
                val expected = when (create) {
                    CreatePolicy.New -> [home, firstKey, middle, targetKey, upper, selected.screenKey]
                    CreatePolicy.Replace -> if (clear == ClearPolicy.Clear) [home, firstKey, middle, selected.screenKey]
                        else [home, firstKey, middle, upper, selected.screenKey]
                    CreatePolicy.Move, CreatePolicy.Resume -> if (clear == ClearPolicy.Clear) [home, firstKey, middle, targetKey]
                        else [home, firstKey, middle, upper, targetKey]
                }

                assertEquals(Unit, result)
                assertEquals(expected, manager.backStack, "$create + $clear")
                assertSame(first, manager.store[firstKey.id])
                assertSame(otherType, manager.store[middle.id])
                assertEquals(0, first.clearCount)
                assertEquals(0, otherType.clearCount)
                assertEquals(1, selected.initializeCount)
                assertEquals(if (create == CreatePolicy.Resume) 1 else 0, selected.resumeCount)
                if (create == CreatePolicy.Move || create == CreatePolicy.Resume) {
                    assertSame(target, selected)
                    assertSame(targetKey, selected.screenKey)
                    assertEquals(2, selected.screenKey.args.get<Int>(0))
                    assertEquals(3, factoryCalls)
                }
                else {
                    assertNotSame(target, selected)
                    assertEquals(99, selected.screenKey.args.get<Int>(0))
                    assertEquals(4, factoryCalls)
                }
                assertEquals(if (create == CreatePolicy.Replace) 1 else 0, target.clearCount)
                assertEquals(create != CreatePolicy.New && clear == ClearPolicy.Clear, manager.store[upper.id] == null)
            }
        }
    }

    @Test
    fun missingTypeCreates404ForEveryPolicyWithoutClearingTheRoot() {
        for (create in CreatePolicy.entries) {
            for (clear in ClearPolicy.entries) {
                val home = key<HomeScreen>()
                val manager = createManager(ScreenRegistry { screen(::HomeScreen); screen404(::Custom404) }, [home])
                val source = manager.requireTopScreen<HomeScreen>()

                source.navigate(::Missing0, create + clear)
                val fallback = manager.requireTopScreen<Custom404>()

                assertEquals([home, fallback.screenKey], manager.backStack)
                assertSame(source, manager.store[home.id])
                assertEquals(metaClassName<Missing0>(), fallback.screenKey.type)
                assertEquals(1, fallback.initializeCount)
                assertEquals(0, fallback.resumeCount)
                assertTrue(fallback.foundSelfDuringInitialize)
            }
        }
    }

    @Test
    fun failedNavigation404FactoryPreservesTheStackAndAllowsRetry() {
        val failure = IllegalStateException("404 factory")
        var shouldFail = true
        val registry = ScreenRegistry {
            screen(::HomeScreen)
            screen404 {
                if (shouldFail) throw failure
                Custom404()
            }
        }
        val home = key<HomeScreen>()
        val manager = createManager(registry, [home])
        val source = manager.requireTopScreen<HomeScreen>()

        assertSame(failure, assertFailsWith<IllegalStateException> {
            source.navigate(::Missing0, CreatePolicy.Replace + ClearPolicy.Clear)
        })
        assertEquals([home], manager.backStack)
        assertSame(source, manager.requireTopScreen<HomeScreen>())
        assertFalse(manager.isClosed)
        shouldFail = false

        source.navigate(::Missing0, CreatePolicy.Replace + ClearPolicy.Clear)
        val fallback = manager.requireTopScreen<Custom404>()
        assertEquals(1, fallback.initializeCount)
        assertTrue(fallback.foundSelfDuringInitialize)
        fallback.navigate(::HomeScreen)
        assertIs<HomeScreen>(manager.store[manager.backStack.last().id])
        assertTrue(manager.pop())
        assertSame(fallback, manager.requireTopScreen<Custom404>())
        assertTrue(fallback.pop())
        assertSame(source, manager.requireTopScreen<HomeScreen>())
    }

    @Test
    fun activeEntryWithoutAnInstanceCreates404OnceAndKeepsTheStackUnchanged() {
        var factoryCalls = 0
        val registry = ScreenRegistry {
            screen(::HomeScreen)
            screen404 { factoryCalls++; Custom404() }
        }
        val originalKey = key<HomeScreen>()
        val manager = createManager(registry, [originalKey])
        manager.store.remove(originalKey.id)
        assertNull(manager.store[originalKey.id])

        val entry = manager.navEntry(originalKey)
        val fallback = assertIs<Custom404>(manager.store[originalKey.id])

        assertEquals([originalKey], manager.backStack)
        assertSame(originalKey, fallback.screenKey)
        assertSame(manager, fallback.manager)
        assertTrue(fallback.foundSelfDuringInitialize)
        assertEquals(1, fallback.initializeCount)
        repeat(3) { assertSame(entry, manager.navEntry(originalKey)) }
        assertEquals(1, factoryCalls)
        assertEquals(originalKey.id.toString(), entry.contentKey)
    }

    @Test
    fun uiOwnerContainsTheSame404ViewModelAsTheManager() {
        val missing = ScreenKey("missing.page")
        val manager = createManager(ScreenRegistry { screen404(::Custom404) }, [missing])
        val fallback = assertIs<Custom404>(manager.store[missing.id])
        val entry = manager.navEntry(missing)
        val uiModel = ViewModelProvider.create(
            store = manager.store.provider.getOrCreate(entry.contentKey),
            factory = viewModelFactory { addInitializer(ScreenModel::class) { error("Must reuse the managed 404.") } },
        )[ScreenStore.DEFAULT_SCREEN_VIEWMODEL_KEY, ScreenModel::class]

        assertSame(fallback, uiModel)
    }

    @Test
    fun popped404WaitsForItsLastUiReferenceAndDoesNotReappearAfterCleanup() {
        val home = key<HomeScreen>()
        val missing = ScreenKey("missing.page")
        val manager = createManager(ScreenRegistry { screen(::HomeScreen); screen404(::Custom404) }, [home, missing])
        val fallback = assertIs<Custom404>(manager.store[missing.id])
        val entry = manager.navEntry(missing)
        val job = assertNotNull(fallback.viewModelScope.coroutineContext[Job])
        val reference = manager.store.retain(missing.id)

        cleaning(reference::close) {
            assertTrue(fallback.pop())
            assertSame(entry, manager.navEntry(missing))
            assertEquals(0, fallback.clearCount)
            assertTrue(job.isActive)
        }

        assertEquals(1, fallback.clearCount)
        assertTrue(job.isCancelled)
        assertFailsWith<IllegalArgumentException> { manager.navEntry(missing) }
        assertNull(manager.store[missing.id])
        assertEquals([home], manager.backStack)
    }

    @Test
    fun sharedRegistryStillScopes404InstancesToTheirOwnManager() {
        val registry = ScreenRegistry { screen404(::Custom404) }
        val missing = ScreenKey("missing.page")
        val firstOwner = newOwner()
        val first = createManager(registry, [missing], firstOwner)
        val second = createManager(registry, [missing])
        val firstScreen = assertIs<Custom404>(first.store[missing.id])
        val secondScreen = assertIs<Custom404>(second.store[missing.id])

        assertNotSame(firstScreen, secondScreen)
        firstOwner.clear()

        assertEquals(1, firstScreen.clearCount)
        assertEquals(0, secondScreen.clearCount)
        assertSame(secondScreen.navEntry, second.navEntry(missing))
        assertFailsWith<IllegalArgumentException> { first.navEntry(missing) }
    }

    @Test
    fun failed404CreationLeavesTheKeyActiveAndAllowsAnotherEntryLookup() {
        var shouldFail = true
        val failure = IllegalStateException("404 factory")
        val registry = ScreenRegistry {
            screen(::HomeScreen)
            screen404 {
                if (shouldFail) throw failure
                Custom404()
            }
        }
        val originalKey = key<HomeScreen>()
        val manager = createManager(registry, [originalKey])
        manager.store.remove(originalKey.id)

        assertSame(failure, assertFailsWith<IllegalStateException> { manager.navEntry(originalKey) })
        assertEquals([originalKey], manager.backStack)
        assertNull(manager.store[originalKey.id])
        shouldFail = false

        val entry = manager.navEntry(originalKey)
        val fallback = assertIs<Custom404>(manager.store[originalKey.id])
        assertEquals(1, fallback.initializeCount)
        assertSame(fallback.navEntry, entry)
    }
}
