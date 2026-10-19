package love.yinlin.platform

import love.yinlin.platform.ffi.LibCLibrary
import love.yinlin.platform.ffi.Posix

internal object LinuxSingleInstance : SingleInstanceImpl, LibCLibrary() {
    var appLockFd = -1

    override fun lock(key: String): Boolean = useMemory { arena ->
        val path = "/tmp/$key"
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

    override fun unlock() {
        if (appLockFd >= 0) {
            // 保留锁文件，防止已打开旧 inode 的实例与新 inode 同时获得锁。
            flock(appLockFd, Posix.LOCK_UN)
            close(appLockFd)
            appLockFd = -1
        }
    }
}