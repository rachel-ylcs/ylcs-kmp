package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.coreGraphics: CoreGraphicsLibrary get() = CoreGraphicsLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class CoreGraphicsLibrary private constructor() : NativeLibrary("/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = CoreGraphicsLibrary()

        private val pointer = NativeType.Pointer
        private val boolean = NativeType.Boolean
        private val int16 = NativeType.Short
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
        private val float64 = NativeType.Double
        private val point = MacOSDef.CGPoint
        private val size = MacOSDef.CGSize
        private val rect = MacOSDef.CGRect
        private val transform = MacOSDef.CGAffineTransform
    }

    val CGAffineTransformConcat by func { declare(transform, transform, retType = transform) }
    val CGAffineTransformInvert by func { declare(transform, retType = transform) }
    val CGAffineTransformMakeRotation by func { declare(float64, retType = transform) }
    val CGAffineTransformMakeScale by func { declare(float64, float64, retType = transform) }
    val CGAffineTransformMakeTranslation by func { declare(float64, float64, retType = transform) }
    val CGAffineTransformRotate by func { declare(transform, float64, retType = transform) }
    val CGAffineTransformScale by func { declare(transform, float64, float64, retType = transform) }
    val CGAffineTransformTranslate by func { declare(transform, float64, float64, retType = transform) }
    val CGBitmapContextCreate by func { declare(pointer, int64, int64, int64, int64, pointer, int32, retType = pointer) }
    val CGBitmapContextCreateImage by func { declare(pointer, retType = pointer) }
    val CGBitmapContextGetBytesPerRow by func { declare(pointer, retType = int64) }
    val CGBitmapContextGetData by func { declare(pointer, retType = pointer) }
    val CGBitmapContextGetHeight by func { declare(pointer, retType = int64) }
    val CGBitmapContextGetWidth by func { declare(pointer, retType = int64) }
    val CGColorCreate by func { declare(pointer, pointer, retType = pointer) }
    val CGColorCreateCopy by func { declare(pointer, retType = pointer) }
    val CGColorCreateGenericGray by func { declare(float64, float64, retType = pointer) }
    val CGColorCreateGenericRGB by func { declare(float64, float64, float64, float64, retType = pointer) }
    val CGColorGetAlpha by func { declare(pointer, retType = float64) }
    val CGColorGetColorSpace by func { declare(pointer, retType = pointer) }
    val CGColorGetComponents by func { declare(pointer, retType = pointer) }
    val CGColorGetNumberOfComponents by func { declare(pointer, retType = int64) }
    val CGColorSpaceCopyName by func { declare(pointer, retType = pointer) }
    val CGColorSpaceCreateDeviceCMYK by func { declare(retType = pointer) }
    val CGColorSpaceCreateDeviceGray by func { declare(retType = pointer) }
    val CGColorSpaceCreateDeviceRGB by func { declare(retType = pointer) }
    val CGColorSpaceCreateWithName by func { declare(pointer, retType = pointer) }
    val CGColorSpaceGetModel by func { declare(pointer, retType = int32) }
    val CGColorSpaceGetNumberOfComponents by func { declare(pointer, retType = int64) }
    val CGContextAddPath by func { declare(pointer, pointer) }
    val CGContextBeginPath by func { declare(pointer) }
    val CGContextClearRect by func { declare(pointer, rect) }
    val CGContextClip by func { declare(pointer) }
    val CGContextClipToRect by func { declare(pointer, rect) }
    val CGContextConcatCTM by func { declare(pointer, transform) }
    val CGContextDrawImage by func { declare(pointer, rect, pointer) }
    val CGContextDrawPath by func { declare(pointer, int32) }
    val CGContextFillRect by func { declare(pointer, rect) }
    val CGContextGetCTM by func { declare(pointer, retType = transform) }
    val CGContextRelease by func { declare(pointer) }
    val CGContextRestoreGState by func { declare(pointer) }
    val CGContextRotateCTM by func { declare(pointer, float64) }
    val CGContextSaveGState by func { declare(pointer) }
    val CGContextScaleCTM by func { declare(pointer, float64, float64) }
    val CGContextSetLineWidth by func { declare(pointer, float64) }
    val CGContextSetRGBFillColor by func { declare(pointer, float64, float64, float64, float64) }
    val CGContextSetRGBStrokeColor by func { declare(pointer, float64, float64, float64, float64) }
    val CGContextStrokeRect by func { declare(pointer, rect) }
    val CGContextTranslateCTM by func { declare(pointer, float64, float64) }
    val CGDataProviderCopyData by func { declare(pointer, retType = pointer) }
    val CGDataProviderCreateWithCFData by func { declare(pointer, retType = pointer) }
    val CGDataProviderCreateWithURL by func { declare(pointer, retType = pointer) }
    val CGDisplayBounds by func { declare(int32, retType = rect) }
    val CGDisplayCopyAllDisplayModes by func { declare(int32, pointer, retType = pointer) }
    val CGDisplayCopyColorSpace by func { declare(int32, retType = pointer) }
    val CGDisplayCopyDisplayMode by func { declare(int32, retType = pointer) }
    val CGDisplayIsActive by func { declare(int32, retType = int32) }
    val CGDisplayIsBuiltin by func { declare(int32, retType = int32) }
    val CGDisplayIsMain by func { declare(int32, retType = int32) }
    val CGDisplayIsOnline by func { declare(int32, retType = int32) }
    val CGDisplayModeGetHeight by func { declare(pointer, retType = int64) }
    val CGDisplayModeGetIODisplayModeID by func { declare(pointer, retType = int32) }
    val CGDisplayModeGetIOFlags by func { declare(pointer, retType = int32) }
    val CGDisplayModeGetPixelHeight by func { declare(pointer, retType = int64) }
    val CGDisplayModeGetPixelWidth by func { declare(pointer, retType = int64) }
    val CGDisplayModeGetRefreshRate by func { declare(pointer, retType = float64) }
    val CGDisplayModeGetWidth by func { declare(pointer, retType = int64) }
    val CGDisplayPixelsHigh by func { declare(int32, retType = int64) }
    val CGDisplayPixelsWide by func { declare(int32, retType = int64) }
    val CGDisplayRegisterReconfigurationCallback by func { declare(pointer, pointer, retType = int32) }
    val CGDisplayRemoveReconfigurationCallback by func { declare(pointer, pointer, retType = int32) }
    val CGDisplayRotation by func { declare(int32, retType = float64) }
    val CGDisplayScreenSize by func { declare(int32, retType = size) }
    val CGEventCreate by func { declare(pointer, retType = pointer) }
    val CGEventCreateKeyboardEvent by func { declare(pointer, int16, boolean, retType = pointer) }
    val CGEventCreateMouseEvent by func { declare(pointer, int32, point, int32, retType = pointer) }
    val CGEventGetFlags by func { declare(pointer, retType = int64) }
    val CGEventGetIntegerValueField by func { declare(pointer, int32, retType = int64) }
    val CGEventGetLocation by func { declare(pointer, retType = point) }
    val CGEventGetTimestamp by func { declare(pointer, retType = int64) }
    val CGEventGetType by func { declare(pointer, retType = int32) }
    val CGEventSetFlags by func { declare(pointer, int64) }
    val CGEventSetIntegerValueField by func { declare(pointer, int32, int64) }
    val CGEventSetLocation by func { declare(pointer, point) }
    val CGEventSourceButtonState by func { declare(int32, int32, retType = boolean) }
    val CGEventSourceCreate by func { declare(int32, retType = pointer) }
    val CGEventSourceFlagsState by func { declare(int32, retType = int64) }
    val CGEventSourceKeyState by func { declare(int32, int16, retType = boolean) }
    val CGGetActiveDisplayList by func { declare(int32, pointer, pointer, retType = int32) }
    val CGGetDisplaysWithPoint by func { declare(point, int32, pointer, pointer, retType = int32) }
    val CGGetOnlineDisplayList by func { declare(int32, pointer, pointer, retType = int32) }
    val CGImageCreate by func { declare(int64, int64, int64, int64, int64, pointer, int32, pointer, pointer, boolean, int32, retType = pointer) }
    val CGImageCreateCopy by func { declare(pointer, retType = pointer) }
    val CGImageCreateWithImageInRect by func { declare(pointer, rect, retType = pointer) }
    val CGImageGetAlphaInfo by func { declare(pointer, retType = int32) }
    val CGImageGetBitmapInfo by func { declare(pointer, retType = int32) }
    val CGImageGetBitsPerComponent by func { declare(pointer, retType = int64) }
    val CGImageGetBitsPerPixel by func { declare(pointer, retType = int64) }
    val CGImageGetBytesPerRow by func { declare(pointer, retType = int64) }
    val CGImageGetColorSpace by func { declare(pointer, retType = pointer) }
    val CGImageGetDataProvider by func { declare(pointer, retType = pointer) }
    val CGImageGetHeight by func { declare(pointer, retType = int64) }
    val CGImageGetWidth by func { declare(pointer, retType = int64) }
    val CGImageRelease by func { declare(pointer) }
    val CGImageRetain by func { declare(pointer, retType = pointer) }
    val CGMainDisplayID by func { declare(retType = int32) }
    val CGPathAddCurveToPoint by func { declare(pointer, pointer, float64, float64, float64, float64, float64, float64) }
    val CGPathAddEllipseInRect by func { declare(pointer, pointer, rect) }
    val CGPathAddLineToPoint by func { declare(pointer, pointer, float64, float64) }
    val CGPathAddPath by func { declare(pointer, pointer, pointer) }
    val CGPathAddQuadCurveToPoint by func { declare(pointer, pointer, float64, float64, float64, float64) }
    val CGPathAddRect by func { declare(pointer, pointer, rect) }
    val CGPathCloseSubpath by func { declare(pointer) }
    val CGPathContainsPoint by func { declare(pointer, pointer, point, boolean, retType = boolean) }
    val CGPathCreateCopy by func { declare(pointer, retType = pointer) }
    val CGPathCreateMutable by func { declare(retType = pointer) }
    val CGPathCreateWithEllipseInRect by func { declare(rect, pointer, retType = pointer) }
    val CGPathCreateWithRect by func { declare(rect, pointer, retType = pointer) }
    val CGPathCreateWithRoundedRect by func { declare(rect, float64, float64, pointer, retType = pointer) }
    val CGPathGetBoundingBox by func { declare(pointer, retType = rect) }
    val CGPathGetPathBoundingBox by func { declare(pointer, retType = rect) }
    val CGPathIsEmpty by func { declare(pointer, retType = boolean) }
    val CGPathMoveToPoint by func { declare(pointer, pointer, float64, float64) }
    val CGPointApplyAffineTransform by func { declare(point, transform, retType = point) }
    val CGPreflightScreenCaptureAccess by func { declare(retType = boolean) }
    val CGRectApplyAffineTransform by func { declare(rect, transform, retType = rect) }
    val CGRectContainsPoint by func { declare(rect, point, retType = boolean) }
    val CGRectContainsRect by func { declare(rect, rect, retType = boolean) }
    val CGRectGetHeight by func { declare(rect, retType = float64) }
    val CGRectGetMaxX by func { declare(rect, retType = float64) }
    val CGRectGetMaxY by func { declare(rect, retType = float64) }
    val CGRectGetMidX by func { declare(rect, retType = float64) }
    val CGRectGetMidY by func { declare(rect, retType = float64) }
    val CGRectGetMinX by func { declare(rect, retType = float64) }
    val CGRectGetMinY by func { declare(rect, retType = float64) }
    val CGRectGetWidth by func { declare(rect, retType = float64) }
    val CGRectInset by func { declare(rect, float64, float64, retType = rect) }
    val CGRectIntegral by func { declare(rect, retType = rect) }
    val CGRectIntersection by func { declare(rect, rect, retType = rect) }
    val CGRectIntersectsRect by func { declare(rect, rect, retType = boolean) }
    val CGRectIsEmpty by func { declare(rect, retType = boolean) }
    val CGRectIsInfinite by func { declare(rect, retType = boolean) }
    val CGRectIsNull by func { declare(rect, retType = boolean) }
    val CGRectOffset by func { declare(rect, float64, float64, retType = rect) }
    val CGRectUnion by func { declare(rect, rect, retType = rect) }
    val CGRequestScreenCaptureAccess by func { declare(retType = boolean) }
    val CGSizeApplyAffineTransform by func { declare(size, transform, retType = size) }
    val CGWindowListCopyWindowInfo by func { declare(int32, int32, retType = pointer) }
    val CGWindowListCreate by func { declare(int32, int32, retType = pointer) }
    val CGWindowListCreateDescriptionFromArray by func { declare(pointer, retType = pointer) }
}
