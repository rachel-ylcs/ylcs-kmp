package love.yinlin.compose.screen

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlin.test.*

class ScreenStoreTest {
    private class ProbeScreen(val initialValue: String = "initial", private val afterUninitialize: () -> Unit = {}) : TestScreen() {
        var value: String = initialValue

        var clearCount: Int = 0
            private set

        override fun uninitialize() {
            ++clearCount
            afterUninitialize()
        }
    }

    private class DetailScreen(val itemId: Long) : TestScreen() {
        var clearCount: Int = 0
            private set

        override fun uninitialize() {
            ++clearCount
        }
    }

    private class TestFailure(message: String) : RuntimeException(message)

    private val resources: MutableList<AutoCloseable> = []

    private fun <T : AutoCloseable> track(resource: T): T = resource.also { resources += it }

    private fun store(): ScreenStore = track(ScreenStore())

    private fun retain(store: ScreenStore, id: ScreenID): ScreenStoreReference = track(store.retain(id))

    private fun create(store: ScreenStore, id: ScreenID, afterUninitialize: () -> Unit = {}): ProbeScreen =
        assertIs<ProbeScreen>(store.create(id) { ProbeScreen(afterUninitialize = afterUninitialize) })

    @AfterTest
    fun releaseResources() {
        var failure: Throwable? = null
        for (resource in resources.asReversed()) {
            runCatching { resource.close() }.exceptionOrNull()?.let { cleanupFailure ->
                val previous = failure
                if (previous == null) failure = cleanupFailure
                else if (previous !== cleanupFailure) previous.addSuppressed(cleanupFailure)
            }
        }
        resources.clear()
        failure?.let { throw it }
    }

    @Test
    fun createAndLookupsReturnSameScreen() {
        val store = store()
        val id = ScreenID()
        var factoryCalls = 0
        val screen = assertIs<ProbeScreen>(store.create(id) {
            ++factoryCalls
            ProbeScreen()
        })

        repeat(3) {
            assertTrue(id in store)
            assertSame(screen, store[id])
        }
        assertEquals(1, factoryCalls)
        assertEquals(0, screen.clearCount)
        assertFalse(store.isClosed)
    }

    @Test
    fun factoryCapturesArgumentsAndScreenOwnsCurrentState() {
        val store = store()
        val id = ScreenID()
        val initialValue = "from route"
        val screen = assertIs<ProbeScreen>(store.create(id) { ProbeScreen(initialValue) })

        assertEquals(initialValue, screen.initialValue)
        assertEquals(initialValue, screen.value)

        screen.value = "updated"
        retain(store, id).close()

        assertSame(screen, store[id])
        assertEquals("updated", assertIs<ProbeScreen>(store[id]).value)
        assertEquals(initialValue, screen.initialValue)
        assertEquals(0, screen.clearCount)
    }

    @Test
    fun duplicateIdentityDoesNotReplaceExistingScreen() {
        val store = store()
        val id = ScreenID()
        val original = create(store, id)
        var factoryCalls = 0

        assertFailsWith<IllegalArgumentException> {
            store.create(id) {
                ++factoryCalls
                ProbeScreen()
            }
        }

        assertEquals(0, factoryCalls)
        assertSame(original, store[id])
        assertTrue(id in store)
        assertEquals(0, original.clearCount)
    }

    @Test
    fun sameClassDifferentIdentitiesHaveIndependentStores() {
        val store = store()
        val firstId = ScreenID()
        val secondId = ScreenID()
        val first = create(store, firstId)
        val second = create(store, secondId)

        assertNotSame(first, second)
        assertNotSame(store.provider.getOrCreate(firstId.toString()), store.provider.getOrCreate(secondId.toString()))
        store.remove(firstId)

        assertEquals(1, first.clearCount)
        assertEquals(0, second.clearCount)
        assertNull(store[firstId])
        assertSame(second, store[secondId])
        assertTrue(secondId in store)
    }

    @Test
    fun differentScreenTypesKeepTheirConcreteInstances() {
        val store = store()
        val firstId = ScreenID()
        val secondId = ScreenID()
        val first = store.create(firstId) { ProbeScreen("first") }
        val second = store.create(secondId) { DetailScreen(42L) }
        val firstScreen = assertIs<ProbeScreen>(store[firstId])
        val secondScreen = assertIs<DetailScreen>(store[secondId])

        assertSame(first, firstScreen)
        assertSame(second, secondScreen)
        assertEquals("first", firstScreen.initialValue)
        assertEquals(42L, secondScreen.itemId)

        store.remove(firstId)

        assertEquals(1, firstScreen.clearCount)
        assertEquals(0, secondScreen.clearCount)
        assertSame(second, store[secondId])
        store.close()
        assertEquals(1, secondScreen.clearCount)
    }

    @Test
    fun sameIdentityInDifferentScopesDoesNotShareScreens() {
        val firstStore = store()
        val secondStore = store()
        val id = ScreenID()
        val first = create(firstStore, id)
        val second = create(secondStore, id)

        assertNotSame(first, second)
        assertNotSame(firstStore.provider.getOrCreate(id.toString()), secondStore.provider.getOrCreate(id.toString()))
        firstStore.close()

        assertEquals(1, first.clearCount)
        assertEquals(0, second.clearCount)
        assertSame(second, secondStore[id])
        assertTrue(id in secondStore)
        assertFalse(secondStore.isClosed)
    }

    @Test
    fun missingEntryLookupsDoNotCreateAnything() {
        val store = store()
        val id = ScreenID()

        assertNull(store[id])
        assertFalse(id in store)
        store.remove(id)
        assertFailsWith<IllegalArgumentException> { store.retain(id) }

        val screen = create(store, id)
        assertSame(screen, store[id])
        assertTrue(id in store)
    }

    @Test
    fun removeWithoutReferencesClearsImmediatelyAndOnlyOnce() {
        val store = store()
        val id = ScreenID()
        val screen = create(store, id)

        store.remove(id)

        assertFalse(id in store)
        assertNull(store[id])
        assertEquals(1, screen.clearCount)
        assertFailsWith<IllegalArgumentException> { store.retain(id) }
        store.remove(id)
        store.close()
        assertEquals(1, screen.clearCount)
    }

    @Test
    fun removeWithReferencePreservesScreenUntilRelease() {
        val store = store()
        val id = ScreenID()
        val screen = create(store, id)
        val reference = retain(store, id)

        store.remove(id)

        assertFalse(id in store)
        assertSame(screen, store[id])
        assertEquals(0, screen.clearCount)

        reference.close()

        assertNull(store[id])
        assertEquals(1, screen.clearCount)
    }

    @Test
    fun lastReferenceControlsCleanupAndRepeatedCloseIsHarmless() {
        val store = store()
        val id = ScreenID()
        val screen = create(store, id)
        val firstReference = retain(store, id)
        val secondReference = retain(store, id)

        store.remove(id)
        store.remove(id)
        firstReference.close()
        firstReference.close()

        assertSame(screen, store[id])
        assertEquals(0, screen.clearCount)

        secondReference.close()
        secondReference.close()
        store.remove(id)

        assertNull(store[id])
        assertEquals(1, screen.clearCount)
    }

    @Test
    fun releasingReferenceDoesNotRemoveActiveEntry() {
        val store = store()
        val id = ScreenID()
        val screen = create(store, id)
        val reference = retain(store, id)

        reference.close()

        assertTrue(id in store)
        assertSame(screen, store[id])
        assertEquals(0, screen.clearCount)
        store.remove(id)
        assertEquals(1, screen.clearCount)
    }

    @Test
    fun removedEntryRejectsNewReferencesAndRecreation() {
        val store = store()
        val id = ScreenID()
        val screen = create(store, id)
        val reference = retain(store, id)
        var factoryCalls = 0

        store.remove(id)

        assertFailsWith<IllegalArgumentException> { store.retain(id) }
        assertFailsWith<IllegalArgumentException> {
            store.create(id) {
                ++factoryCalls
                ProbeScreen()
            }
        }
        assertEquals(0, factoryCalls)
        assertSame(screen, store[id])
        assertEquals(0, screen.clearCount)

        reference.close()
        assertNull(store[id])
        assertEquals(1, screen.clearCount)
    }

    @Test
    fun closeClearsAllEntriesAndIsIdempotent() {
        val store = store()
        val ids = [ScreenID(), ScreenID(), ScreenID()]
        val screens = ids.map { create(store, it) }

        store.close()
        store.close()

        assertTrue(store.isClosed)
        for ([index, id] in ids.withIndex()) {
            assertFalse(id in store)
            assertNull(store[id])
            assertEquals(1, screens[index].clearCount)
        }
    }

    @Test
    fun closeWaitsForRetainedEntriesButClearsOtherEntries() {
        val store = store()
        val retainedId = ScreenID()
        val otherId = ScreenID()
        val retained = create(store, retainedId)
        val other = create(store, otherId)
        val reference = retain(store, retainedId)

        store.close()
        store.close()

        assertTrue(store.isClosed)
        assertFalse(retainedId in store)
        assertFalse(otherId in store)
        assertSame(retained, store[retainedId])
        assertNull(store[otherId])
        assertEquals(0, retained.clearCount)
        assertEquals(1, other.clearCount)

        reference.close()

        assertNull(store[retainedId])
        assertEquals(1, retained.clearCount)
    }

    @Test
    fun closedScopeRejectsCreateAndRetain() {
        val store = store()
        val id = ScreenID()
        val screen = create(store, id)
        val reference = retain(store, id)
        var factoryCalls = 0

        store.close()

        assertFailsWith<IllegalArgumentException> { store.retain(id) }
        assertFailsWith<IllegalArgumentException> {
            store.create(ScreenID()) {
                ++factoryCalls
                ProbeScreen()
            }
        }
        assertEquals(0, factoryCalls)
        assertSame(screen, store[id])

        reference.close()
        assertNull(store[id])
    }

    @Test
    fun providerClearAllDoesNotClearActiveManagerScope() {
        val store = store()
        val id = ScreenID()
        val screen = create(store, id)

        store.provider.clearAllKeys()

        assertTrue(id in store)
        assertSame(screen, store[id])
        assertEquals(0, screen.clearCount)
        store.close()
        assertEquals(1, screen.clearCount)
    }

    @Test
    fun providerClearKeyWaitsForStackReference() {
        val store = store()
        val id = ScreenID()
        val screen = create(store, id)

        store.provider.clearKey(id.toString())

        assertTrue(id in store)
        assertSame(screen, store[id])
        assertEquals(0, screen.clearCount)
        store.remove(id)
        assertNull(store[id])
        assertEquals(1, screen.clearCount)
    }

    @Test
    fun providerTokenForIdStringDelaysCleanup() {
        val store = store()
        val id = ScreenID()
        val screen = create(store, id)

        // Navigation3 也以 contentKey = id.toString() 从同一 Provider 获取引用。
        store.provider.acquireToken(id.toString()).use {
            store.remove(id)
            assertFalse(id in store)
            assertSame(screen, store[id])
            assertEquals(0, screen.clearCount)
        }

        assertNull(store[id])
        assertEquals(1, screen.clearCount)
    }

    @Test
    fun factoryFailureRollsBackAndAllowsRetry() {
        val store = store()
        val id = ScreenID()
        val failure = TestFailure("factory")

        val caught = assertFailsWith<TestFailure> {
            store.create(id) { throw failure }
        }

        assertSame(failure, caught)
        assertFalse(id in store)
        assertNull(store[id])
        assertFailsWith<IllegalArgumentException> { store.retain(id) }
        assertFalse(store.isClosed)

        val retry = create(store, id)
        assertTrue(id in store)
        assertSame(retry, store[id])
        assertEquals(0, retry.clearCount)
    }

    @Test
    fun factoryFailureDiscardsEntryStoreBeforeRetry() {
        val store = store()
        val id = ScreenID()
        val previousStore = store.provider.getOrCreate(id.toString())

        assertFailsWith<TestFailure> {
            store.create(id) { throw TestFailure("factory") }
        }

        assertFalse(id in store)
        assertNull(store[id])

        val retry = create(store, id)
        assertNotSame(previousStore, store.provider.getOrCreate(id.toString()))
        assertSame(retry, store[id])
    }

    @Test
    fun clearedFactoryResultIsRejectedAndAllowsRetryWithoutRepeatingUninitialize() {
        val previousStore = store()
        val cleared = create(previousStore, ScreenID())
        previousStore.close()

        val store = store()
        val id = ScreenID()

        val caught = assertFailsWith<IllegalArgumentException> { store.create(id) { cleared } }

        assertEquals("The factory returned a cleared screen.", caught.message)
        assertFalse(id in store)
        assertNull(store[id])
        assertEquals(1, cleared.clearCount)
        assertFalse(store.isClosed)

        val retry = create(store, id)
        assertSame(retry, store[id])
        assertEquals(0, retry.clearCount)
    }

    @Test
    fun closeDuringFactoryKeepsCreationFailureWhenCleanupAlsoFails() {
        val store = store()
        val id = ScreenID()
        val cleanupFailure = TestFailure("cleanup")
        var created: ProbeScreen? = null

        val caught = assertFailsWith<IllegalArgumentException> {
            store.create(id) {
                ProbeScreen(afterUninitialize = { throw cleanupFailure }).also {
                    created = it
                    store.close()
                }
            }
        }

        assertEquals("The screen store was closed during creation.", caught.message)
        assertEquals(1, caught.suppressedExceptions.size)
        assertSame(cleanupFailure, caught.suppressedExceptions.single())
        assertEquals(1, assertNotNull(created).clearCount)
        assertTrue(store.isClosed)
        assertFalse(id in store)
        assertNull(store[id])
    }

    @Test
    fun closeDuringFactoryKeepsCreationFailureWhenCleanupThrowsCancellation() {
        val store = store()
        val id = ScreenID()
        val cleanupFailure = CancellationException("cleanup")
        var created: ProbeScreen? = null

        val caught = assertFailsWith<IllegalArgumentException> {
            store.create(id) {
                ProbeScreen(afterUninitialize = { throw cleanupFailure }).also {
                    created = it
                    store.close()
                }
            }
        }

        assertEquals("The screen store was closed during creation.", caught.message)
        assertEquals(1, caught.suppressedExceptions.size)
        assertSame(cleanupFailure, caught.suppressedExceptions.single())
        assertEquals(1, assertNotNull(created).clearCount)
        assertTrue(store.isClosed)
        assertFalse(id in store)
        assertNull(store[id])
    }

    @Test
    fun reentrantCreateOfSameIdIsRejected() {
        val store = store()
        val id = ScreenID()
        var innerFactoryCalls = 0

        val screen = assertIs<ProbeScreen>(store.create(id) {
            assertNull(store[id])
            assertFailsWith<IllegalArgumentException> {
                store.create(id) {
                    ++innerFactoryCalls
                    ProbeScreen()
                }
            }
            ProbeScreen()
        })

        assertEquals(0, innerFactoryCalls)
        assertSame(screen, store[id])
        assertTrue(id in store)
        assertEquals(0, screen.clearCount)
    }

    @Test
    fun closeDuringFactoryRollsBackNewScreen() {
        val store = store()
        val id = ScreenID()
        var created: ProbeScreen? = null

        assertFailsWith<IllegalArgumentException> {
            store.create(id) {
                ProbeScreen().also {
                    created = it
                    store.close()
                }
            }
        }

        assertTrue(store.isClosed)
        assertFalse(id in store)
        assertNull(store[id])
        assertEquals(1, assertNotNull(created).clearCount)
    }

    @Test
    fun closeContinuesOtherEntriesWhenCleanupThrows() {
        val store = store()
        val firstId = ScreenID()
        val healthyId = ScreenID()
        val secondId = ScreenID()
        val firstFailure = TestFailure("first cleanup")
        val secondFailure = TestFailure("second cleanup")
        val first = create(store, firstId) { throw firstFailure }
        val healthy = create(store, healthyId)
        val second = create(store, secondId) { throw secondFailure }

        val caught = assertFailsWith<TestFailure> { store.close() }

        assertSame(firstFailure, caught)
        assertTrue(caught.suppressedExceptions.isEmpty())
        assertTrue(store.isClosed)
        for (id in [firstId, healthyId, secondId]) {
            assertFalse(id in store)
            assertNull(store[id])
        }
        assertEquals(1, first.clearCount)
        assertEquals(1, healthy.clearCount)
        assertEquals(1, second.clearCount)

        store.close()
        assertEquals(1, healthy.clearCount)
    }

    @Test
    fun uninitializeRunsAfterScopeCancellationAndStoreRecordRemoval() {
        val store = store()
        val id = ScreenID()
        lateinit var screen: ProbeScreen
        var scopeWasCancelled = false
        var recordWasRemoved = false
        screen = create(store, id) {
            scopeWasCancelled = assertNotNull(screen.viewModelScope.coroutineContext[Job]).isCancelled
            recordWasRemoved = store[id] == null
        }
        val job = assertNotNull(screen.viewModelScope.coroutineContext[Job])
        val reference = retain(store, id)

        store.remove(id)
        assertEquals(0, screen.clearCount)
        assertTrue(job.isActive)

        reference.close()

        assertEquals(1, screen.clearCount)
        assertTrue(scopeWasCancelled)
        assertTrue(recordWasRemoved)
        assertTrue(job.isCancelled)
        reference.close()
        store.close()
        assertEquals(1, screen.clearCount)
    }

    @Test
    fun defaultViewModelScopeIsCancelledOnlyAfterLastReference() {
        val store = store()
        val id = ScreenID()
        val screen = create(store, id)
        val scope = screen.viewModelScope
        val job = assertNotNull(scope.coroutineContext[Job])
        val reference = retain(store, id)

        assertSame(scope, screen.viewModelScope)
        assertTrue(job.isActive)
        store.remove(id)
        assertSame(scope, screen.viewModelScope)
        assertTrue(job.isActive)

        reference.close()

        assertTrue(job.isCancelled)
        assertEquals(1, screen.clearCount)
        assertNull(store[id])
    }

    @Test
    fun cancellationExceptionDuringCloseDoesNotSkipOtherEntries() {
        val store = store()
        val firstId = ScreenID()
        val secondId = ScreenID()
        val failure = CancellationException("cleanup")
        val first = create(store, firstId) { throw failure }
        val second = create(store, secondId)

        val caught = assertFailsWith<CancellationException> { store.close() }

        assertSame(failure, caught)
        assertTrue(store.isClosed)
        assertEquals(1, first.clearCount)
        assertEquals(1, second.clearCount)
        assertNull(store[firstId])
        assertNull(store[secondId])
        store.close()
    }
}
