package love.yinlin.platform.ffi

@Suppress("PropertyName")
open class CoreFoundationLibrary : NativeLibrary("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation") {
    val CFArrayGetCount by func(NativeType.Pointer, retType = NativeType.Long)
    val CFArrayGetValueAtIndex by func(NativeType.Pointer, NativeType.Long, retType = NativeType.Pointer)
    val CFStringCreateWithCString by func(NativeType.Pointer, NativeType.Pointer, NativeType.Int, retType = NativeType.Pointer)
    val CFDictionaryGetValue by func(NativeType.Pointer, NativeType.Pointer, retType = NativeType.Pointer)
    val CFEqual by func(NativeType.Pointer, NativeType.Pointer, retType = NativeType.Byte)
    val CFBooleanGetValue by func(NativeType.Pointer, retType = NativeType.Byte)
    val CFNumberGetValue by func(NativeType.Pointer, NativeType.Long, NativeType.Pointer, retType = NativeType.Byte)
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
}
