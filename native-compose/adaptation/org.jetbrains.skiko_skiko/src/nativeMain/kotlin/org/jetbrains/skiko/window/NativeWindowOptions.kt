package org.jetbrains.skiko.window

/** Logical window size; the render callback receives drawable pixels, including display scaling. */
class NativeWindowOptions(
    val title: String = "Skiko Native",
    val width: Int = 800,
    val height: Int = 480,
    val backend: NativeWindowBackend = NativeWindowBackend.GPU,
    val continuous: Boolean = false,
    val smoke: Boolean = false,
) {
    init {
        require(title.length <= 1024 && '\u0000' !in title)
        require(width in 1 .. 16384 && height in 1 .. 16384 && width.toLong() * height <= 67108864)
    }
}