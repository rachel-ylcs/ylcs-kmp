package love.yinlin.foundation.os.process

import love.yinlin.platform.Platform
import love.yinlin.platform.ffi.*
import love.yinlin.platform.platform
import java.lang.foreign.Linker
import kotlin.concurrent.thread

actual object SingleInstance {
    private val open by FFI.libc.func(Linker.Option.firstVariadicArg(2)) {
        declare(NativeType.String, NativeType.Int, NativeType.Int, retType = NativeType.Int)
    }

    private object Windows {
        private val CreateEventW by FFI.kernel32.errorBind {
            declare(Win32Type.POINTER, Win32Type.BOOL, Win32Type.BOOL, NativeType.WString, retType = Win32Type.HANDLE)
        }

        private var appEvent: Win32.HANDLE = Address.NULL

        @Synchronized
        fun lock(key: String): Boolean = useMemory { arena ->
            if (appEvent.isNotNull || key.isEmpty() || '\u0000' in key) return@useMemory false
            val name = arena.wstr("$key.${platform.name}")
            val event = CreateEventW(arena, Address.NULL, Win32.FALSE, Win32.FALSE, name) as Win32.HANDLE
            if (event.isNull) return@useMemory false
            if (CreateEventW.lastError == Win32.ERROR_ALREADY_EXISTS) {
                FFI.kernel32.CloseHandle(event)
                return@useMemory false
            }
            appEvent = event
            true
        }

        @Synchronized
        fun unlock() {
            if (appEvent.isNotNull) {
                FFI.kernel32.CloseHandle(appEvent)
                appEvent = Address.NULL
            }
        }
    }

    private object Linux {
        var appLockFd = -1

        fun lock(key: String): Boolean = useMemory { arena ->
            val path = "/tmp/$key.${platform.name}"
            appLockFd = open(arena.astr(path), Posix.O_CREAT or Posix.O_RDWR, 438) as Int
            if (appLockFd < 0) false
            else {
                val result = FFI.libc.flock(appLockFd, Posix.LOCK_EX or Posix.LOCK_NB) as Int
                if (result < 0) {
                    FFI.libc.close(appLockFd)
                    appLockFd = -1
                    false
                }
                else {
                    val pid = (FFI.libc.getpid() as Int).toString()
                    FFI.libc.ftruncate(appLockFd, 0)
                    FFI.libc.write(appLockFd, arena.astr(pid), pid.length.toLong())
                    true
                }
            }
        }

        fun unlock() {
            if (appLockFd >= 0) {
                // 保留锁文件，防止已打开旧 inode 的实例与新 inode 同时获得锁
                FFI.libc.flock(appLockFd, Posix.LOCK_UN)
                FFI.libc.close(appLockFd)
                appLockFd = -1
            }
        }
    }

    private object MacOS {
        private const val O_NOFOLLOW = 0x100
        private const val O_CREAT = 0x200
        private const val O_CLOEXEC = 0x1000000
        private var appLockFd = -1

        @Synchronized
        fun lock(key: String): Boolean = useMemory { arena ->
            if (appLockFd >= 0 || key.isEmpty() || '\u0000' in key) return@useMemory false
            val home = System.getProperty("user.home") ?: return@useMemory false
            val path = "$home/.rachel-${key.encodeToByteArray().toHexString()}.${platform.name}.lock"
            val descriptor = open(arena.astr(path), O_CREAT or Posix.O_RDWR or O_CLOEXEC or O_NOFOLLOW, 384) as Int
            if (descriptor < 0) return@useMemory false
            if ((FFI.libc.flock(descriptor, Posix.LOCK_EX or Posix.LOCK_NB) as Int) != 0) {
                FFI.libc.close(descriptor)
                return@useMemory false
            }
            appLockFd = descriptor
            true
        }

        @Synchronized
        fun unlock() {
            if (appLockFd >= 0) {
                FFI.libc.close(appLockFd)
                appLockFd = -1
            }
        }
    }

    actual fun run(key: String): Boolean {
        val result = when (platform) {
            Platform.Windows -> Windows.lock(key)
            Platform.Linux -> Linux.lock(key)
            Platform.MacOS -> MacOS.lock(key)
            else -> false
        }

        if (result) {
            val thread = thread(start = false, block = when (platform) {
                Platform.Windows -> Windows::unlock
                Platform.Linux -> Linux::unlock
                Platform.MacOS -> MacOS::unlock
                else -> error("unreachable")
            })
            Runtime.getRuntime().addShutdownHook(thread)
        }
        return result
    }
}
