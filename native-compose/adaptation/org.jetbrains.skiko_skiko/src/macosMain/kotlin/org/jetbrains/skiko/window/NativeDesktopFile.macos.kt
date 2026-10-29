package org.jetbrains.skiko.window

import platform.AppKit.NSWorkspace
import platform.Foundation.NSURL

internal actual fun openNativeFile(path: String): Boolean = NSWorkspace.sharedWorkspace.openURL(NSURL.fileURLWithPath(path))
