package love.yinlin.platform

import love.yinlin.platform.ffi.Linux
import love.yinlin.platform.ffi.NativeLibrary
import love.yinlin.platform.ffi.NativeType
import love.yinlin.platform.ffi.aString

internal object LinuxSingleInstance : SingleInstanceImpl, NativeLibrary() {
    val open by func(
        NativeType.string,
        NativeType.Int,
        NativeType.Int,
        retType = NativeType.Int,
    )

    val flock by func(
        NativeType.Int,
        NativeType.Int,
        retType = NativeType.Int,
    )

    val getpid by func(
        retType = NativeType.Int,
    )

    val write by func(
        NativeType.Int,
        NativeType.Pointer,
        NativeType.Long,
        retType = NativeType.Long,
    )

    val close by func(
        NativeType.Int,
        retType = NativeType.Int,
    )

    val ftruncate by func(
        NativeType.Int,
        NativeType.Long,
        retType = NativeType.Int,
    )

    val unlink by func(
        NativeType.Pointer,
        retType = NativeType.Int,
    )

    var appLockFd = -1
    var lockFilePath: String? = null

    override fun lock(key: String): Boolean = useMemory { arena ->
        val path = "/tmp/$key"
        lockFilePath = path
        appLockFd = open(arena.aString(path), Linux.O_CREAT or Linux.O_RDWR, 438) as Int
        if (appLockFd < 0) false
        else {
            val result = flock(appLockFd, Linux.LOCK_EX or Linux.LOCK_NB) as Int
            if (result < 0) {
                close(appLockFd)
                appLockFd = -1
                false
            }
            else {
                val pid = (getpid() as Int).toString()
                ftruncate(appLockFd, 0)
                write(appLockFd, arena.aString(pid), pid.length.toLong())
                true
            }
        }
    }

    override fun unlock() {
        if (appLockFd >= 0) {
            flock(appLockFd, Linux.LOCK_UN)
            close(appLockFd)
            appLockFd = -1
            lockFilePath?.let { path ->
                useMemory { arena ->
                    unlink(arena.aString(path))
                }
                lockFilePath = null
            }
        }
    }
}