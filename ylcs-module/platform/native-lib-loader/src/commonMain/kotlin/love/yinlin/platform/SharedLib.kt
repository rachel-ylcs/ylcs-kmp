package love.yinlin.platform

expect sealed interface SharedLib {
    fun load()

    class Env(name: String) : SharedLib {
        override fun load()
    }

    class Resource(name: String) : SharedLib {
        override fun load()
    }
}