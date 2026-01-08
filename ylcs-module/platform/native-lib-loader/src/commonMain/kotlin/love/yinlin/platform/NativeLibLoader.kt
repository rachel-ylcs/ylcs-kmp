package love.yinlin.platform

expect object NativeLibLoader {
    fun env(name: String)
    fun resource(name: String)
}