package love.yinlin.foundation.os

expect object SingleInstance {
    fun run(key: String): Boolean
}