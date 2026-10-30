package love.yinlin.foundation.os

actual object SingleInstance {
    actual fun run(key: String): Boolean = true
}