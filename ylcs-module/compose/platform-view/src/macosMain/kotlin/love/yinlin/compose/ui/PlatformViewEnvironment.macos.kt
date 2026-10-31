package love.yinlin.compose.ui

import androidx.lifecycle.LifecycleOwner
import org.jetbrains.skiko.window.NativeWindowScope
import platform.AppKit.NSView

class PlatformViewEnvironment(val window: NativeWindowScope, val parentView: NSView, val lifecycleOwner: LifecycleOwner)
