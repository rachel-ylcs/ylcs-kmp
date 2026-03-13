@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.window

import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.AVKit.*
import platform.CoreGraphics.*
import platform.CoreMedia.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSEC_PER_SEC

interface PictureInPictureDelegate {
    fun didStartPictureInPicture()
    fun didStopPictureInPicture()
    fun didFailToStartPictureInPicture(error: NSError)
}

class PIPView(frame: CValue<CGRect>) : UIView(frame),
    AVPictureInPictureSampleBufferPlaybackDelegateProtocol,
    AVPictureInPictureControllerDelegateProtocol {
    val displayLayer: AVSampleBufferDisplayLayer get() = this.layer as AVSampleBufferDisplayLayer
    var delegate: PictureInPictureDelegate? = null
    var pipController: AVPictureInPictureController? = null

    @OptIn(BetaInteropApi::class)
    fun setupPIP() {
        if (pipController != null) return

        val contentSource = AVPictureInPictureControllerContentSource()
        AVPictureInPictureControllerContentSource.create(displayLayer, this)

        pipController = AVPictureInPictureController(contentSource = contentSource).also {
            it.delegate = this
            it.setValue(NSNumber(int = 1), forKey = "controlsStyle")
        }
    }

    fun getSnapshot(): CMSampleBufferRef? {
        val subview = subviews.firstOrNull() as? UIView ?: return null
        return subview.asImage().asSampleBuffer()
    }

    override fun pictureInPictureController(pictureInPictureController: AVPictureInPictureController, setPlaying: Boolean) {

    }

    override fun pictureInPictureControllerTimeRangeForPlayback(pictureInPictureController: AVPictureInPictureController): CValue<CMTimeRange> {
        return CMTimeRangeMake(
            start = kCMTimeIndefinite.readValue(),
            duration = CMTimeMakeWithSeconds(Double.MAX_VALUE, NSEC_PER_SEC.toInt())
        )
    }

    override fun pictureInPictureControllerIsPlaybackPaused(pictureInPictureController: AVPictureInPictureController): Boolean {
        return false
    }

    override fun pictureInPictureController(pictureInPictureController: AVPictureInPictureController, didTransitionToRenderSize: CValue<CMVideoDimensions>) {

    }

    override fun pictureInPictureController(pictureInPictureController: AVPictureInPictureController, skipByInterval: CValue<CMTime>, completionHandler: () -> Unit) {
        completionHandler()
    }

    override fun pictureInPictureController(pictureInPictureController: AVPictureInPictureController, failedToStartPictureInPictureWithError: NSError) {
        delegate?.didFailToStartPictureInPicture(failedToStartPictureInPictureWithError)
    }

    override fun pictureInPictureControllerDidStartPictureInPicture(pictureInPictureController: AVPictureInPictureController) {
        delegate?.didStartPictureInPicture()
    }

    override fun pictureInPictureControllerDidStopPictureInPicture(pictureInPictureController: AVPictureInPictureController) {
        delegate?.didStopPictureInPicture()
    }
}