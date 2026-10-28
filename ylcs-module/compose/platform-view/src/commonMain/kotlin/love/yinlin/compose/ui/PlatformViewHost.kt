package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

internal fun platformViewCleanup(vararg actions: () -> Unit) {
    var failure: Throwable? = null
    for (action in actions) {
        try {
            action()
        }
        catch (error: Throwable) {
            val previous = failure
            if (previous == null) failure = error
            else if (previous !== error) previous.addSuppressed(error)
        }
    }
    failure?.let { throw it }
}

@Composable
fun <S : PlatformViewStateOwner> rememberPlatformViewState(factory: @DisallowComposableCalls () -> S): S = remember {
    OwnedPlatformViewState(factory())
}.state

@Composable
fun <V : Any, P : Any> PlatformViewHost(state: BasicPlatformViewState<V, P>, parameters: P, modifier: Modifier = Modifier) {
    state.Render(parameters, modifier)
}
