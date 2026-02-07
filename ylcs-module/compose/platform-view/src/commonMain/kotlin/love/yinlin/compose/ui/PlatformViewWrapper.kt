package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
inline fun <T : Any, V : BasicPlatformView<T>> rememberPlatformView(
    crossinline factory: @DisallowComposableCalls () -> V
): V = remember(factory)

@Composable
inline fun <T : Any, V : BasicPlatformView<T>, S1> rememberPlatformView(
    arg1: S1,
    crossinline factory: @DisallowComposableCalls (State<S1>) -> V
): V {
    val state1 = rememberUpdatedState(arg1)
    return remember { factory(state1) }
}

@Composable
inline fun <T : Any, V : BasicPlatformView<T>, S1, S2> rememberPlatformView(
    arg1: S1,
    arg2: S2,
    crossinline factory: @DisallowComposableCalls (State<S1>, State<S2>) -> V
): V {
    val state1 = rememberUpdatedState(arg1)
    val state2 = rememberUpdatedState(arg2)
    return remember { factory(state1, state2) }
}

@Composable
inline fun <T : Any, V : BasicPlatformView<T>, S1, S2, S3> rememberPlatformView(
    arg1: S1,
    arg2: S2,
    arg3: S3,
    crossinline factory: @DisallowComposableCalls (State<S1>, State<S2>, State<S3>) -> V
): V {
    val state1 = rememberUpdatedState(arg1)
    val state2 = rememberUpdatedState(arg2)
    val state3 = rememberUpdatedState(arg3)
    return remember { factory(state1, state2, state3) }
}

@Composable
inline fun <T : Any, V : BasicPlatformView<T>, S1, S2, S3, S4> rememberPlatformView(
    arg1: S1,
    arg2: S2,
    arg3: S3,
    arg4: S4,
    crossinline factory: @DisallowComposableCalls (State<S1>, State<S2>, State<S3>, State<S4>) -> V
): V {
    val state1 = rememberUpdatedState(arg1)
    val state2 = rememberUpdatedState(arg2)
    val state3 = rememberUpdatedState(arg3)
    val state4 = rememberUpdatedState(arg4)
    return remember { factory(state1, state2, state3, state4) }
}

@Composable
inline fun <T : Any, V : BasicPlatformView<T>, S1, S2, S3, S4, S5> rememberPlatformView(
    arg1: S1,
    arg2: S2,
    arg3: S3,
    arg4: S4,
    arg5: S5,
    crossinline factory: @DisallowComposableCalls (State<S1>, State<S2>, State<S3>, State<S4>, State<S5>) -> V
): V {
    val state1 = rememberUpdatedState(arg1)
    val state2 = rememberUpdatedState(arg2)
    val state3 = rememberUpdatedState(arg3)
    val state4 = rememberUpdatedState(arg4)
    val state5 = rememberUpdatedState(arg5)
    return remember { factory(state1, state2, state3, state4, state5) }
}