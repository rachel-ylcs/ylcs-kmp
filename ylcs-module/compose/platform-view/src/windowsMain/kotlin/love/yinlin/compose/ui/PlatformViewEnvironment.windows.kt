@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.lifecycle.LifecycleOwner
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.skiko.window.NativeWindowScope
import platform.windows.HWND

class PlatformViewEnvironment(val window: NativeWindowScope, val parentWindow: HWND, val lifecycleOwner: LifecycleOwner)
