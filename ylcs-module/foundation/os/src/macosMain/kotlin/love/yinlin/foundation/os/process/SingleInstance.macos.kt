package love.yinlin.foundation.os.process

import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.concurrent.atomic
import love.yinlin.platform.platform
import platform.Foundation.NSHomeDirectory
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
actual object SingleInstance {
    private val appLockFd = atomic(-1)

    actual fun run(key: String): Boolean {
        if (appLockFd.value >= 0 || key.isEmpty() || '\u0000' in key) return false
        val path = "${NSHomeDirectory()}/.rachel-${key.encodeToByteArray().toHexString()}.${platform.name}.lock"
        val descriptor = open(path, O_CREAT or O_RDWR or O_CLOEXEC or O_NOFOLLOW, 384)
        if (descriptor < 0) return false
        if (flock(descriptor, LOCK_EX or LOCK_NB) != 0 || !appLockFd.compareAndSet(-1, descriptor)) {
            close(descriptor)
            return false
        }
        return true
    }
}
