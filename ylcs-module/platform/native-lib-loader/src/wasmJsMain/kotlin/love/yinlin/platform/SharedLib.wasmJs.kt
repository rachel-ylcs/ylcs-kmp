package love.yinlin.platform

actual sealed interface SharedLib {
    actual fun load()

    actual class Env actual constructor(private val name: String) : SharedLib {
        actual override fun load() { }
    }

    actual class Resource actual constructor(private val name: String) : SharedLib {
        actual override fun load() { }
    }
}