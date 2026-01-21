package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIView

@Stable
abstract class PlatformView<V : UIView> : BasicPlatformView<V>() {
    protected abstract fun build(): V

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun HostView(modifier: Modifier) {
        UIKitView(
            modifier = modifier,
            factory = { hostFactory(::build) },
            update = hostUpdate,
            onReset = hostReset,
            onRelease = ::hostRelease,
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                // https://kotlinlang.org/docs/multiplatform/whats-new-compose-110.html#overlay-placement-for-interop-views
                placedAsOverlay = true
            )
        )
    }
}