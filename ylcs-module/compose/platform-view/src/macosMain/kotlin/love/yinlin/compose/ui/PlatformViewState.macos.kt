@file:OptIn(ExperimentalComposeUiApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import platform.AppKit.NSView

@Stable
abstract class PlatformViewState<V : NSView, P : Any> : BasicPlatformViewState<V, P>() {
    protected abstract fun factory(environment: PlatformViewEnvironment): V

    @Composable
    final override fun Render(parameters: P, modifier: Modifier) {
        NativePlatformViewHost(this, parameters, modifier) { context, owner, onNativeFocus ->
            MacosPlatformViewContainer(context, onNativeFocus) { parent ->
                factory(PlatformViewEnvironment(context.window, parent, owner))
            }
        }
    }
}
