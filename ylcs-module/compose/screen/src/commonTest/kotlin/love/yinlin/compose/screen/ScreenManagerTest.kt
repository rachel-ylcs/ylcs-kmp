package love.yinlin.compose.screen

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.extension.cleaning
import love.yinlin.reflect.metaClassName
import kotlin.test.*

@OptIn(CompatibleRachelApi::class)
@Suppress("unused")
class ScreenManagerTest {
    private open class ProbeScreen : TestScreen() {
        var initializeCount = 0
        var clearCount = 0
        var foundSelfDuringInitialize = false

        override fun initialize() {
            ++initializeCount
            foundSelfDuringInitialize = manager.backStack.any { it.id == screenKey.id } && manager.store[screenKey.id] === this
        }

        override fun uninitialize() { ++clearCount }
    }

    private class RootScreen : ProbeScreen()
    private class NamedScreen(val id: Int, val key: String) : ProbeScreen()
    private class OtherScreen(val value: String) : ProbeScreen()
    private class NumberScreen(initialValue: Int?) : ProbeScreen() {
        var value: Int? by mutableStateOf(initialValue)
        var resumeCount = 0

        override fun resume() = withResume { next: Int? ->
            ++resumeCount
            value = next
        }
    }

    private class ReentrantScreen(val initialValue: Int) : ProbeScreen() {
        val values: MutableList<Int> = []

        override fun resume() = withResume { next: Int ->
            values += next
            if (next == 1) {
                manager.navigate(::ReentrantScreen, 2, policy = CreatePolicy.Resume + ClearPolicy.None)
                withResume { outer: Int -> values += outer }
            }
        }
    }

    @Serializable
    private data class Payload(val title: String)

    private class TwoScreen(val first: Int, val second: String?) : ProbeScreen()
    private class ThreeScreen(val first: Int, val second: String?, val third: Boolean) : ProbeScreen()
    private class FourScreen(val first: Int, val second: String?, val third: Boolean, val fourth: Payload) : ProbeScreen()
    private class FiveScreen(val first: Long, val second: String?, val third: Boolean, val fourth: Payload?, val fifth: List<Int>?) : ProbeScreen()

    private class CallbackScreen(private val action: CallbackScreen.() -> Unit) : ProbeScreen() {
        override fun initialize() {
            super.initialize()
            action()
        }
    }

    private class ClearingScreen(private val action: () -> Unit) : ProbeScreen() {
        override fun uninitialize() {
            super.uninitialize()
            action()
        }
    }

    private class ResumeFailureScreen(private val failure: Throwable) : ProbeScreen() {
        var resumeCount = 0

        override fun resume() {
            ++resumeCount
            throw failure
        }
    }

    private val owners: MutableList<ViewModelStore> = []

    private fun newOwner(): ViewModelStore = ViewModelStore().also { owners += it }

    private fun defaultRegistry(): ScreenRegistry = ScreenRegistry {
        screen(::RootScreen)
        screen(::OtherScreen)
        screen(::NumberScreen)
        screen(::TwoScreen)
        screen(::ThreeScreen)
        screen(::FourScreen)
        screen(::FiveScreen)
        screen(::NamedScreen)
    }

    private fun top(manager: ScreenManager): ScreenModel? =
        manager.backStack.lastOrNull()?.let { manager.store[it.id] }

    private inline fun <reified S : ScreenModel> key(args: ScreenArgs = ScreenArgs.Empty): ScreenKey =
        ScreenKey(type = metaClassName<S>(), args = args)

    private fun createManager(
        initial: List<ScreenKey> = [key<RootScreen>()],
        registry: ScreenRegistry = defaultRegistry(),
        owner: ViewModelStore = newOwner(),
        name: String = TestManagerID
    ): ScreenManager = ViewModelProvider.create(
        store = owner,
        factory = viewModelFactory { addInitializer(ScreenManager::class) { ScreenManager(registry, initial) } }
    )[name, ScreenManager::class]

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
    fun unboundScreensRejectManagerAndKeyAccessAndNavigation() {
        val screen = RootScreen()
        assertFailsWith<IllegalArgumentException> { screen.manager }
        assertFailsWith<IllegalArgumentException> { screen.screenKey }
        assertFailsWith<IllegalArgumentException> { screen.navigate(::RootScreen) }
        assertFailsWith<IllegalArgumentException> { screen.pop() }
    }

    @Test
    fun repeatedAttachmentKeepsOriginalManagerAndKey() {
        val manager = createManager()
        val otherManager = createManager()
        val screen = assertIs<RootScreen>(top(manager))
        val originalKey = screen.screenKey
        val otherKey = otherManager.backStack.single()

        assertFailsWith<IllegalArgumentException> { screen.attach(otherManager, otherKey) }

        assertSame(manager, screen.manager)
        assertSame(originalKey, screen.screenKey)
        assertSame(screen, manager.store[originalKey.id])
        assertNotSame(screen, otherManager.store[otherKey.id])
        assertEquals(1, screen.initializeCount)
    }

    @Test
    fun internalConstructorStartsEmptyAndSupportsTypedNavigation() {
        val owner = newOwner()
        val manager = ViewModelProvider.create(
            store = owner,
            factory = viewModelFactory { addInitializer(ScreenManager::class) { ScreenManager(defaultRegistry()) } }
        )[TestManagerID, ScreenManager::class]
        assertTrue(manager.backStack.isEmpty())

        manager.navigate(::RootScreen)
        val screen = manager.requireTopScreen<RootScreen>()

        assertSame(manager, screen.manager)
        assertSame(screen.screenKey, manager.backStack.single())
        assertEquals(1, screen.initializeCount)
        assertTrue(screen.foundSelfDuringInitialize)
        assertFalse(screen.pop())
    }

    @Test
    fun initialScreensAreBoundAndPublishedBeforeInitialize() {
        val rootKey = key<RootScreen>()
        val numberKey = key<NumberScreen>(ScreenArgs.build<Int?>(7))
        val manager = createManager([rootKey, numberKey])
        val root = assertIs<RootScreen>(manager.store[rootKey.id])
        val number = assertIs<NumberScreen>(manager.store[numberKey.id])

        assertEquals(2, manager.backStack.size)
        assertSame(number, top(manager))
        for (screen in [root, number]) {
            assertSame(manager, screen.manager)
            assertEquals(1, screen.initializeCount)
            assertTrue(screen.foundSelfDuringInitialize)
        }
        assertSame(rootKey, root.screenKey)
        assertSame(numberKey, number.screenKey)
        assertSame(numberKey.args, number.screenKey.args)
        assertEquals(7, number.value)
    }

    @Test
    fun initialStackIsDetachedFromCallerList() {
        val rootKey = key<RootScreen>()
        val initial: MutableList<ScreenKey> = [rootKey]
        val manager = createManager(initial)

        initial.clear()
        initial += key<OtherScreen>(ScreenArgs.build("unrelated"))

        assertEquals([rootKey], manager.backStack)
        assertIs<RootScreen>(top(manager))
    }

    @Test
    fun typedNavigationSupportsZeroToFiveArguments() {
        val manager = createManager()
        val payload = Payload("中文 | ?")
        val source: String? = null
        val numbers: List<Int> = [1, 2]

        manager.navigate(::RootScreen)
        val zero = manager.requireTopScreen<RootScreen>()
        manager.navigate(::NumberScreen, 1)
        val one = manager.requireTopScreen<NumberScreen>()
        manager.navigate(::TwoScreen, 2, source)
        val two = manager.requireTopScreen<TwoScreen>()
        manager.navigate(::ThreeScreen, 3, "three", true)
        val three = manager.requireTopScreen<ThreeScreen>()
        manager.navigate(::FourScreen, 4, source, false, payload)
        val four = manager.requireTopScreen<FourScreen>()
        manager.navigate(::FiveScreen, Long.MAX_VALUE, source, true, payload, numbers)
        val five = manager.requireTopScreen<FiveScreen>()

        assertEquals(7, manager.backStack.size)
        assertEquals(1, one.value)
        assertEquals(2, two.first)
        assertNull(two.second)
        assertEquals("three", three.second)
        assertTrue(three.third)
        assertNull(four.second)
        assertFalse(four.third)
        assertEquals(payload, four.fourth)
        assertEquals(Long.MAX_VALUE, five.first)
        assertNull(five.second)
        assertEquals(payload, five.fourth)
        assertEquals([1, 2], five.fifth)
        assertSame(five, top(manager))
        for (screen in [zero, one, two, three, four, five]) {
            assertSame(manager, screen.manager)
            assertEquals(1, screen.initializeCount)
        }
    }

    @Test
    fun screenCanDeclareItsOwnIdAndKeyWithoutConflictingWithNavigationMetadata() {
        val manager = createManager()
        manager.navigate(::NamedScreen, 7, "business-key")
        val screen = manager.requireTopScreen<NamedScreen>()

        assertEquals(7, screen.id)
        assertEquals("business-key", screen.key)
        assertSame(manager, screen.manager)
        assertSame(screen.screenKey, manager.backStack.last())
        assertEquals(metaClassName<NamedScreen>(), screen.screenKey.type)
    }

    @Test
    fun navigationConstructorReferenceIsNotExecuted() {
        val manager = createManager()
        manager.navigate<NumberScreen, Int?>({ error("The navigation reference must not be executed.") }, 7)
        val screen = manager.requireTopScreen<NumberScreen>()

        assertEquals(7, screen.value)
        assertSame(screen, top(manager))
    }

    @Test
    fun factoryCapturedCallbackDoesNotBecomeANavigationArgument() {
        var factoryCalls = 0
        var callbackCalls = 0
        val registry = ScreenRegistry {
            screen(::RootScreen)
            screen<CallbackScreen> {
                ++factoryCalls
                CallbackScreen { ++callbackCalls }
            }
        }
        val manager = createManager(registry = registry)
        manager.navigate<CallbackScreen>({ error("The navigation reference must not be executed.") })
        val screen = manager.requireTopScreen<CallbackScreen>()

        assertEquals(1, factoryCalls)
        assertEquals(1, callbackCalls)
        assertSame(ScreenArgs.Empty, screen.screenKey.args)

        val restoredKey = ScreenKey.parse(screen.screenKey.toString())
        assertTrue(restoredKey.args.isEmpty)
        val restoredManager = createManager([restoredKey], registry)
        val restoredScreen = assertIs<CallbackScreen>(top(restoredManager))

        assertEquals(2, factoryCalls)
        assertEquals(2, callbackCalls)
        assertNotSame(screen, restoredScreen)
        assertSame(restoredKey, restoredScreen.screenKey)
    }

    @Test
    fun everyPolicyUsesNearestInstanceAndClearsCorrectScreens() {
        for (create in CreatePolicy.entries) {
            for (clear in ClearPolicy.entries) {
                val rootKey = key<RootScreen>()
                val firstKey = key<NumberScreen>(ScreenArgs.build<Int?>(10))
                val middleKey = key<OtherScreen>(ScreenArgs.build("middle"))
                val targetKey = key<NumberScreen>(ScreenArgs.build<Int?>(20))
                val topKey = key<OtherScreen>(ScreenArgs.build("top"))
                val manager = createManager([rootKey, firstKey, middleKey, targetKey, topKey])
                val first = assertIs<NumberScreen>(manager.store[firstKey.id])
                val target = assertIs<NumberScreen>(manager.store[targetKey.id])
                val top = assertIs<OtherScreen>(manager.store[topKey.id])

                manager.navigate(::NumberScreen, 99, policy = create + clear)
                val result = manager.requireTopScreen<NumberScreen>()
                val expected = when (create) {
                    CreatePolicy.New -> [rootKey, firstKey, middleKey, targetKey, topKey, result.screenKey]
                    CreatePolicy.Replace -> if (clear == ClearPolicy.Clear) [rootKey, firstKey, middleKey, result.screenKey]
                        else [rootKey, firstKey, middleKey, topKey, result.screenKey]
                    CreatePolicy.Move, CreatePolicy.Resume -> if (clear == ClearPolicy.Clear) [rootKey, firstKey, middleKey, targetKey]
                        else [rootKey, firstKey, middleKey, topKey, targetKey]
                }

                assertEquals(expected, manager.backStack, "$create + $clear")
                assertSame(result, top(manager))
                assertEquals(0, first.clearCount)
                assertEquals(10, first.value)
                assertEquals(if (create == CreatePolicy.Replace) 1 else 0, target.clearCount)
                assertEquals(if (create != CreatePolicy.New && clear == ClearPolicy.Clear) 1 else 0, top.clearCount)
                assertEquals(1, result.initializeCount)
                if (create == CreatePolicy.Move || create == CreatePolicy.Resume) {
                    assertSame(target, result)
                    assertSame(targetKey, result.screenKey)
                    assertEquals(if (create == CreatePolicy.Resume) 99 else 20, result.value)
                    assertEquals(if (create == CreatePolicy.Resume) 1 else 0, result.resumeCount)
                }
                else {
                    assertNotSame(target, result)
                    assertNotEquals(targetKey.id, result.screenKey.id)
                    assertEquals(99, result.value)
                    assertEquals(0, result.resumeCount)
                }
            }
        }
    }

    @Test
    fun missingTypeMatchCreatesForEveryPolicyWithoutClearingRoot() {
        for (create in CreatePolicy.entries) {
            for (clear in ClearPolicy.entries) {
                val manager = createManager()
                val root = assertIs<RootScreen>(top(manager))
                manager.navigate(::NumberScreen, 7, policy = create + clear)
                val screen = manager.requireTopScreen<NumberScreen>()

                assertEquals([root.screenKey, screen.screenKey], manager.backStack)
                assertEquals(0, root.clearCount)
                assertEquals(7, screen.value)
                assertEquals(1, screen.initializeCount)
                assertEquals(0, screen.resumeCount)
            }
        }
    }

    @Test
    fun moveAndResumeAtTopKeepStackIdentityAndCreationArguments() {
        val manager = createManager()
        manager.navigate(::NumberScreen, 1)
        val screen = manager.requireTopScreen<NumberScreen>()
        val stack = manager.backStack
        val entries = stack.toList()
        val key = screen.screenKey

        manager.navigate(::NumberScreen, 2, policy = CreatePolicy.Move + ClearPolicy.Clear)
        assertSame(screen, manager.requireTopScreen<NumberScreen>())
        assertSame(stack, manager.backStack)
        assertEquals(entries, manager.backStack)
        assertEquals(1, screen.value)

        manager.navigate(::NumberScreen, 3, policy = CreatePolicy.Resume + ClearPolicy.Clear)
        assertSame(screen, manager.requireTopScreen<NumberScreen>())
        assertSame(stack, manager.backStack)
        assertEquals(entries, manager.backStack)
        assertSame(key, screen.screenKey)
        assertEquals(1, screen.screenKey.args.get<Int?>(0))
        assertEquals(3, screen.value)
        assertEquals(1, screen.resumeCount)
        assertEquals(1, screen.initializeCount)
    }

    @Test
    fun reentrantResumeThroughManagerRestoresTheOuterArguments() {
        val registry = ScreenRegistry {
            screen(::RootScreen)
            screen(::ReentrantScreen)
        }
        val manager = createManager(registry = registry)
        manager.navigate(::ReentrantScreen, 0)
        val screen = manager.requireTopScreen<ReentrantScreen>()
        val stack = manager.backStack
        val entries = stack.toList()

        manager.navigate(::ReentrantScreen, 1, policy = CreatePolicy.Resume + ClearPolicy.None)
        assertSame(screen, manager.requireTopScreen<ReentrantScreen>())

        assertEquals([1, 2, 1], screen.values)
        assertEquals(0, screen.screenKey.args.get<Int>(0))
        assertSame(stack, manager.backStack)
        assertEquals(entries, manager.backStack)
        assertSame(screen, top(manager))
        assertEquals(1, screen.initializeCount)
        assertFailsWith<IllegalArgumentException> { screen.requireResumeArgs(1) }
    }

    @Test
    fun screenHelpersUseItsBoundManagerAndRejectRemovedSources() {
        val manager = createManager()
        val root = assertIs<RootScreen>(top(manager))
        root.navigate(::NumberScreen, 7)
        val screen = root.manager.requireTopScreen<NumberScreen>()

        assertSame(manager, screen.manager)
        assertTrue(screen.pop())
        assertSame(root, top(manager))
        assertEquals(1, screen.clearCount)
        assertFailsWith<IllegalArgumentException> { screen.navigate(::RootScreen) }
        assertFailsWith<IllegalArgumentException> { screen.pop() }
        assertFalse(root.pop())
    }

    @Test
    fun popKeepsTheLastPage() {
        val manager = createManager()
        val root = assertIs<RootScreen>(top(manager))
        manager.navigate(::NumberScreen, 7)
        val screen = manager.requireTopScreen<NumberScreen>()

        assertTrue(manager.pop())
        assertEquals(1, screen.clearCount)
        assertSame(root, top(manager))
        assertFalse(manager.pop())
        assertEquals(1, manager.backStack.size)
        assertEquals(0, root.clearCount)
    }

    @Test
    fun failedReplacementDoesNotClearExistingScreens() {
        var shouldFail = false
        val failure = IllegalStateException("factory")
        val registry = ScreenRegistry {
            screen(::RootScreen)
            screen(::OtherScreen)
            screen<NumberScreen, Int?> { value ->
                if (shouldFail) throw failure
                NumberScreen(value)
            }
        }
        val manager = createManager(registry = registry)
        manager.navigate(::NumberScreen, 1)
        val original = manager.requireTopScreen<NumberScreen>()
        manager.navigate(::OtherScreen, "top")
        val top = manager.requireTopScreen<OtherScreen>()
        val stack = manager.backStack.toList()
        shouldFail = true

        val caught = assertFailsWith<IllegalStateException> {
            manager.navigate(::NumberScreen, 2, policy = CreatePolicy.Replace + ClearPolicy.Clear)
        }

        assertSame(failure, caught)
        assertEquals(stack, manager.backStack)
        assertSame(original, manager.store[original.screenKey.id])
        assertSame(top, top(manager))
        assertEquals(0, original.clearCount)
        assertEquals(0, top.clearCount)

        shouldFail = false
        manager.navigate(::NumberScreen, 2, policy = CreatePolicy.Replace + ClearPolicy.Clear)
        val replacement = manager.requireTopScreen<NumberScreen>()
        assertEquals(2, replacement.value)
        assertEquals(1, original.clearCount)
        assertEquals(1, top.clearCount)
    }

    @Test
    fun invalidRequestsLeaveExistingStackUnchanged() {
        val manager = createManager()
        val root = assertIs<RootScreen>(top(manager))
        val stack = manager.backStack.toList()

        assertFailsWith<IllegalArgumentException> { manager.navigate<NumberScreen>({ error("Must not execute the navigation constructor.") }) }
        assertFailsWith<SerializationException> { manager.navigate<NumberScreen, String>({ NumberScreen(0) }, "not-an-int") }

        assertEquals(stack, manager.backStack)
        assertSame(root, top(manager))
        assertEquals(0, root.clearCount)
    }

    @Test
    fun failedInitialConstructionClearsPreviouslyCreatedScreens() {
        var created: RootScreen? = null
        val registry = ScreenRegistry {
            screen<RootScreen> { RootScreen().also { created = it } }
            screen(::NumberScreen)
        }

        assertFailsWith<SerializationException> {
            createManager([key<RootScreen>(), key<NumberScreen>(ScreenArgs.build("not-an-int"))], registry)
        }

        assertEquals(1, assertNotNull(created).clearCount)
        assertEquals(0, assertNotNull(created).initializeCount)
    }

    @Test
    fun failedInitialCallbackClosesTheWholeUnpublishedManager() {
        val failure = IllegalStateException("initial callback")
        var created: CallbackScreen? = null
        var sibling: RootScreen? = null
        var escapedManager: ScreenManager? = null
        val registry = ScreenRegistry {
            screen<CallbackScreen> {
                CallbackScreen {
                    escapedManager = manager
                    throw failure
                }.also { created = it }
            }
            screen<RootScreen> { RootScreen().also { sibling = it } }
        }

        val caught = assertFailsWith<IllegalStateException> {
            createManager([key<CallbackScreen>(), key<RootScreen>()], registry)
        }

        assertSame(failure, caught)
        assertEquals(1, assertNotNull(created).initializeCount)
        assertEquals(1, assertNotNull(created).clearCount)
        assertEquals(0, assertNotNull(sibling).initializeCount)
        assertEquals(1, assertNotNull(sibling).clearCount)
        val manager = assertNotNull(escapedManager)
        assertTrue(manager.isClosed)
        assertTrue(manager.backStack.isEmpty())
        assertTrue(assertNotNull(manager.viewModelScope.coroutineContext[Job]).isCancelled)
    }

    @Test
    fun cancellationDuringInitialCallbackStillClearsAllCreatedScreens() {
        val failure = CancellationException("initial callback")
        var created: CallbackScreen? = null
        var sibling: RootScreen? = null
        val registry = ScreenRegistry {
            screen<CallbackScreen> { CallbackScreen { throw failure }.also { created = it } }
            screen<RootScreen> { RootScreen().also { sibling = it } }
        }

        val caught = assertFailsWith<CancellationException> {
            createManager([key<CallbackScreen>(), key<RootScreen>()], registry)
        }

        assertSame(failure, caught)
        assertEquals(1, assertNotNull(created).clearCount)
        assertEquals(1, assertNotNull(sibling).clearCount)
    }

    @Test
    fun duplicateInitialIdsAreRejectedBeforeCallingFactories() {
        var factoryCalls = 0
        val registry = ScreenRegistry {
            screen<RootScreen> {
                ++factoryCalls
                RootScreen()
            }
        }
        val rootKey = key<RootScreen>()

        assertFailsWith<IllegalArgumentException> { createManager([rootKey, rootKey], registry) }
        assertEquals(0, factoryCalls)
    }

    @Test
    fun factoriesCannotChangeTheStackWhileCreatingAScreen() {
        lateinit var manager: ScreenManager
        var reenter = false
        val registry = ScreenRegistry {
            screen(::RootScreen)
            screen(::OtherScreen)
            screen<NumberScreen, Int?> { value ->
                if (reenter) manager.navigate(::OtherScreen, "reentrant")
                NumberScreen(value)
            }
        }
        manager = createManager(registry = registry)
        val stack = manager.backStack.toList()
        reenter = true

        assertFailsWith<IllegalArgumentException> { manager.navigate(::NumberScreen, 1) }
        assertEquals(stack, manager.backStack)

        reenter = false
        manager.navigate(::NumberScreen, 2)
        assertEquals(2, manager.requireTopScreen<NumberScreen>().value)
    }

    @Test
    fun initializationCanNavigateAfterTheStackWasPublished() {
        val registry = ScreenRegistry {
            screen(::RootScreen)
            screen(::OtherScreen)
            screen<CallbackScreen> { CallbackScreen { navigate(::OtherScreen, "redirect") } }
        }
        val manager = createManager(registry = registry)
        manager.navigate<CallbackScreen>({ error("The navigation reference must not be executed.") })
        val screen = assertIs<CallbackScreen>(manager.store[manager.backStack[1].id])

        assertTrue(screen.foundSelfDuringInitialize)
        assertEquals(1, screen.initializeCount)
        assertEquals("redirect", assertIs<OtherScreen>(top(manager)).value)
        assertEquals(3, manager.backStack.size)
    }

    @Test
    fun initializeIsNotRepeatedWhenAnInitialPageWasSelectedEarly() {
        val numberKey = key<NumberScreen>(ScreenArgs.build<Int?>(1))
        val registry = ScreenRegistry {
            screen(::NumberScreen)
            screen<CallbackScreen> { CallbackScreen { navigate(::NumberScreen, 7, policy = CreatePolicy.Resume + ClearPolicy.None) } }
        }
        val manager = createManager([key<CallbackScreen>(), numberKey], registry)
        val screen = assertIs<NumberScreen>(manager.store[numberKey.id])

        assertEquals(1, screen.initializeCount)
        assertEquals(1, screen.resumeCount)
        assertEquals(7, screen.value)
    }

    @Test
    fun initializeFailureKeepsCommittedEntryAndAllowsFurtherNavigation() {
        val failure = IllegalStateException("initialize")
        val registry = ScreenRegistry {
            screen(::RootScreen)
            screen(::OtherScreen)
            screen<CallbackScreen> { CallbackScreen { throw failure } }
        }
        val manager = createManager(registry = registry)

        val caught = assertFailsWith<IllegalStateException> {
            manager.navigate<CallbackScreen>({ error("The navigation reference must not be executed.") })
        }
        val screen = assertIs<CallbackScreen>(top(manager))
        assertSame(failure, caught)
        assertEquals(1, screen.initializeCount)
        assertEquals(0, screen.clearCount)

        manager.navigate<CallbackScreen>({ error("unused") }, policy = CreatePolicy.Move + ClearPolicy.None)
        assertEquals(1, screen.initializeCount)
        manager.navigate(::OtherScreen, "next")
        assertEquals("next", manager.requireTopScreen<OtherScreen>().value)
    }

    @Test
    fun resumeFailureKeepsCommittedSelectionAndFinishedCleanup() {
        val failure = IllegalStateException("resume")
        val registry = ScreenRegistry {
            screen(::RootScreen)
            screen(::OtherScreen)
            screen<ResumeFailureScreen> { ResumeFailureScreen(failure) }
        }
        val manager = createManager(registry = registry)
        val root = assertIs<RootScreen>(top(manager))
        manager.navigate<ResumeFailureScreen>({ error("unused") })
        val target = manager.requireTopScreen<ResumeFailureScreen>()
        manager.navigate(::OtherScreen, "previous")
        val previousTop = manager.requireTopScreen<OtherScreen>()

        val caught = assertFailsWith<IllegalStateException> {
            manager.navigate<ResumeFailureScreen>({ error("unused") }, policy = CreatePolicy.Resume + ClearPolicy.Clear)
        }

        assertSame(failure, caught)
        assertEquals([root.screenKey, target.screenKey], manager.backStack)
        assertSame(target, top(manager))
        assertEquals(1, previousTop.clearCount)
        assertEquals(0, target.clearCount)
        assertEquals(1, target.resumeCount)
        manager.navigate(::OtherScreen, "next")
        assertEquals("next", manager.requireTopScreen<OtherScreen>().value)
    }

    @Test
    fun boundScreensCannotBeReusedByAnotherManager() {
        val first = createManager()
        val original = assertIs<RootScreen>(top(first))
        val registry = ScreenRegistry { screen<RootScreen> { original } }

        assertFailsWith<IllegalArgumentException> { createManager([original.screenKey], registry) }
        assertSame(first, original.manager)
        assertSame(original, top(first))
        assertEquals(0, original.clearCount)
    }

    @Test
    fun removedScreenLivesUntilItsLastReferenceAndRejectsNavigation() {
        val manager = createManager()
        manager.navigate(::NumberScreen, 7)
        val screen = manager.requireTopScreen<NumberScreen>()
        val job = assertNotNull(screen.viewModelScope.coroutineContext[Job])

        manager.store.retain(screen.screenKey.id).use {
            assertTrue(manager.pop())
            assertFalse(manager.backStack.any { it.id == screen.screenKey.id })
            assertSame(screen, manager.store[screen.screenKey.id])
            assertEquals(0, screen.clearCount)
            assertTrue(job.isActive)
            assertFailsWith<IllegalArgumentException> { screen.navigate(::RootScreen) }
        }

        assertNull(manager.store[screen.screenKey.id])
        assertEquals(1, screen.clearCount)
        assertTrue(job.isCancelled)
    }

    @Test
    fun ownerRecreationLookupKeepsManagerResumeStateAndScreenIdentity() {
        val owner = newOwner()
        val initial = [key<RootScreen>()]
        val registry = defaultRegistry()
        val first = createManager(initial, registry, owner)
        first.navigate(::NumberScreen, 1)
        val screen = first.requireTopScreen<NumberScreen>()
        first.navigate(::NumberScreen, 9, policy = CreatePolicy.Resume + ClearPolicy.None)
        val savedKeys = first.backStack.map { ScreenKey.parse(it.toString()) }

        val restored = createManager(savedKeys, registry, owner)

        assertSame(first, restored)
        assertSame(screen, top(restored))
        assertEquals(9, assertIs<NumberScreen>(top(restored)).value)
        assertEquals(1, screen.screenKey.args.get<Int?>(0))
        assertEquals(1, screen.initializeCount)
        assertEquals(1, screen.resumeCount)
    }

    @Test
    fun managersInDifferentOwnersHaveIndependentScreensForIdenticalKeys() {
        val initial = [key<RootScreen>()]
        val registry = defaultRegistry()
        val firstOwner = newOwner()
        val first = createManager(initial, registry, firstOwner)
        val second = createManager(initial, registry)
        val firstScreen = assertIs<RootScreen>(top(first))
        val secondScreen = assertIs<RootScreen>(top(second))

        assertNotSame(first, second)
        assertNotSame(firstScreen, secondScreen)
        assertSame(first, firstScreen.manager)
        assertSame(second, secondScreen.manager)
        firstOwner.clear()

        assertTrue(first.isClosed)
        assertFalse(second.isClosed)
        assertEquals(1, firstScreen.clearCount)
        assertEquals(0, secondScreen.clearCount)
    }

    @Test
    fun managerKeysSeparateNavigationScopesInsideTheSameOwner() {
        val owner = newOwner()
        val registry = defaultRegistry()
        val initial = [key<RootScreen>()]
        val first = createManager(initial, registry, owner, name = "host.first")
        val second = createManager(initial, registry, owner, name = "host.second")
        val firstScreen = assertIs<RootScreen>(top(first))
        val secondScreen = assertIs<RootScreen>(top(second))

        assertNotSame(first, second)
        assertNotSame(firstScreen, secondScreen)
        assertEquals(firstScreen.screenKey.id, secondScreen.screenKey.id)
        first.navigate(::NumberScreen, 7)
        val added = first.requireTopScreen<NumberScreen>()
        assertEquals([secondScreen.screenKey], second.backStack)
        assertTrue(first.pop())
        assertEquals(1, added.clearCount)
        assertSame(secondScreen, top(second))
        assertEquals(0, secondScreen.clearCount)

        owner.clear()
        assertTrue(first.isClosed)
        assertTrue(second.isClosed)
        assertEquals(1, firstScreen.clearCount)
        assertEquals(1, secondScreen.clearCount)
    }

    @Test
    fun ownerCleanupClosesManagerAndDefersRetainedScreenCleanup() {
        val owner = newOwner()
        val manager = createManager(owner = owner)
        val root = assertIs<RootScreen>(top(manager))
        manager.navigate(::NumberScreen, 7)
        val screen = manager.requireTopScreen<NumberScreen>()
        val managerJob = assertNotNull(manager.viewModelScope.coroutineContext[Job])
        val screenJob = assertNotNull(screen.viewModelScope.coroutineContext[Job])

        manager.store.retain(screen.screenKey.id).use {
            owner.clear()

            assertTrue(manager.isClosed)
            assertTrue(manager.backStack.isEmpty())
            assertNull(top(manager))
            assertEquals(1, root.clearCount)
            assertEquals(0, screen.clearCount)
            assertTrue(managerJob.isCancelled)
            assertTrue(screenJob.isActive)
            assertFailsWith<IllegalArgumentException> { manager.navigate(::RootScreen) }
            assertFailsWith<IllegalArgumentException> { manager.pop() }
        }

        assertEquals(1, screen.clearCount)
        assertTrue(screenJob.isCancelled)
    }

    @Test
    fun cleanupFailureDoesNotPreventRemainingCleanupOrInitialization() {
        val failure = IllegalStateException("cleanup")
        val registry = ScreenRegistry {
            screen(::RootScreen)
            screen(::NumberScreen)
            screen(::OtherScreen)
            screen<ClearingScreen> { ClearingScreen { throw failure } }
        }
        val manager = createManager(registry = registry)
        manager.navigate(::NumberScreen, 1)
        val target = manager.requireTopScreen<NumberScreen>()
        manager.navigate<ClearingScreen>({ error("unused") })
        val failing = manager.requireTopScreen<ClearingScreen>()
        manager.navigate(::OtherScreen, "healthy")
        val healthy = manager.requireTopScreen<OtherScreen>()

        val caught = assertFailsWith<IllegalStateException> {
            manager.navigate(::NumberScreen, 7, policy = CreatePolicy.Resume + ClearPolicy.Clear)
        }

        assertSame(failure, caught)
        assertEquals(1, failing.clearCount)
        assertEquals(1, healthy.clearCount)
        assertEquals(7, target.value)
        assertEquals(1, target.resumeCount)
        assertSame(target, top(manager))
    }

    @Test
    fun cancelledCleanupDoesNotPreventRemainingCleanupOrResume() {
        lateinit var failing: ClearingScreen
        val registry = ScreenRegistry {
            screen(::RootScreen)
            screen(::NumberScreen)
            screen(::OtherScreen)
            screen<ClearingScreen> {
                ClearingScreen { failing.viewModelScope.ensureActive() }.also { failing = it }
            }
        }
        val manager = createManager(registry = registry)
        manager.navigate(::NumberScreen, 1)
        val target = manager.requireTopScreen<NumberScreen>()
        manager.navigate<ClearingScreen>({ error("unused") })
        val failingJob = assertNotNull(failing.viewModelScope.coroutineContext[Job])
        manager.navigate(::OtherScreen, "healthy")
        val healthy = manager.requireTopScreen<OtherScreen>()

        assertFailsWith<CancellationException> {
            manager.navigate(::NumberScreen, 7, policy = CreatePolicy.Resume + ClearPolicy.Clear)
        }

        assertTrue(failingJob.isCancelled)
        assertEquals(1, failing.clearCount)
        assertEquals(1, healthy.clearCount)
        assertNull(manager.store[failing.screenKey.id])
        assertNull(manager.store[healthy.screenKey.id])
        assertEquals(7, target.value)
        assertEquals(1, target.resumeCount)
        assertSame(target, top(manager))
    }

    @Test
    fun firstCleanupFailureIsKeptWhenResumeThrowsCancellation() {
        val cleanupFailure = IllegalStateException("cleanup")
        val resumeFailure = CancellationException("resume")
        val registry = ScreenRegistry {
            screen(::RootScreen)
            screen<ClearingScreen> { ClearingScreen { throw cleanupFailure } }
            screen<ResumeFailureScreen> { ResumeFailureScreen(resumeFailure) }
        }
        val manager = createManager(registry = registry)
        manager.navigate<ResumeFailureScreen>({ error("unused") })
        val target = manager.requireTopScreen<ResumeFailureScreen>()
        manager.navigate<ClearingScreen>({ error("unused") })
        val failing = manager.requireTopScreen<ClearingScreen>()

        val caught = assertFailsWith<IllegalStateException> {
            manager.navigate<ResumeFailureScreen>({ error("unused") }, policy = CreatePolicy.Resume + ClearPolicy.Clear)
        }

        assertSame(cleanupFailure, caught)
        assertEquals(1, failing.clearCount)
        assertEquals(1, target.resumeCount)
        assertEquals(0, target.clearCount)
        assertSame(target, top(manager))
    }

    @Test
    fun backStackContentsAreObservableAndDetachedCopiesKeepTheirKeys() {
        val rootKey = key<RootScreen>()
        val manager = createManager([rootKey])
        val view = manager.backStack
        val referenceReads: MutableList<Any> = []
        val observed: MutableList<Any> = []
        val old = view.toList()

        Snapshot.observe(readObserver = { referenceReads.add(it) }, writeObserver = null) {
            assertSame(view, manager.backStack)
        }
        assertTrue(referenceReads.isEmpty())

        Snapshot.observe(readObserver = { observed.add(it) }, writeObserver = null) {
            top(manager)
            manager.store[rootKey.id]
        }
        assertTrue(observed.any { it === view })

        manager.navigate(::NumberScreen, 7)
        val screen = manager.requireTopScreen<NumberScreen>()

        assertEquals([rootKey], old)
        assertSame(view, manager.backStack)
        assertEquals([rootKey, screen.screenKey], view)
        assertSame(rootKey, manager.backStack.first())
        assertSame(rootKey.args, manager.backStack.first().args)
    }

    @Test
    fun backStackViewKeepsItsIdentityWhileItsContentsChange() {
        val owner = newOwner()
        val manager = createManager(owner = owner)
        val view = manager.backStack
        val initial = view.toList()
        manager.navigate(::NumberScreen, 7)
        val screen = manager.requireTopScreen<NumberScreen>()
        val pushed = view.toList()

        assertSame(view, manager.backStack)
        assertEquals(2, view.size)
        assertEquals(1, initial.size)
        assertSame(initial.first(), pushed.first())
        assertSame(initial.first().args, pushed.first().args)

        assertTrue(manager.pop())
        assertEquals(initial, view)
        assertEquals([initial.first(), screen.screenKey], pushed)
        owner.clear()

        assertTrue(view.isEmpty())
        assertSame(view, manager.backStack)
        assertEquals(1, initial.size)
        assertEquals(2, pushed.size)
    }

    @Test
    fun multiStepNavigationPublishesOnlyTheCompleteStack() {
        for (create in [CreatePolicy.Replace, CreatePolicy.Move, CreatePolicy.Resume]) {
            for (clear in ClearPolicy.entries) {
                val manager = createManager()
                val root = assertIs<RootScreen>(top(manager))
                manager.navigate(::NumberScreen, 1)
                manager.navigate(::OtherScreen, "upper")
                val upper = manager.requireTopScreen<OtherScreen>()
                val observed: MutableList<Any> = []
                val published: MutableList<List<ScreenKey>> = []
                Snapshot.observe(readObserver = { observed.add(it) }, writeObserver = null) { manager.backStack.size }
                val observer = Snapshot.registerApplyObserver { changed, _ ->
                    if (changed.any { state -> observed.any { it === state } }) published.add(manager.backStack.toList())
                }

                cleaning(observer::dispose) {
                    manager.navigate(::NumberScreen, 7, policy = create + clear)
                    val result = manager.requireTopScreen<NumberScreen>()
                    val expected = if (clear == ClearPolicy.Clear) [root.screenKey, result.screenKey]
                    else [root.screenKey, upper.screenKey, result.screenKey]

                    assertEquals([expected], published, "$create + $clear")
                    assertEquals(expected, manager.backStack)
                }
            }
        }
    }

    @Test
    fun failedSnapshotCommitClearsTheUnpublishedScreenAndKeepsTheStack() {
        var created: NumberScreen? = null
        val registry = ScreenRegistry {
            screen(::RootScreen)
            screen<NumberScreen, Int?> { value -> NumberScreen(value).also { created = it } }
        }
        val manager = createManager(registry = registry)
        val before = manager.backStack.toList()
        val snapshot = Snapshot.takeSnapshot()

        cleaning(snapshot::dispose) {
            assertFailsWith<IllegalStateException> { snapshot.enter { manager.navigate(::NumberScreen, 7) } }
        }

        val screen = assertNotNull(created)
        assertEquals(1, screen.clearCount)
        assertEquals(0, screen.initializeCount)
        assertNull(manager.store[screen.screenKey.id])
        assertEquals(before, manager.backStack)
        assertEquals(0, assertIs<RootScreen>(top(manager)).clearCount)
    }

    @Test
    fun anEmptyManagerCanCreateItsFirstPage() {
        val manager = createManager(initial = [])
        assertEquals(0, manager.backStack.size)
        assertNull(top(manager))
        assertFalse(manager.pop())

        manager.navigate(::NumberScreen, 7)
        val screen = manager.requireTopScreen<NumberScreen>()
        assertSame(screen, top(manager))
        assertEquals(1, manager.backStack.size)
        assertEquals(1, screen.initializeCount)
    }
}
