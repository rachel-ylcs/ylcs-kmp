package love.yinlin.compose.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName
import kotlin.test.*

@OptIn(CompatibleRachelApi::class)
class ScreenNavigationTest {
    private open class ProbeScreen : TestScreen() {
        var initializeCount = 0
        var clearCount = 0
        var initializeAction: ProbeScreen.() -> Unit = {}

        override fun initialize() {
            ++initializeCount
            initializeAction()
        }

        override fun uninitialize() { ++clearCount }
    }

    private class HomeScreen : ProbeScreen()

    private class CounterScreen(initialValue: Int) : ProbeScreen() {
        var value: Int by mutableStateOf(initialValue)
        var resumeCount = 0

        override fun resume() = withResume { next: Int ->
            value = next
            ++resumeCount
        }
    }

    private val owners: MutableList<ViewModelStore> = []
    private val managerId = ScreenID().toString()
    private val registry = ScreenRegistry {
        screen(::HomeScreen)
        screen(::CounterScreen)
    }

    private fun owner(): ViewModelStore = ViewModelStore().also { owners += it }

    private inline fun <reified Main : ScreenModel> createManager(
        owner: ViewModelStore = owner(),
        id: String = this.managerId,
        registry: ScreenRegistry = this.registry
    ): ScreenManager = ScreenManager.obtainScreenManager(owner, id, metaClassName<Main>()) { registry }

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
    fun mainScreenIsCreatedOnlyWhenTheManagerIsFirstCreated() {
        val owner = owner()
        var factoryCount = 0
        val registry = ScreenRegistry {
            screen<HomeScreen> {
                ++factoryCount
                HomeScreen()
            }
        }
        val manager = createManager<HomeScreen>(owner, registry = registry)
        val screen = assertIs<HomeScreen>(manager.store[manager.backStack.single().id])

        repeat(3) { assertSame(manager, createManager<HomeScreen>(owner, registry = registry)) }

        assertEquals(1, factoryCount)
        assertEquals(1, manager.backStack.size)
        assertEquals(1, screen.initializeCount)
        assertEquals(0, screen.clearCount)
    }

    @Test
    fun managerIsStoredUnderItsUuidWithoutAPrefix() {
        val owner = owner()
        val manager = createManager<HomeScreen>(owner)
        val provider = ViewModelProvider.create(
            store = owner,
            factory = viewModelFactory {
                addInitializer(ScreenManager::class) {
                    error("The UUID must locate the existing manager directly.")
                }
            }
        )

        assertSame(manager, provider[managerId, ScreenManager::class])
    }

    @Test
    fun registryIsBuiltOnlyWhenTheManagerIsFirstCreated() {
        val owner = owner()
        var registrationCount = 0
        var factoryCount = 0
        val manager = ScreenManager.obtainScreenManager(owner, managerId, metaClassName<HomeScreen>()) {
            ScreenRegistry {
                ++registrationCount
                screen<HomeScreen> {
                    ++factoryCount
                    HomeScreen()
                }
            }
        }

        repeat(3) {
            val retained = ScreenManager.obtainScreenManager(owner, managerId, metaClassName<HomeScreen>()) {
                error("A retained manager must not rebuild its registry.")
            }
            assertSame(manager, retained)
        }

        assertEquals(1, registrationCount)
        assertEquals(1, factoryCount)
        assertEquals(1, manager.backStack.size)
    }

    @Test
    fun failingRegistrationDoesNotOccupyTheManagerKey() {
        val owner = owner()
        val failure = IllegalStateException("Screen registration failed.")

        assertSame(failure, assertFailsWith<IllegalStateException> {
            ScreenManager.obtainScreenManager(owner, managerId, metaClassName<HomeScreen>()) {
                ScreenRegistry { throw failure }
            }
        })

        val manager = createManager<HomeScreen>(owner)

        assertFalse(manager.isClosed)
        assertIs<HomeScreen>(manager.store[manager.backStack.single().id])
        assertSame(manager, createManager<HomeScreen>(owner))
    }

    @Test
    fun genericMainUsesTheRegisteredFactoryWithNoArguments() {
        val manager = createManager<HomeScreen>()
        val key = manager.backStack.single()
        val screen = assertIs<HomeScreen>(manager.store[key.id])

        assertSame(ScreenArgs.Empty, key.args)
        assertTrue(key.args.isEmpty)
        assertEquals(metaClassName<HomeScreen>(), key.type)
        assertSame(manager, screen.manager)
        assertSame(key, screen.screenKey)
        assertEquals(1, screen.initializeCount)
        assertEquals(key, ScreenKey.parse(key.toString()))
        assertFalse(manager.pop())
    }

    @Test
    fun retrievingTheRetainedManagerPreservesResumeStateAndItsEntry() {
        val owner = owner()
        val manager = createManager<HomeScreen>(owner)
        manager.navigate(::CounterScreen, 1)
        val screen = manager.requireTopScreen<CounterScreen>()
        val key = screen.screenKey
        val entry = manager.navEntry(key)
        manager.navigate(::HomeScreen)
        manager.navigate(::CounterScreen, 9, policy = CreatePolicy.Resume + ClearPolicy.None)
        screen.value = 12
        val backStack = manager.backStack
        val savedKeys = backStack.toList()

        val recreatedRegistry = ScreenRegistry {
            screen<HomeScreen> { error("A retained manager must not use the new registry.") }
            screen<CounterScreen, Int> { error("A retained screen must not be recreated.") }
        }
        val retained = createManager<HomeScreen>(owner, registry = recreatedRegistry)

        assertSame(manager, retained)
        assertSame(screen, retained.store[key.id])
        assertSame(entry, retained.navEntry(key))
        assertSame(backStack, retained.backStack)
        assertEquals(savedKeys, retained.backStack)
        assertSame(key, retained.backStack.last())
        assertEquals(12, screen.value)
        assertEquals(1, screen.resumeCount)
        assertEquals(1, key.args.get<Int>(0))
        assertEquals(1, screen.initializeCount)
    }

    @Test
    fun differentKeysInTheSameOwnerHaveIndependentNavigationStacks() {
        val owner = owner()
        val firstId = ScreenID().toString()
        val secondId = ScreenID().toString()
        val first = createManager<HomeScreen>(owner, id = firstId)
        val second = createManager<HomeScreen>(owner, id = secondId)
        first.navigate(::CounterScreen, 7)
        val screen = first.requireTopScreen<CounterScreen>()

        assertNotSame(first, second)
        assertSame(first, createManager<HomeScreen>(owner, id = firstId))
        assertSame(second, createManager<HomeScreen>(owner, id = secondId))
        assertNotSame(first.store.provider, second.store.provider)
        assertEquals(2, first.backStack.size)
        assertEquals(1, second.backStack.size)
        assertNull(second.store[screen.screenKey.id])
        assertTrue(first.pop())
        assertEquals(1, screen.clearCount)
        assertFalse(second.isClosed)
    }

    @Test
    fun identicalKeysInDifferentOwnersHaveIndependentManagers() {
        val firstOwner = owner()
        val secondOwner = owner()
        val first = createManager<HomeScreen>(firstOwner)
        val second = createManager<HomeScreen>(secondOwner)
        val firstScreen = assertIs<HomeScreen>(first.store[first.backStack.single().id])
        val secondScreen = assertIs<HomeScreen>(second.store[second.backStack.single().id])

        firstOwner.clear()

        assertNotSame(first, second)
        assertTrue(first.isClosed)
        assertEquals(1, firstScreen.clearCount)
        assertFalse(second.isClosed)
        assertEquals(0, secondScreen.clearCount)
        assertSame(second, createManager<HomeScreen>(secondOwner))
    }

    @Test
    fun clearingTheOwnerWaitsForTheScreensLastUiReference() {
        val owner = owner()
        val manager = createManager<HomeScreen>(owner)
        val home = assertIs<HomeScreen>(manager.store[manager.backStack.single().id])
        manager.navigate(::CounterScreen, 7)
        val screen = manager.requireTopScreen<CounterScreen>()
        val key = screen.screenKey
        val entry = manager.navEntry(key)
        val job = assertNotNull(screen.viewModelScope.coroutineContext[Job])
        val reference = manager.store.provider.acquireToken(entry.contentKey)

        try {
            owner.clear()

            assertTrue(manager.isClosed)
            assertTrue(manager.store.isClosed)
            assertTrue(manager.backStack.isEmpty())
            assertEquals(1, home.clearCount)
            assertEquals(0, screen.clearCount)
            assertSame(entry, manager.navEntry(key))
            assertTrue(job.isActive)
        }
        finally { reference.close() }

        assertEquals(1, screen.clearCount)
        assertTrue(job.isCancelled)
        assertNull(manager.store[key.id])
    }

    @Test
    fun nestedManagerSurvivesACoveredPageAndClearsWithThatPage() {
        val manager = createManager<HomeScreen>()
        manager.navigate(::CounterScreen, 10)
        val parentScreen = manager.requireTopScreen<CounterScreen>()

        val parentStore = manager.store.provider.getOrCreate(parentScreen.screenKey.id.toString())
        val nestedId = ScreenID().toString()
        val nested = createManager<HomeScreen>(parentStore, id = nestedId)
        nested.navigate(::CounterScreen, 20)
        val childScreen = nested.requireTopScreen<CounterScreen>()
        childScreen.value = 25

        manager.navigate(::HomeScreen)

        assertSame(nested, createManager<HomeScreen>(parentStore, id = nestedId))
        assertEquals(25, childScreen.value)
        assertEquals(0, childScreen.clearCount)
        assertFalse(nested.isClosed)

        assertTrue(manager.pop())
        assertFalse(nested.isClosed)
        assertTrue(manager.pop())

        assertTrue(nested.isClosed)
        assertTrue(nested.backStack.isEmpty())
        assertEquals(1, parentScreen.clearCount)
        assertEquals(1, childScreen.clearCount)
    }

    @Test
    fun nestedManagerWaitsForItsParentPagesLastUiReference() {
        val manager = createManager<HomeScreen>()
        manager.navigate(::CounterScreen, 10)
        val parent = manager.requireTopScreen<CounterScreen>()
        val parentEntry = manager.navEntry(parent.screenKey)
        val parentStore = manager.store.provider.getOrCreate(parentEntry.contentKey)
        val nestedId = ScreenID().toString()
        val nested = createManager<HomeScreen>(parentStore, id = nestedId)
        nested.navigate(::CounterScreen, 20)
        val child = nested.requireTopScreen<CounterScreen>()
        val childJob = assertNotNull(child.viewModelScope.coroutineContext[Job])
        val reference = manager.store.provider.acquireToken(parentEntry.contentKey)

        try {
            assertTrue(manager.pop())

            assertFalse(nested.isClosed)
            assertSame(nested, createManager<HomeScreen>(parentStore, id = nestedId))
            assertEquals(0, parent.clearCount)
            assertEquals(0, child.clearCount)
            assertTrue(childJob.isActive)
        }
        finally { reference.close() }

        assertTrue(nested.isClosed)
        assertTrue(nested.backStack.isEmpty())
        assertEquals(1, parent.clearCount)
        assertEquals(1, child.clearCount)
        assertTrue(childJob.isCancelled)
    }

    @Test
    fun failingMainInitializationCleansUpAndAllowsANewManagerToBeCreated() {
        val owner = owner()
        val failure = IllegalStateException("Main initialization failed.")
        var failedManager: ScreenManager? = null
        var createdScreen: HomeScreen? = null
        var screenJob: Job? = null
        val failingRegistry = ScreenRegistry {
            screen<HomeScreen> {
                HomeScreen().also {
                    createdScreen = it
                    it.initializeAction = {
                        failedManager = manager
                        screenJob = viewModelScope.coroutineContext[Job]
                        throw failure
                    }
                }
            }
        }

        val actual = assertFailsWith<IllegalStateException> {
            createManager<HomeScreen>(owner, registry = failingRegistry)
        }

        assertSame(failure, actual)
        assertTrue(assertNotNull(failedManager).isClosed)
        assertEquals(1, assertNotNull(createdScreen).initializeCount)
        assertEquals(1, assertNotNull(createdScreen).clearCount)
        assertTrue(assertNotNull(screenJob).isCancelled)

        val replacement = createManager<HomeScreen>(owner)

        assertNotSame(failedManager, replacement)
        assertFalse(replacement.isClosed)
        assertEquals(1, replacement.backStack.size)
    }

    @Test
    fun failingMainFactoryCanRetryWithoutRetainingAPartialManager() {
        val owner = owner()
        val failure = IllegalStateException("Main factory failed.")
        var factoryCount = 0
        var fail = true
        val registry = ScreenRegistry {
            screen<HomeScreen> {
                ++factoryCount
                if (fail) throw failure
                HomeScreen()
            }
        }

        assertSame(failure, assertFailsWith<IllegalStateException> {
            createManager<HomeScreen>(owner, registry = registry)
        })

        fail = false
        val manager = createManager<HomeScreen>(owner, registry = registry)
        val screen = assertIs<HomeScreen>(manager.store[manager.backStack.single().id])

        assertEquals(2, factoryCount)
        assertFalse(manager.isClosed)
        assertEquals(1, screen.initializeCount)
        assertEquals(0, screen.clearCount)
        assertSame(manager, createManager<HomeScreen>(owner, registry = registry))
        assertEquals(2, factoryCount)
    }

    @Test
    fun genericMainRejectsAFactoryThatRequiresArguments() {
        val owner = owner()

        assertFailsWith<IllegalArgumentException> { createManager<CounterScreen>(owner) }

        val manager = createManager<HomeScreen>(owner)

        assertIs<HomeScreen>(manager.store[manager.backStack.single().id])
        assertTrue(manager.backStack.single().args.isEmpty)
        assertFalse(manager.isClosed)
    }

    @Test
    fun unregisteredMainUsesDefault404AndRetainsTheManager() {
        val owner = owner()
        val manager = createManager<HomeScreen>(owner, registry = ScreenRegistry {})
        val key = manager.backStack.single()

        assertEquals(metaClassName<HomeScreen>(), key.type)
        assertIs<Screen404>(manager.store[key.id])
        assertEquals(1, manager.backStack.size)
        assertFalse(manager.isClosed)
        assertSame(manager, createManager<HomeScreen>(owner))
        assertIs<Screen404>(manager.store[key.id])
    }
}
