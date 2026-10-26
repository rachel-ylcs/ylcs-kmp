package love.yinlin.nativecompose.ui

import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Verifies that the official Native runtime supplies state and executable compositions. */
class NativeRuntimeTest {
    @Test
    fun snapshotsIsolateWritesUntilTheyAreApplied() {
        val state = mutableStateOf(1)
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            snapshot.enter {
                state.value = 7
                assertEquals(7, state.value)
            }
            assertEquals(1, state.value)
            snapshot.apply().check()
            assertEquals(7, state.value)
        }
        finally { snapshot.dispose() }
    }

    @Test
    fun compositionAppliesEffectsAndDisposesThem() {
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(UnitApplier(), recomposer)
        var applied = 0
        var disposed = false
        try {
            composition.setContent {
                val remembered = remember { 42 }
                SideEffect { applied = remembered }
                DisposableEffect(Unit) { onDispose { disposed = true } }
            }
            assertEquals(42, applied)
        }
        finally {
            composition.dispose()
            recomposer.cancel()
        }
        assertTrue(disposed)
    }
}
