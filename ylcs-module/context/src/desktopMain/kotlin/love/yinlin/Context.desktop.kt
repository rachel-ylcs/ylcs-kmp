package love.yinlin

actual class Context actual constructor(delegate: PlatformContextDelegate) {
    var handle: Long = 0L
        private set

    fun bindWindow(handle: Long) {
        this.handle = handle
    }
}