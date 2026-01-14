package love.yinlin.platform

import love.yinlin.platform.ffi.Address
import love.yinlin.platform.ffi.NativeLibrary
import love.yinlin.platform.ffi.NativeType

internal object MacOSNativeWindow : NativeWindowImpl, NativeLibrary("/usr/lib/libobjc.A.dylib") {
    val sel_registerName by func(NativeType.Pointer, retType = NativeType.Pointer)
    val objc_msgSend by func(NativeType.Pointer, NativeType.Pointer, NativeType.Boolean)
    val objc_autoreleasePoolPush by func(retType = NativeType.Pointer)
    val objc_autoreleasePoolPop by func(NativeType.Pointer)

    override fun updateClickThrough(handle: Long, enabled: Boolean) {
        val pool = objc_autoreleasePoolPush()
        try {
            val windowPtr = Address.ofAddress(handle)
            useMemory { arena ->
                val selName = arena.allocateFrom("setIgnoresMouseEvents:")
                val sel = sel_registerName(selName) as Address
                objc_msgSend(windowPtr, sel, enabled)
            }
        }
        finally {
            objc_autoreleasePoolPop(pool)
        }
    }
}