package androidx.compose.ui.window

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi

@ExperimentalComposeUiApi
val LocalNativeWindowContext = staticCompositionLocalOf<NativeWindowContext> {
    error("No Native window hosts this composition")
}
