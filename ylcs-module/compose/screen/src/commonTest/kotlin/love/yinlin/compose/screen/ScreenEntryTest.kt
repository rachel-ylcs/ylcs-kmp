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
import love.yinlin.extension.cleaning
import love.yinlin.reflect.metaClassName
import kotlin.test.*

@OptIn(CompatibleRachelApi::class)
class ScreenEntryTest {
    private open class ProbeScreen : TestScreen() {
        var initializeCount = 0
        var clearCount = 0

        override fun initialize() { ++initializeCount }
        override fun uninitialize() { ++clearCount }
    }

    private class HomeScreen : ProbeScreen()

    private class CounterScreen(initialValue: Int) : ProbeScreen() {
        var value: Int by mutableStateOf(initialValue)
        var resumeCount = 0

        override fun resume() = withResume { next: Int ->
            ++resumeCount
            value = next
        }
    }

    private class SimpleScreen : TestScreen()

    private val owners: MutableList<ViewModelStore> = []
    private var counterFactoryCalls = 0

    private inline fun <reified S : ScreenModel> key(args: ScreenArgs = ScreenArgs.Empty): ScreenKey =
        ScreenKey(type = metaClassName<S>(), args = args)

    private fun createManager(initial: List<ScreenKey> = [key<HomeScreen>()]): ScreenManager {
        val owner = ViewModelStore().also { owners += it }
        val registry = ScreenRegistry {
            screen(::HomeScreen)
            screen { value: Int ->
                ++counterFactoryCalls
                CounterScreen(value)
            }
            screen(::SimpleScreen)
        }
        return ViewModelProvider.create(
            store = owner,
            factory = viewModelFactory { addInitializer(ScreenManager::class) { ScreenManager(registry, initial) } }
        )[TestManagerID, ScreenManager::class]
    }

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
    fun entryLookupReusesTheInstanceWithoutCallingItsFactoryOrInitializeAgain() {
        val manager = createManager()
        manager.navigate(::CounterScreen, 7)
        val screen = manager.requireTopScreen<CounterScreen>()
        val key = screen.screenKey
        val args = key.args
        val entry = manager.navEntry(key)

        repeat(3) { assertSame(entry, manager.navEntry(key)) }

        assertEquals(1, counterFactoryCalls)
        assertEquals(1, screen.initializeCount)
        assertEquals(0, screen.resumeCount)
        assertEquals(0, screen.clearCount)
        assertSame(key, screen.screenKey)
        assertSame(args, screen.screenKey.args)
        assertEquals(key.id.toString(), assertIs<String>(entry.contentKey))
        assertTrue(entry.metadata.isEmpty())
    }

    @Test
    fun identicalTypesAndArgumentsStillHaveIndependentContentKeys() {
        val manager = createManager()
        manager.navigate(::CounterScreen, 7)
        val first = manager.requireTopScreen<CounterScreen>()
        manager.navigate(::CounterScreen, 7)
        val second = manager.requireTopScreen<CounterScreen>()
        val firstEntry = manager.navEntry(first.screenKey)
        val secondEntry = manager.navEntry(second.screenKey)

        assertNotSame(first, second)
        assertNotSame(firstEntry, secondEntry)
        assertNotEquals(firstEntry.contentKey, secondEntry.contentKey)
        assertEquals(first.screenKey.id.toString(), firstEntry.contentKey)
        assertEquals(second.screenKey.id.toString(), secondEntry.contentKey)
        assertEquals(2, counterFactoryCalls)
    }

    @Test
    fun moveAndResumeKeepTheEntryAndItsInitialArguments() {
        val manager = createManager()
        manager.navigate(::CounterScreen, 1)
        val screen = manager.requireTopScreen<CounterScreen>()
        val key = screen.screenKey
        val entry = manager.navEntry(key)
        manager.navigate(::HomeScreen)

        manager.navigate(::CounterScreen, 2, policy = CreatePolicy.Move + ClearPolicy.None)
        assertSame(screen, manager.requireTopScreen<CounterScreen>())
        assertSame(entry, manager.navEntry(key))
        assertEquals(1, screen.value)

        manager.navigate(::CounterScreen, 9, policy = CreatePolicy.Resume + ClearPolicy.None)
        assertSame(screen, manager.requireTopScreen<CounterScreen>())
        assertSame(entry, manager.navEntry(key))
        assertEquals(9, screen.value)
        assertEquals(1, key.args.get<Int>(0))
        assertEquals(1, screen.initializeCount)
        assertEquals(1, screen.resumeCount)
        assertEquals(1, counterFactoryCalls)
    }

    @Test
    fun newAndReplaceCreateNewEntriesWhileKeepingOlderInstancesIndependent() {
        val manager = createManager()
        manager.navigate(::CounterScreen, 1)
        val first = manager.requireTopScreen<CounterScreen>()
        val firstEntry = manager.navEntry(first.screenKey)
        manager.navigate(::CounterScreen, 2)
        val second = manager.requireTopScreen<CounterScreen>()
        val secondEntry = manager.navEntry(second.screenKey)
        manager.navigate(::CounterScreen, 3, policy = CreatePolicy.Replace + ClearPolicy.None)
        val replacement = manager.requireTopScreen<CounterScreen>()
        val replacementEntry = manager.navEntry(replacement.screenKey)

        assertNotSame(firstEntry, secondEntry)
        assertNotSame(secondEntry, replacementEntry)
        assertNotEquals(secondEntry.contentKey, replacementEntry.contentKey)
        assertSame(firstEntry, manager.navEntry(first.screenKey))
        assertEquals(0, first.clearCount)
        assertEquals(1, second.clearCount)
        assertFailsWith<IllegalArgumentException> { manager.navEntry(second.screenKey) }
    }

    @Test
    fun restoredKeysKeepTheirContentIdsAndManagersKeepTheirOwnEntries() {
        val originalKey = key<CounterScreen>(ScreenArgs.build(7))
        val restoredKey = ScreenKey.parse(originalKey.toString())
        val firstManager = createManager([originalKey])
        val secondManager = createManager([restoredKey])
        val firstEntry = firstManager.navEntry(originalKey)
        val secondEntry = secondManager.navEntry(restoredKey)

        assertNotSame(firstEntry, secondEntry)
        assertEquals(originalKey.id.toString(), firstEntry.contentKey)
        assertEquals(firstEntry.contentKey, secondEntry.contentKey)
        assertNotSame(firstManager.store[originalKey.id], secondManager.store[restoredKey.id])

        owners.first().clear()

        assertSame(secondEntry, secondManager.navEntry(restoredKey))
        assertEquals(0, assertIs<CounterScreen>(secondManager.store[restoredKey.id]).clearCount)
    }

    @Test
    fun unknownEntriesAndConflictingTypesAreRejectedWithoutCreatingScreens() {
        val manager = createManager()
        val homeKey = manager.backStack.single()
        val homeEntry = manager.navEntry(homeKey)
        val unknown = key<CounterScreen>(ScreenArgs.build(7))
        val conflicting = homeKey.copy(type = unknown.type)

        assertFailsWith<IllegalArgumentException> { manager.navEntry(unknown) }
        assertFailsWith<IllegalArgumentException> { manager.navEntry(conflicting) }

        assertEquals(0, counterFactoryCalls)
        assertSame(homeEntry, manager.navEntry(homeKey))
        assertEquals([homeKey], manager.backStack)
    }

    @Test
    fun basicScreensProvideEntriesWithoutAnotherBaseClass() {
        val manager = createManager()
        manager.navigate(::SimpleScreen)
        val screen = manager.requireTopScreen<SimpleScreen>()

        assertSame(screen.navEntry, manager.navEntry(screen.screenKey))
        assertEquals(screen.screenKey.id.toString(), screen.navEntry.contentKey)

        assertSame(screen, manager.store[screen.screenKey.id])
        assertEquals(2, manager.backStack.size)
    }

    @Test
    fun uiProviderUsesTheManagersExistingScreenStore() {
        val manager = createManager()
        manager.navigate(::CounterScreen, 7)
        val screen = manager.requireTopScreen<CounterScreen>()
        val entry = manager.navEntry(screen.screenKey)
        val uiStore = manager.store.provider.getOrCreate(entry.contentKey)
        val model = ViewModelProvider.create(
            store = uiStore,
            factory = viewModelFactory {
                addInitializer(ScreenModel::class) { error("UI must reuse the managed screen.") }
            }
        )[ScreenStore.DEFAULT_SCREEN_VIEWMODEL_KEY, ScreenModel::class]

        assertSame(screen, model)
        assertEquals(1, counterFactoryCalls)
        assertEquals(1, screen.initializeCount)
    }

    @Test
    fun providerUiTokenDefersCleanupUntilTheOutgoingContentIsDisposed() {
        val manager = createManager()
        manager.navigate(::CounterScreen, 7)
        val screen = manager.requireTopScreen<CounterScreen>()
        val entry = manager.navEntry(screen.screenKey)

        val reference = manager.store.provider.acquireToken(entry.contentKey)

        cleaning(reference::close) {
            assertTrue(manager.pop())
            assertSame(entry, manager.navEntry(screen.screenKey))
            assertEquals(0, screen.clearCount)
        }

        manager.store.provider.clearKey(entry.contentKey)

        assertEquals(1, screen.clearCount)
        assertNull(manager.store[screen.screenKey.id])
    }

    @Test
    fun poppedEntryRemainsAvailableUntilItsLastUiReferenceIsReleased() {
        val manager = createManager()
        manager.navigate(::CounterScreen, 7)
        val screen = manager.requireTopScreen<CounterScreen>()
        val key = screen.screenKey
        val entry = manager.navEntry(key)
        val job = assertNotNull(screen.viewModelScope.coroutineContext[Job])
        val reference = manager.store.retain(key.id)

        cleaning(reference::close) {
            assertTrue(manager.pop())
            assertFalse(key.id in manager.store)
            assertSame(entry, manager.navEntry(key))
            assertEquals(0, screen.clearCount)
            assertTrue(job.isActive)
        }

        assertEquals(1, screen.clearCount)
        assertTrue(job.isCancelled)
        assertFailsWith<IllegalArgumentException> { manager.navEntry(key) }
    }

    @Test
    fun cachedEntryDoesNotDelayCleanupWithoutAUiReference() {
        val manager = createManager()
        manager.navigate(::CounterScreen, 7)
        val screen = manager.requireTopScreen<CounterScreen>()
        val entry = manager.navEntry(screen.screenKey)

        assertTrue(manager.pop())

        assertEquals(1, screen.clearCount)
        assertEquals(screen.screenKey.id.toString(), entry.contentKey)
        assertFailsWith<IllegalArgumentException> { manager.navEntry(screen.screenKey) }
    }
}
