package io.github.alexzhirkevich.qrose.options

import androidx.compose.runtime.Stable

/**
 * Style of the qr-code pixels.
 * */
@Stable
fun interface QrPixelShape : QrShapeModifier {

    companion object {
        val Default : QrPixelShape = square()
    }
}

@Stable
fun QrPixelShape.Companion.square(size: Float = 1f) : QrPixelShape =
    object : QrPixelShape, QrShapeModifier by SquareShape(size){}

@Stable
fun QrPixelShape.Companion.circle(size: Float = 1f) : QrPixelShape =
    object : QrPixelShape, QrShapeModifier by CircleShape(size){}

@Stable
fun QrPixelShape.Companion.roundCorners(radius : Float = .5f) : QrPixelShape =
    object : QrPixelShape, QrShapeModifier by RoundCornersShape(radius,true){}

@Stable
fun QrPixelShape.Companion.verticalLines(width : Float = 1f) : QrPixelShape =
    object : QrPixelShape, QrShapeModifier by VerticalLinesShape(width){}

@Stable
fun QrPixelShape.Companion.horizontalLines(width : Float = 1f) : QrPixelShape =
    object : QrPixelShape, QrShapeModifier by HorizontalLinesShape(width){}