package love.yinlin.foundation.os.process

actual object SingleInstance {
    actual fun run(key: String): Boolean = true
}