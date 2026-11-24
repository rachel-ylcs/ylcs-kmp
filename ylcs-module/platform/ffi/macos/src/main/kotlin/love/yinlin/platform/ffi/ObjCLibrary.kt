package love.yinlin.platform.ffi

@Suppress("PropertyName")
open class ObjCLibrary : NativeLibrary("/usr/lib/libobjc.A.dylib") {
    val sel_registerName by func(NativeType.Pointer, retType = NativeType.Pointer)
    val objc_msgSend by func(NativeType.Pointer, NativeType.Pointer, NativeType.Boolean)
    val objc_autoreleasePoolPush by func(retType = NativeType.Pointer)
    val objc_autoreleasePoolPop by func(NativeType.Pointer)
}
