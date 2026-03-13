@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.window

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.CoreMedia.*
import platform.CoreVideo.*
import platform.UIKit.*

internal fun UIView.asImage(): UIImage {
    var offset: CValue<CGPoint>? = null
    if (this is UIScrollView) {
        offset = contentOffset
        val bottomOffsetY = contentSize.useContents { height } - bounds.useContents { size.height }
        if (bottomOffsetY >= 0) contentOffset = CGPointMake(0.0, bottomOffsetY)
    }

    val superview = this.superview
    val index = superview?.subviews?.indexOf(this)?.toLong() ?: 0L

    if (window?.windowScene?.activationState == UISceneActivationStateBackground) removeFromSuperview()

    val renderer = UIGraphicsImageRenderer(bounds = bounds)
    val image = renderer.imageWithActions { context ->
        layer.renderInContext(context?.CGContext)
    }

    if (offset != null) (this as? UIScrollView)?.contentOffset = offset

    if (this.superview == null) superview?.insertSubview(this, atIndex = index)

    return image
}

internal fun UIImage.asBuffer(): CVPixelBufferRef? = memScoped {
    val width = size.useContents { width }.toLong()
    val height = size.useContents { height }.toLong()

    // 构建属性字典
    val dict = CFDictionaryCreateMutable(null, 0, null, null)
    CFDictionaryAddValue(dict, kCVPixelBufferIOSurfaceOpenGLESFBOCompatibilityKey, kCFBooleanTrue)
    CFDictionaryAddValue(dict, kCVPixelBufferIOSurfaceOpenGLESTextureCompatibilityKey, kCFBooleanTrue)

    val attrs = CFDictionaryCreateMutable(null, 0, null, null)
    CFDictionaryAddValue(attrs, kCVPixelBufferCGImageCompatibilityKey, kCFBooleanTrue)
    CFDictionaryAddValue(attrs, kCVPixelBufferCGBitmapContextCompatibilityKey, kCFBooleanTrue)
    CFDictionaryAddValue(attrs, kCVPixelBufferMetalCompatibilityKey, kCFBooleanTrue)
    CFDictionaryAddValue(attrs, kCVPixelBufferOpenGLCompatibilityKey, kCFBooleanTrue)
    CFDictionaryAddValue(attrs, kCVPixelBufferIOSurfacePropertiesKey, dict)

    val pixelBufferVar = alloc<CVPixelBufferRefVar>()
    val status = CVPixelBufferCreate(
        kCFAllocatorDefault,
        width,
        height,
        kCVPixelFormatType_32ARGB,
        attrs,
        pixelBufferVar.ptr
    )

    val pixelBuffer = pixelBufferVar.value ?: return null

    if (status != kCVReturnSuccess) return null

    CVPixelBufferLockBaseAddress(pixelBuffer, 0UL)
    val pixelData = CVPixelBufferGetBaseAddress(pixelBuffer)

    val rgbColorSpace = CGColorSpaceCreateDeviceRGB()
    val context = CGBitmapContextCreate(
        data = pixelData,
        width = width,
        height = height,
        bitsPerComponent = 8UL,
        bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer),
        space = rgbColorSpace,
        bitmapInfo = CGImageAlphaInfo.kCGImageAlphaNoneSkipFirst.value
    )

    if (context != null) {
        CGContextTranslateCTM(context, 0.0, height.toDouble())
        CGContextScaleCTM(context, 1.0, -1.0)

        UIGraphicsPushContext(context)
        drawInRect(CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
        UIGraphicsPopContext()
    }

    CVPixelBufferUnlockBaseAddress(pixelBuffer, 0UL)

    return pixelBuffer
}

// https://gist.github.com/rampadc/79c01eb3fa4eba0b941befa7c55f4e13
internal fun UIImage.asSampleBuffer(): CMSampleBufferRef? {
    val pixelBuffer = asBuffer() ?: return null
    return memScoped {
        val sampleBufferVar = alloc<CMSampleBufferRefVar>()
        val scale: CMTimeScale = 1_000_000_000 // NSEC_PER_SEC

        val timingInfo = alloc<CMSampleTimingInfo>().apply {
            presentationTimeStamp = CMTimeMake(0, scale)
            duration = CMTimeMakeWithSeconds(10.0, scale)
            decodeTimeStamp = CMTimeMake(0, scale)
        }

        val formatDescriptionVar = alloc<CMFormatDescriptionRefVar>()
        CMVideoFormatDescriptionCreateForImageBuffer(
            allocator = kCFAllocatorDefault,
            imageBuffer = pixelBuffer,
            formatDescriptionOut = formatDescriptionVar.ptr
        )

        CMSampleBufferCreateReadyWithImageBuffer(
            allocator = kCFAllocatorDefault,
            imageBuffer = pixelBuffer,
            formatDescription = formatDescriptionVar.value,
            sampleTiming = timingInfo.ptr,
            sampleBufferOut = sampleBufferVar.ptr
        )

        sampleBufferVar.value
    }
}