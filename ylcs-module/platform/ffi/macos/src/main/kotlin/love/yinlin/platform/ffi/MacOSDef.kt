package love.yinlin.platform.ffi

object MacOSDef {
    val CFRange: Layout = NamedStructLayout("CFRange") {
        "location" with NativeType.Long
        "length" with NativeType.Long
    }

    val CFGregorianDate: Layout = NamedStructLayout("CFGregorianDate") {
        "year" with NativeType.Int
        "month" with NativeType.Byte
        "day" with NativeType.Byte
        "hour" with NativeType.Byte
        "minute" with NativeType.Byte
        "second" with NativeType.Double
    }

    val CFGregorianUnits: Layout = NamedStructLayout("CFGregorianUnits") {
        "years" with NativeType.Int
        "months" with NativeType.Int
        "days" with NativeType.Int
        "hours" with NativeType.Int
        "minutes" with NativeType.Int
        padding(4L)
        "seconds" with NativeType.Double
    }

    val CFStreamError: Layout = NamedStructLayout("CFStreamError") {
        "domain" with NativeType.Long
        "error" with NativeType.Int
        padding(4L)
    }

    val CFUUIDBytes: Layout = NamedStructLayout("CFUUIDBytes") {
        for (index in 0 ..< 16) "byte$index" with NativeType.Byte
    }

    val NSRange: Layout = CFRange.withName("NSRange")

    val CGPoint: Layout = NamedStructLayout("CGPoint") {
        "x" with NativeType.Double
        "y" with NativeType.Double
    }

    val CGSize: Layout = NamedStructLayout("CGSize") {
        "width" with NativeType.Double
        "height" with NativeType.Double
    }

    val CGRect: Layout = NamedStructLayout("CGRect") {
        "origin" with CGPoint
        "size" with CGSize
    }

    val NSEdgeInsets: Layout = NamedStructLayout("NSEdgeInsets") {
        "top" with NativeType.Double
        "left" with NativeType.Double
        "bottom" with NativeType.Double
        "right" with NativeType.Double
    }

    val NSHashEnumerator: Layout = NamedStructLayout("NSHashEnumerator") {
        "_pi" with NativeType.Long
        "_si" with NativeType.Long
        "_bs" with NativeType.Pointer
    }

    val NSMapEnumerator: Layout = NSHashEnumerator.withName("NSMapEnumerator")

    val NSHashTableCallBacks: Layout = NamedStructLayout("NSHashTableCallBacks") {
        "hash" with NativeType.Pointer
        "isEqual" with NativeType.Pointer
        "retain" with NativeType.Pointer
        "release" with NativeType.Pointer
        "describe" with NativeType.Pointer
    }

    val NSMapTableKeyCallBacks: Layout = NamedStructLayout("NSMapTableKeyCallBacks") {
        "hash" with NativeType.Pointer
        "isEqual" with NativeType.Pointer
        "retain" with NativeType.Pointer
        "release" with NativeType.Pointer
        "describe" with NativeType.Pointer
        "notAKeyMarker" with NativeType.Pointer
    }

    val NSMapTableValueCallBacks: Layout = NamedStructLayout("NSMapTableValueCallBacks") {
        "retain" with NativeType.Pointer
        "release" with NativeType.Pointer
        "describe" with NativeType.Pointer
    }

    val ObjCMethodDescription: Layout = NamedStructLayout("objc_method_description") {
        "name" with NativeType.Pointer
        "types" with NativeType.Pointer
    }

    val NXHashTablePrototype: Layout = NamedStructLayout("NXHashTablePrototype") {
        "hash" with NativeType.Pointer
        "isEqual" with NativeType.Pointer
        "free" with NativeType.Pointer
        "style" with NativeType.Int
        padding(4L)
    }

    val NXHashState: Layout = NamedStructLayout("NXHashState") {
        "i" with NativeType.Int
        "j" with NativeType.Int
    }

    val IOGPoint: Layout = NamedStructLayout("IOGPoint") {
        "x" with NativeType.Short
        "y" with NativeType.Short
    }
}
