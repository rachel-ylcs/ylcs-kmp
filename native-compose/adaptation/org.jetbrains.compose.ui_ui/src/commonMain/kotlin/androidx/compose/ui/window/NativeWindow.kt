package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import org.jetbrains.skiko.window.NativeWindowOptions

/**
 * Owns one SDL desktop window, composition and event loop on the main thread.
 * The call blocks until the window closes, then disposes composition effects and host resources.
 * [onCloseRequest] returns true to accept an OS close request. Escape is delivered to Compose.
 */
@ExperimentalComposeUiApi
expect fun NativeWindow(options: NativeWindowOptions = NativeWindowOptions(title = "Compose Native"), onCloseRequest: () -> Boolean = { true }, content: @Composable () -> Unit)
