package love.yinlin.platform

import love.yinlin.platform.ffi.Address
import love.yinlin.platform.ffi.NativeLibrary
import love.yinlin.platform.ffi.NativeType
import java.lang.invoke.MethodHandles

internal object MacOSSingleInstance : SingleInstanceImpl, NativeLibrary("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation") {
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

    override fun lock(key: String): Boolean = useMemory { arena ->
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
            MacOSSingleInstance::class.java.getDeclaredMethod(
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

    override fun unlock() {
        if (appMessagePort.isNotNull) {
            CFMessagePortInvalidate(appMessagePort)
            CFRelease(appMessagePort)
            appMessagePort = Address.NULL
        }
    }
}