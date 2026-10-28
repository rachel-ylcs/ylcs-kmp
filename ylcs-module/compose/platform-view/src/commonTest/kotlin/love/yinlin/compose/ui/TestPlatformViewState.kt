package love.yinlin.compose.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TestPlatformViewState {
    private class View {
        var disposed = false
    }

    private data class Parameters(val url: String = "first", val enabled: Boolean = true, val onEvent: () -> Unit = {})

    private class Node(val view: View? = null, val release: () -> Unit = {}) {
        val children: MutableList<Node> = []
    }

    private class Applier : AbstractApplier<Node>(Node()) {
        val retired: MutableList<Node> = []
        override fun insertTopDown(index: Int, instance: Node) {}
        override fun insertBottomUp(index: Int, instance: Node) { current.children.add(index, instance) }
        override fun move(from: Int, to: Int, count: Int) = error("This test never moves native nodes")
        override fun remove(index: Int, count: Int) { repeat(count) { retired.add(current.children.removeAt(index)) } }
        override fun onClear() {
            retired.addAll(root.children)
            root.children.clear()
        }
        fun releaseRetired() {
            val pending = retired.toList()
            retired.clear()
            pending.forEach { it.release() }
        }
    }

    private class State : BasicPlatformViewState<View, Parameters>() {
        var attaches = 0
        var detaches = 0
        var releases = 0
        var loads = 0
        var updates = 0
        var tasksCancelled = 0
        var attachFailure: Throwable? = null
        var detachFailure: Throwable? = null
        var releaseFailure: Throwable? = null

        override fun attach(view: View) {
            attaches++
            attachFailure?.let { throw it }
        }

        override fun synchronize(view: View, previous: Parameters?, next: Parameters) {
            updates++
            if (previous?.url != next.url) loads++
        }

        override fun detach(view: View) {
            view.disposed = true
            detaches++
            detachFailure?.let { throw it }
        }

        override fun releaseResources() {
            releases++
            releaseFailure?.let { throw it }
        }

        fun event(view: View) = onView(view) { parameters.onEvent() }

        fun startTask(view: View) = launch(view) {
            try { awaitCancellation() }
            finally { tasksCancelled++ }
        }

        @Composable
        override fun Render(parameters: Parameters, modifier: Modifier) {}
    }

    @BeforeTest
    fun prepareMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun mount(state: State, view: View, parameters: Parameters = Parameters()): PlatformViewSlot<View, Parameters> {
        val slot = PlatformViewSlot(state)
        slot.create(parameters) { view }
        slot.update(parameters)
        slot.onRemembered()
        return slot
    }

    @Composable
    private fun NativeNode(state: State, identity: Int) {
        key(identity) {
            val slot = remember { PlatformViewSlot(state) }
            val parameters = Parameters()
            ComposeNode<Node, Applier>(
                factory = { Node(slot.create(parameters) { View() }, slot::finish) },
                update = { set(parameters) { slot.update(it) } },
            )
        }
    }

    @Test
    fun actualCompositionReplacementForgetsOldBindingBeforeActivatingNewOne() {
        val state = State()
        val applier = Applier()
        val recomposer = Recomposer(Dispatchers.Main)
        val composition = Composition(applier, recomposer)
        try {
            composition.setContent { NativeNode(state, 0) }
            val original = requireNotNull(applier.root.children.single().view)
            composition.setContent { NativeNode(state, 1) }
            assertEquals(2, state.attaches)
            assertEquals(1, applier.retired.size)
            assertTrue(state.isActive)
            assertFalse(original.disposed)
            applier.releaseRetired()
            assertTrue(original.disposed)
            assertTrue(state.isActive)
            assertEquals(1, state.detaches)
        }
        finally {
            composition.dispose()
            applier.releaseRetired()
            recomposer.cancel()
            state.release()
        }
        assertEquals(2, state.detaches)
        assertEquals(1, state.releases)
    }

    @Test
    fun updatesDoNotRecreateViewsOrRepeatUnchangedExpensiveWork() {
        val state = State()
        val view = View()
        val first = Parameters()
        val slot = mount(state, view, first)
        slot.update(first)
        slot.update(first.copy(enabled = false))
        assertEquals(1, state.attaches)
        assertEquals(2, state.updates)
        assertEquals(1, state.loads)
        slot.update(first.copy(url = "second"))
        assertEquals(2, state.loads)
        slot.finish()
    }

    @Test
    fun callbacksUseLatestCommittedParameters() {
        val state = State()
        val view = View()
        var firstEvents = 0
        var latestEvents = 0
        val slot = mount(state, view, Parameters(onEvent = { firstEvents++ }))
        slot.update(Parameters(onEvent = { latestEvents++ }))
        state.event(view)
        assertEquals(0, firstEvents)
        assertEquals(1, latestEvents)
        assertEquals(1, state.loads)
        slot.finish()
    }

    @Test
    fun latePlatformReleaseDoesNotDeactivateReplacementOrDeliverStaleEvents() {
        val state = State()
        val firstView = View()
        val nextView = View()
        var events = 0
        val parameters = Parameters(onEvent = { events++ })
        val first = mount(state, firstView, parameters)
        first.onForgotten()
        assertFalse(state.isActive)
        assertFalse(firstView.disposed)
        val next = mount(state, nextView, parameters)
        state.event(firstView)
        state.event(nextView)
        first.finish()
        assertTrue(state.isActive)
        assertFalse(nextView.disposed)
        assertEquals(1, events)
        next.finish()
        assertFalse(state.isActive)
    }

    @Test
    fun externallyOwnedStateSurvivesViewRemoval() {
        val state = State()
        val first = mount(state, View())
        first.onForgotten()
        first.finish()
        assertEquals(0, state.releases)
        val next = mount(state, View())
        assertTrue(state.isActive)
        assertEquals(2, state.attaches)
        assertEquals(2, state.loads)
        next.finish()
        state.release()
        assertEquals(1, state.releases)
    }

    @Test
    fun ownerReleaseWaitsForEveryRetiringViewAndRejectsCallbacksAndNewHosts() {
        val state = State()
        var events = 0
        val firstView = View()
        val first = mount(state, firstView)
        first.onForgotten()
        val currentView = View()
        val current = mount(state, currentView, Parameters(onEvent = { events++ }))
        state.startTask(currentView)
        state.release()
        assertEquals(1, state.tasksCancelled)
        assertFalse(state.isActive)
        assertEquals(0, state.releases)
        assertFalse(currentView.disposed)
        state.event(currentView)
        assertEquals(0, events)
        assertFailsWith<IllegalStateException> { mount(state, View()) }
        current.onForgotten()
        current.finish()
        assertEquals(0, state.releases)
        first.finish()
        assertEquals(1, state.releases)
        state.release()
        assertEquals(1, state.releases)
    }

    @Test
    fun forgettingHostCancelsItsWorkBeforeNativeViewIsDestroyed() {
        val state = State()
        val view = View()
        val slot = mount(state, view)
        state.startTask(view)
        slot.onForgotten()
        assertEquals(1, state.tasksCancelled)
        assertFalse(view.disposed)
        slot.finish()
        assertTrue(view.disposed)
    }

    @Test
    fun twoActiveHostsAreRejectedWithoutBreakingFirstHost() {
        val state = State()
        val firstView = View()
        val first = mount(state, firstView)
        val duplicate = View()
        assertFailsWith<IllegalStateException> { mount(state, duplicate) }
        assertTrue(duplicate.disposed)
        assertFalse(firstView.disposed)
        assertTrue(state.isActive)
        assertEquals(1, state.attaches)
        first.finish()
    }

    @Test
    fun abandonedNativeViewIsDestroyedEvenIfItNeverAttached() {
        val state = State()
        val slot = PlatformViewSlot(state)
        val view = slot.create(Parameters()) { View() }
        slot.onAbandoned()
        slot.onAbandoned()
        assertTrue(view.disposed)
        assertEquals(0, state.attaches)
        assertEquals(1, state.detaches)
        assertEquals(0, state.releases)
    }

    @Test
    fun lateNativeFactoryStillAttachesAndReceivesInitialParameters() {
        val state = State()
        val slot = PlatformViewSlot(state)
        slot.onRemembered()
        slot.create(Parameters()) { View() }
        assertTrue(state.isActive)
        assertEquals(0, state.updates)
        slot.update(Parameters())
        assertEquals(1, state.updates)
        slot.finish()
    }

    @Test
    fun attachAndFactoryFailuresDoNotRetainBindings() {
        val state = State()
        val failure = IllegalStateException("attach failed")
        state.attachFailure = failure
        val view = View()
        assertSame(failure, assertFails { mount(state, view) })
        assertTrue(view.disposed)
        assertFalse(state.isActive)
        val failedFactory = PlatformViewSlot(state)
        assertSame(failure, assertFails { failedFactory.create(Parameters()) { throw failure } })
        failedFactory.onAbandoned()
        state.release()
        assertEquals(1, state.releases)
        assertEquals(1, state.detaches)
    }

    @Test
    fun cleanupCompletesEvenWhenDetachThrowsCancellation() {
        val state = State()
        val slot = mount(state, View())
        val detachFailure = CancellationException("detach failed")
        val releaseFailure = IllegalStateException("release failed")
        state.detachFailure = detachFailure
        state.releaseFailure = releaseFailure
        state.release()
        assertSame(detachFailure, assertFails { slot.finish() })
        assertEquals(releaseFailure, detachFailure.suppressedExceptions.single())
        assertFalse(state.isActive)
        slot.finish()
        state.release()
        assertEquals(1, state.detaches)
        assertEquals(1, state.releases)
    }

    @Test
    fun rememberedOwnerReleasesForgottenAndAbandonedStates() {
        val forgotten = State()
        val owned = OwnedPlatformViewState(forgotten)
        owned.onRemembered()
        owned.onForgotten()
        owned.onForgotten()
        assertEquals(1, forgotten.releases)
        val abandoned = State()
        OwnedPlatformViewState(abandoned).onAbandoned()
        assertEquals(1, abandoned.releases)
    }
}
