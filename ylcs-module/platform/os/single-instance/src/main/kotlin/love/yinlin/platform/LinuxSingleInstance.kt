package love.yinlin.platform

import love.yinlin.platform.ffi.LibCLibrary
import love.yinlin.platform.ffi.Posix

internal object LinuxSingleInstance : SingleInstanceImpl, LibCLibrary() {
    var appLockFd = -1
    var lockFilePath: String? = null

    override fun lock(key: String): Boolean = useMemory { arena ->
        val path = "/tmp/$key"
        lockFilePath = path
        appLockFd = open(arena.astr(path), Posix.O_CREAT or Posix.O_RDWR, 438) as Int
        if (appLockFd < 0) false
        else {
            val result = flock(appLockFd, Posix.LOCK_EX or Posix.LOCK_NB) as Int
            if (result < 0) {
                close(appLockFd)
                appLockFd = -1
                false
            }
            else {
                val pid = (getpid() as Int).toString()
                ftruncate(appLockFd, 0)
                write(appLockFd, arena.astr(pid), pid.length.toLong())
                true
            }
        }
    }

    override fun unlock() = useMemory { arena ->
        if (appLockFd >= 0) {
            flock(appLockFd, Posix.LOCK_UN)
            close(appLockFd)
            appLockFd = -1
            lockFilePath?.let { path ->
                unlink(arena.astr(path))
                lockFilePath = null
            }
        }
    }
}