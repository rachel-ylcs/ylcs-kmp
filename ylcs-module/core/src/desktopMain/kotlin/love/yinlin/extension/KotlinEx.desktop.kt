package love.yinlin.extension

import java.lang.ref.Cleaner

private object NativeDestructor {
    val cleaner: Cleaner = Cleaner.create()
}

class RAII(constructor: () -> Long, private val destructor: (Long) -> Unit) : Runnable {
    internal val handle: Long = constructor()
    override fun run() = destructor(handle)
}

abstract class Destructible(raii: RAII) : AutoCloseable {
    protected var nativeHandle: Long = raii.handle
        private set
    protected open fun onClose() { }

    init {
        NativeDestructor.cleaner.register(this, raii)
    }

    final override fun close() {
        onClose()
        // 其实置空并没有什么意义了, 这里只是为了与native侧保持一致
        // 因为如果有人继续用nullptr, 可能会被native那边检测到从而相安无事
        nativeHandle = 0L
    }
}