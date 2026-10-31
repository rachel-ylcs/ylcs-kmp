package androidx.compose.ui.viewinterop

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.LocalNativeWindowContext
import androidx.compose.ui.window.NativeWindowContext

@ExperimentalComposeUiApi
@Composable
fun <T : NativeInteropView> NativeView(
    factory: (NativeWindowContext) -> T,
    modifier: Modifier = Modifier,
    onRelease: (T) -> Unit = { it.close() },
    update: (T) -> Unit = { }
) {
    val context = LocalNativeWindowContext.current

    InteropView(
        factory = { key ->
            NativeInteropViewHolder(
                factory = {
                    factory(context).also { context.interopContainer.register(it) }
                },
                container = context.interopContainer,
                compositeKeyHashCode = key
            )
        },
        modifier = modifier,
        onRelease = { view ->
            try { onRelease(view) }
            catch (failure: Throwable) {
                try {
                    view.close()
                }
                catch (cleanup: Throwable) {
                    if (cleanup !== failure) failure.addSuppressed(cleanup)
                }
                throw failure
            }
            finally {
                context.interopContainer.unregister(view)
            }
        },
        update = update
    )
}
