package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.imageIO: ImageIOLibrary get() = ImageIOLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class ImageIOLibrary private constructor() : NativeLibrary("/System/Library/Frameworks/ImageIO.framework/ImageIO") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = ImageIOLibrary()

        private val pointer = NativeType.Pointer
        private val boolean = NativeType.Boolean
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
    }

    val CGImageDestinationAddImage by func { declare(pointer, pointer, pointer) }
    val CGImageDestinationAddImageAndMetadata by func { declare(pointer, pointer, pointer, pointer) }
    val CGImageDestinationAddImageFromSource by func { declare(pointer, pointer, int64, pointer) }
    val CGImageDestinationCopyImageSource by func { declare(pointer, pointer, pointer, pointer, retType = boolean) }
    val CGImageDestinationCopyTypeIdentifiers by func { declare(retType = pointer) }
    val CGImageDestinationCreateWithData by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CGImageDestinationCreateWithDataConsumer by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CGImageDestinationCreateWithURL by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CGImageDestinationFinalize by func { declare(pointer, retType = boolean) }
    val CGImageDestinationGetTypeID by func { declare(retType = int64) }
    val CGImageDestinationSetProperties by func { declare(pointer, pointer) }
    val CGImageMetadataCopyStringValueWithPath by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CGImageMetadataCopyTagMatchingImageProperty by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CGImageMetadataCopyTagWithPath by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CGImageMetadataCopyTags by func { declare(pointer, retType = pointer) }
    val CGImageMetadataCreateFromXMPData by func { declare(pointer, retType = pointer) }
    val CGImageMetadataCreateMutable by func { declare(retType = pointer) }
    val CGImageMetadataCreateMutableCopy by func { declare(pointer, retType = pointer) }
    val CGImageMetadataCreateXMPData by func { declare(pointer, pointer, retType = pointer) }
    val CGImageMetadataGetTypeID by func { declare(retType = int64) }
    val CGImageMetadataRemoveTagWithPath by func { declare(pointer, pointer, pointer, retType = boolean) }
    val CGImageMetadataSetTagWithPath by func { declare(pointer, pointer, pointer, pointer, retType = boolean) }
    val CGImageMetadataSetValueWithPath by func { declare(pointer, pointer, pointer, pointer, retType = boolean) }
    val CGImageMetadataTagCopyName by func { declare(pointer, retType = pointer) }
    val CGImageMetadataTagCopyNamespace by func { declare(pointer, retType = pointer) }
    val CGImageMetadataTagCopyPrefix by func { declare(pointer, retType = pointer) }
    val CGImageMetadataTagCopyValue by func { declare(pointer, retType = pointer) }
    val CGImageMetadataTagCreate by func { declare(pointer, pointer, pointer, int32, pointer, retType = pointer) }
    val CGImageMetadataTagGetType by func { declare(pointer, retType = int32) }
    val CGImageMetadataTagGetTypeID by func { declare(retType = int64) }
    val CGImageSourceCopyMetadataAtIndex by func { declare(pointer, int64, pointer, retType = pointer) }
    val CGImageSourceCopyProperties by func { declare(pointer, pointer, retType = pointer) }
    val CGImageSourceCopyPropertiesAtIndex by func { declare(pointer, int64, pointer, retType = pointer) }
    val CGImageSourceCopyTypeIdentifiers by func { declare(retType = pointer) }
    val CGImageSourceCreateImageAtIndex by func { declare(pointer, int64, pointer, retType = pointer) }
    val CGImageSourceCreateIncremental by func { declare(pointer, retType = pointer) }
    val CGImageSourceCreateThumbnailAtIndex by func { declare(pointer, int64, pointer, retType = pointer) }
    val CGImageSourceCreateWithData by func { declare(pointer, pointer, retType = pointer) }
    val CGImageSourceCreateWithDataProvider by func { declare(pointer, pointer, retType = pointer) }
    val CGImageSourceCreateWithURL by func { declare(pointer, pointer, retType = pointer) }
    val CGImageSourceGetCount by func { declare(pointer, retType = int64) }
    val CGImageSourceGetPrimaryImageIndex by func { declare(pointer, retType = int64) }
    val CGImageSourceGetStatus by func { declare(pointer, retType = int32) }
    val CGImageSourceGetStatusAtIndex by func { declare(pointer, int64, retType = int32) }
    val CGImageSourceGetType by func { declare(pointer, retType = pointer) }
    val CGImageSourceGetTypeID by func { declare(retType = int64) }
    val CGImageSourceUpdateData by func { declare(pointer, pointer, boolean) }
    val CGImageSourceUpdateDataProvider by func { declare(pointer, pointer, boolean) }
}
