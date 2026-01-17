package love.yinlin.extension

import java.lang.ref.Cleaner

class RAII(constructor: () -> Long, private val destructor: (Long) -> Unit) : Runnable {
    internal companion object {
        internal val cleaner: Cleaner = Cleaner.create()
    }

    internal val handle: Long = constructor()
    override fun run() = destructor(handle)
}

abstract class Destructible(raii: RAII) {
    protected val nativeHandle: Long = raii.handle
    init {
        RAII.cleaner.register(this, raii)
    }
}