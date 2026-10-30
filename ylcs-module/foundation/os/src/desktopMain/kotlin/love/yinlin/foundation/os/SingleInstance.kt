package love.yinlin.foundation.os

import love.yinlin.platform.Platform
import love.yinlin.platform.ffi.*
import love.yinlin.platform.platform
import java.lang.invoke.MethodHandles
import kotlin.concurrent.thread

actual object SingleInstance {
    private object Windows : Kernel32Library() {
        var appEvent: Win32.HANDLE = Address.NULL

        fun lock(key: String): Boolean = useMemory { arena ->
            appEvent = OpenEventW(Win32.EVENT_ALL_ACCESS, Win32.FALSE, arena.wstr(key)) as Win32.HANDLE
            if (appEvent.isNull) {
                appEvent = CreateEventW(Address.NULL, Win32.FALSE, Win32.FALSE, arena.wstr(key)) as Win32.HANDLE
                true
            }
            else false
        }

        fun unlock() {
            if (appEvent.isNotNull) {
                CloseHandle(appEvent)
                appEvent = Address.NULL
            }
        }
    }

    private object Linux : LibCLibrary() {
        var appLockFd = -1

        fun lock(key: String): Boolean = useMemory { arena ->
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

        fun unlock() {
            if (appLockFd >= 0) {
                // 保留锁文件，防止已打开旧 inode 的实例与新 inode 同时获得锁。
                flock(appLockFd, Posix.LOCK_UN)
                close(appLockFd)
                appLockFd = -1
            }
        }
    }

    private object MacOS : NativeLibrary("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation") {
        val CFStringCreateWithCString by func(
            NativeType.Pointer,
            NativeType.Pointer,
            NativeType.Int,
            retType = NativeType.Pointer
        )
        val CFMessagePortCreateLocal by func(
            NativeType.Pointer,
            NativeType.Pointer,
            NativeType.Pointer,
            NativeType.Pointer,
            NativeType.Pointer,
            retType = NativeType.Pointer,
        )
        val CFMessagePortInvalidate by func(NativeType.Pointer)
        val CFRelease by func(NativeType.Pointer)

        @JvmStatic
        @Suppress("FunctionName", "unused")
        private fun MessageCallback(port: Address, msgid: Int, data: Address, info: Address): Address = Address.NULL

        var appMessagePort: Address = Address.NULL

        fun lock(key: String): Boolean = useMemory { arena ->
            val cStr = arena.astr(key)
            val kcfStringEncodingUTF8 = 0x08000100
            val name = CFStringCreateWithCString(Address.NULL, cStr, kcfStringEncodingUTF8) as Address

            val function = declare(
                NativeType.Pointer,
                NativeType.Int,
                NativeType.Pointer,
                NativeType.Pointer,
                retType = NativeType.Pointer,
            )
            val handle = MethodHandles.lookup().unreflect(
                MacOS::class.java.getDeclaredMethod(
                    "MessageCallback",
                    Address::class.java,
                    Int::class.java,
                    Address::class.java,
                    Address::class.java,
                )
            )
            val callback = upcall(handle, function)
            appMessagePort = CFMessagePortCreateLocal(Address.NULL, name, callback, Address.NULL, Address.NULL) as Address
            appMessagePort.isNotNull
        }

        fun unlock() {
            if (appMessagePort.isNotNull) {
                CFMessagePortInvalidate(appMessagePort)
                CFRelease(appMessagePort)
                appMessagePort = Address.NULL
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