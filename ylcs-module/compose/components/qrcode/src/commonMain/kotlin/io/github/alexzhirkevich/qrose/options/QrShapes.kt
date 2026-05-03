package io.github.alexzhirkevich.qrose.options

import androidx.compose.runtime.Immutable

/**
 * Shapes of QR code elements
 *
 * @param code shape of the QR code pattern.
 * @param darkPixel shape of the dark QR code pixels
 * @param lightPixel shape of the light QR code pixels
 * @param ball shape of the QR code eye balls
 * @param frame shape of the QR code eye frames
 * @param centralSymmetry if true, [ball] and [frame] shapes will be turned
 * to the center according to the current corner
 * */
@Immutable
class QrShapes(
    val code: QrCodeShape = QrCodeShape.Default,
    val darkPixel: QrPixelShape = QrPixelShape.Default,
    val lightPixel : QrPixelShape = QrPixelShape.Default,
    val ball : QrBallShape = QrBallShape.Default,
    val frame : QrFrameShape = QrFrameShape.Default,
    val centralSymmetry: Boolean = true
) {

    fun copy(
        code: QrCodeShape = this.code,
        darkPixel: QrPixelShape = this.darkPixel,
        lightPixel : QrPixelShape = this.lightPixel,
        ball : QrBallShape = this.ball,
        frame : QrFrameShape = this.frame,
        centralSymmetry: Boolean = this.centralSymmetry
    ): QrShapes = QrShapes(
        code = code,
        darkPixel = darkPixel,
        lightPixel = lightPixel,
        ball = ball,
        frame = frame,
        centralSymmetry = centralSymmetry
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as QrShapes

        if (code != other.code) return false
        if (darkPixel != other.darkPixel) return false
        if (lightPixel != other.lightPixel) return false
        if (ball != other.ball) return false
        if (frame != other.frame) return false
        if (centralSymmetry != other.centralSymmetry) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + darkPixel.hashCode()
        result = 31 * result + lightPixel.hashCode()
        result = 31 * result + ball.hashCode()
        result = 31 * result + frame.hashCode()
        result = 31 * result + centralSymmetry.hashCode()
        return result
    }
}