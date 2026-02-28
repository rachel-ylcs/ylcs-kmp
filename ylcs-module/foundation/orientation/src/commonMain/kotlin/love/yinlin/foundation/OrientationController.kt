package love.yinlin.foundation

expect class OrientationController(context: Context) {
    var orientation: Orientation
    fun rotate()
    fun restore()
}