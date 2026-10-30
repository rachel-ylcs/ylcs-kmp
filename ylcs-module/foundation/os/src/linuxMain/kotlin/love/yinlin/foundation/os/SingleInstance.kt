package love.yinlin.foundation.os

import kotlinx.cinterop.*
import platform.linux.flock
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
actual object SingleInstance {
    private var appLockFd = -1

    actual fun run(key: String): Boolean {
        if (appLockFd >= 0 || key.isEmpty() || '\u0000' in key || '/' in key) return false
        val descriptor = open("/tmp/$key", O_CREAT or O_RDWR or O_CLOEXEC or O_NOFOLLOW, 438u)
        if (descriptor < 0) return false
        if (flock(descriptor, LOCK_EX or LOCK_NB) != 0 || ftruncate(descriptor, 0) != 0) {
            close(descriptor)
            return false
        }
        val pid = getpid().toString().encodeToByteArray()
        pid.usePinned { pinned ->
            var offset = 0
            while (offset < pid.size) {
                val count = write(descriptor, pinned.addressOf(offset), (pid.size - offset).convert<size_t>())
                if (count < 0 && errno == EINTR) continue
                if (count <= 0) {
                    close(descriptor)
                    return false
                }
                offset += count.toInt()
            }
        }
        appLockFd = descriptor
        return true
    }
}
