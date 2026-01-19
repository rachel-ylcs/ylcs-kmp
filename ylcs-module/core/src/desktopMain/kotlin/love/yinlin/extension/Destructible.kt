package love.yinlin.extension

abstract class Destructible(raii: RAII) {
    val nativeHandle: Long = raii.handle
    init {
        RAII.cleaner.register(this, raii)
    }
}