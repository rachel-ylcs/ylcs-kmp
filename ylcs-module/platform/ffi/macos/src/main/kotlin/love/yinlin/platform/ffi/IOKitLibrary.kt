package love.yinlin.platform.ffi

@Suppress("PropertyName")
open class IOKitLibrary : NativeLibrary("/System/Library/Frameworks/IOKit.framework/IOKit") {
    val IOPSCopyPowerSourcesInfo by func(retType = NativeType.Pointer)
    val IOPSCopyPowerSourcesList by func(NativeType.Pointer, retType = NativeType.Pointer)
    val IOPSGetPowerSourceDescription by func(NativeType.Pointer, NativeType.Pointer, retType = NativeType.Pointer)
}
