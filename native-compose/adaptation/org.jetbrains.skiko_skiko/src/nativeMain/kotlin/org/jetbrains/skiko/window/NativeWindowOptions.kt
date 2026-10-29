package org.jetbrains.skiko.window

class NativeWindowOptions(
    val title: String = "Skiko Native",
    val width: Int = 800,
    val height: Int = 480,
    val backend: NativeWindowBackend = NativeWindowBackend.GPU,
    val transparent: Boolean = false,
    val undecorated: Boolean = false,
    val focusable: Boolean = true,
    val resizable: Boolean = true,
    val clickThrough: Boolean = false,
) {
    init {
        require(title.length <= 1024 && '\u0000' !in title)
        require(width in 1 .. 16384 && height in 1 .. 16384 && width.toLong() * height <= 67108864)
        require(!clickThrough || undecorated) { "Mouse passthrough requires an undecorated window" }
    }
}
