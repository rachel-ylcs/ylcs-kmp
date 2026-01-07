package love.yinlin.platform

import java.lang.invoke.MethodHandles

internal object MacOSSingleInstance : SingleInstance, NativeLibrary("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation") {
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

    val CFMessagePortInvalidate by func(
        NativeType.Pointer,
    )

    val CFRelease by func(
        NativeType.Pointer,
    )

    @JvmStatic
    @Suppress("FunctionName", "unused")
    private fun MessageCallback(port: Native.Pointer, msgid: Int, data: Native.Pointer, info: Native.Pointer): Native.Pointer {
        return Native.NULL
    }

    var appMessagePort: Native.Pointer = Native.NULL

    override fun lock(key: String): Boolean = useMemory { arena ->
        val cStr = arena.aString(key)
        val name = CFStringCreateWithCString(Native.NULL, cStr, MacOS.KCFSTRINGENCODINGUTF8) as Native.Pointer

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
                Native.Pointer::class.java,
                Int::class.java,
                Native.Pointer::class.java,
                Native.Pointer::class.java,
            )
        )
        val callback = upcall(handle, function)
        appMessagePort = CFMessagePortCreateLocal(Native.NULL, name, callback, Native.NULL, Native.NULL) as Native.Pointer
        appMessagePort.isNotNull
    }

    override fun unlock() {
        if (appMessagePort.isNotNull) {
            CFMessagePortInvalidate(appMessagePort)
            CFRelease(appMessagePort)
            appMessagePort = Native.NULL
        }
    }
}