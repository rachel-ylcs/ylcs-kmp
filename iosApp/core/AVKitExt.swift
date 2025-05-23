import UIKit
import AVKit

extension UIView {
  @objc
  open func asImage() -> UIImage {
    var offset: CGPoint?
    if let scrollView = self as? UIScrollView {
      offset = scrollView.contentOffset
      
      let bottomOffset = CGPoint(x: 0, y: scrollView.contentSize.height - scrollView.bounds.size.height)
      
      if bottomOffset.y >= 0 {
        scrollView.contentOffset = bottomOffset
      }
    }
    
    let superview = self.superview
    let index = self.subviews.firstIndex(of: self)
    if window?.windowScene?.activationState == .background {
      removeFromSuperview()
    }
    
    let renderer = UIGraphicsImageRenderer(bounds: bounds)

    let image = renderer.image {
        rendererContext in

        layer.render(in: rendererContext.cgContext)
    }
    
    if offset != nil {
      (self as? UIScrollView)?.contentOffset = offset!
    }
    
    if self.superview == nil {
      superview?.insertSubview(self, at: index ?? 0)
    }
    
    return image
  }
}

// From https://gist.github.com/jknthn/d8e197c036bb4b1c7c11b040a35580a7
extension UIImage {
  @objc
  open func asBuffer() -> CVPixelBuffer? {
    let dict: CFDictionary = [
      kCVPixelBufferIOSurfaceOpenGLESFBOCompatibilityKey : kCFBooleanTrue,
      kCVPixelBufferIOSurfaceOpenGLESTextureCompatibilityKey: kCFBooleanTrue
    ] as CFDictionary
    let attrs = [
      kCVPixelBufferCGImageCompatibilityKey: kCFBooleanTrue as Any,
      kCVPixelBufferCGBitmapContextCompatibilityKey: kCFBooleanTrue as Any,
      kCVPixelBufferMetalCompatibilityKey: kCFBooleanTrue as Any,
      kCVPixelBufferOpenGLCompatibilityKey: kCFBooleanTrue as Any,
      kCVPixelBufferIOSurfaceOpenGLESTextureCompatibilityKey: kCFBooleanTrue as Any,
      kCVPixelBufferIOSurfacePropertiesKey: dict
    ] as CFDictionary
    var pixelBuffer : CVPixelBuffer?
    let status = CVPixelBufferCreate(kCFAllocatorDefault, Int(self.size.width), Int(self.size.height), kCVPixelFormatType_32ARGB, attrs, &pixelBuffer)
    guard (status == kCVReturnSuccess) else {
      return nil
    }

    CVPixelBufferLockBaseAddress(pixelBuffer!, CVPixelBufferLockFlags(rawValue: 0))
    let pixelData = CVPixelBufferGetBaseAddress(pixelBuffer!)

    let rgbColorSpace = CGColorSpaceCreateDeviceRGB()
    let context = CGContext(data: pixelData, width: Int(self.size.width), height: Int(self.size.height), bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(pixelBuffer!), space: rgbColorSpace, bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue)

    context?.translateBy(x: 0, y: self.size.height)
    context?.scaleBy(x: 1.0, y: -1.0)

    UIGraphicsPushContext(context!)
    self.draw(in: CGRect(x: 0, y: 0, width: self.size.width, height: self.size.height))
    UIGraphicsPopContext()
    CVPixelBufferUnlockBaseAddress(pixelBuffer!, CVPixelBufferLockFlags(rawValue: 0))

    return pixelBuffer
  }

  @objc
  open func asSampleBuffer() -> CMSampleBuffer? {
      return createSampleBufferFrom(pixelBuffer: self.asBuffer()!)
  }
}

// https://gist.github.com/rampadc/79c01eb3fa4eba0b941befa7c55f4e13
public func createSampleBufferFrom(pixelBuffer: CVPixelBuffer) -> CMSampleBuffer? {
  var sampleBuffer: CMSampleBuffer?

  let scale = CMTimeScale(NSEC_PER_SEC)

  var timingInfo = CMSampleTimingInfo()
  timingInfo.presentationTimeStamp = .init(value: 0, timescale: scale)
  timingInfo.duration = CMTime(seconds: 10, preferredTimescale: scale)
  timingInfo.decodeTimeStamp = .init(value: 0, timescale: scale)
  var formatDescription: CMFormatDescription? = nil
  CMVideoFormatDescriptionCreateForImageBuffer(allocator: kCFAllocatorDefault, imageBuffer: pixelBuffer, formatDescriptionOut: &formatDescription)

  _ = CMSampleBufferCreateReadyWithImageBuffer(
    allocator: kCFAllocatorDefault,
    imageBuffer: pixelBuffer,
    formatDescription: formatDescription!,
    sampleTiming: &timingInfo,
    sampleBufferOut: &sampleBuffer
  )

  guard let buffer = sampleBuffer else {
    return nil
  }
  
  return buffer
}
