package love.yinlin.foundation.os.process

expect object SingleInstance {
    fun run(key: String): Boolean
}
