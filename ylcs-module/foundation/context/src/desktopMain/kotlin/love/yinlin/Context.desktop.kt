package love.yinlin

actual class Context actual constructor(delegate: PlatformContextDelegate) {
    // 在 initDelay 后可用
    var handle: Long = 0L
        private set

    fun bindWindow(window: Long) {
        this.handle = window
    }
}