package love.yinlin.foundation

actual class OrientationController actual constructor(context: Context) {
    actual var orientation: Orientation
        get() = Orientation.Portrait
        set(value) { }
    actual fun rotate() { }
    actual fun restore() { }
}