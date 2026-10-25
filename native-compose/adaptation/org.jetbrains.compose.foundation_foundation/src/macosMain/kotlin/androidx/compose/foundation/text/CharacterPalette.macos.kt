package androidx.compose.foundation.text

import platform.AppKit.NSApplication

internal actual fun showCharacterPalette() = NSApplication.sharedApplication.orderFrontCharacterPalette(null)
