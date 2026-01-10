package love.yinlin.platform.ffi

open class LibCLibrary : NativeLibrary() {
    val open by func(
        NativeType.String,
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
}