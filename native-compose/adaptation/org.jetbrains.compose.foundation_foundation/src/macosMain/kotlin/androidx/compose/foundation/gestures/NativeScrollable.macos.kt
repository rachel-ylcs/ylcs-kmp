package androidx.compose.foundation.gestures

// SDL already supplies the accelerated Cocoa wheel delta, as on the JVM.
internal actual fun nativeScrollLineCount(): Int = 1
