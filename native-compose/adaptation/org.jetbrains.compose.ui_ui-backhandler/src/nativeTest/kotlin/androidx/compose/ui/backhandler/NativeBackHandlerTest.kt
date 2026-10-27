package androidx.compose.ui.backhandler

import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.*

class NativeBackHandlerTest {
    @Test
    fun composedHandlerReceivesBackAndIsRemovedOnDisposal() {
        val dispatcher = NavigationEventDispatcher()
        val owner = object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = dispatcher
        }
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(UnitApplier(), recomposer)
        var backs = 0
        try {
            composition.setContent {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    @Suppress("DEPRECATION")
                    BackHandler { backs++ }
                }
            }
            assertTrue(input.hasEnabledBackHandlers)
            input.backCompleted()
            assertEquals(1, backs)
            composition.dispose()
            assertFalse(input.hasEnabledBackHandlers)
        }
        finally {
            composition.dispose()
            recomposer.cancel()
            dispatcher.dispose()
        }
    }

    @Test
    fun disabledHandlerDoesNotCaptureBack() {
        val dispatcher = NavigationEventDispatcher()
        val owner = object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = dispatcher
        }
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(UnitApplier(), recomposer)
        try {
            composition.setContent {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    @Suppress("DEPRECATION")
                    BackHandler(enabled = false) { error("Disabled handler ran") }
                }
            }
            assertFalse(input.hasEnabledBackHandlers)
        }
        finally {
            composition.dispose()
            recomposer.cancel()
            dispatcher.dispose()
        }
    }
}
