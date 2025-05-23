import Foundation
import UIKit
import AVKit

@objc
public protocol PictureInPictureDelegate where Self: AnyObject {
    
    /// Start PIP.
    func didStartPictureInPicture()
    
    /// Stop PIP.
    func didStopPictureInPicture()
    
    /// PIP failed to start.
    func didFailToStartPictureInPicture(error: Error)
}

@objcMembers
public class PIPView: UIView, AVPictureInPictureSampleBufferPlaybackDelegate, AVPictureInPictureControllerDelegate {

    public override class var layerClass: AnyClass {
        AVSampleBufferDisplayLayer.self
    }
    
    public weak var delegate: PictureInPictureDelegate?
    
    public var pipController: AVPictureInPictureController?
        
    public func setupPIP() {
        if (pipController != nil) {
            return
        }
        pipController = AVPictureInPictureController(contentSource: .init(sampleBufferDisplayLayer: self.layer as! AVSampleBufferDisplayLayer, playbackDelegate: self))
        pipController?.delegate = self
        pipController?.setValue(1, forKey: "controlsStyle")
    }
    
    public func getSnapshot() -> CMSampleBuffer {
        let view = self.subviews.first!
        return createSampleBufferFrom(pixelBuffer: view.asImage().asBuffer()!)!
    }
    
    public func pictureInPictureController(_ pictureInPictureController: AVPictureInPictureController, setPlaying playing: Bool) {
    }
    
    public func pictureInPictureControllerTimeRangeForPlayback(_ pictureInPictureController: AVPictureInPictureController) -> CMTimeRange {
        CMTimeRange(start: .indefinite, duration: CMTime(seconds: .infinity, preferredTimescale: CMTimeScale(NSEC_PER_SEC)))
    }
    
    public func pictureInPictureControllerIsPlaybackPaused(_ pictureInPictureController: AVPictureInPictureController) -> Bool {
        false
    }
    
    public func pictureInPictureController(_ pictureInPictureController: AVPictureInPictureController, didTransitionToRenderSize newRenderSize: CMVideoDimensions) {
    }
    
    public func pictureInPictureController(_ pictureInPictureController: AVPictureInPictureController, skipByInterval skipInterval: CMTime, completion completionHandler: @escaping () -> Void) {
    }
    
    public func pictureInPictureController(_ pictureInPictureController: AVPictureInPictureController, failedToStartPictureInPictureWithError error: Error) {
        delegate?.didFailToStartPictureInPicture(error: error)
    }
    
    public func pictureInPictureControllerDidStartPictureInPicture(_ pictureInPictureController: AVPictureInPictureController) {
        delegate?.didStartPictureInPicture()
    }
    
    public func pictureInPictureControllerDidStopPictureInPicture(_ pictureInPictureController: AVPictureInPictureController) {
        delegate?.didStopPictureInPicture()
    }
}
