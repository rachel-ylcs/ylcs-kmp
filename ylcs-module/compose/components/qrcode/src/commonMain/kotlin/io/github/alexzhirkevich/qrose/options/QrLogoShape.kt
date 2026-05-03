package io.github.alexzhirkevich.qrose.options

import androidx.compose.runtime.Stable

@Stable
interface QrLogoShape : QrShapeModifier {

    companion object {
        val Default : QrLogoShape = object : QrLogoShape, QrShapeModifier by SquareShape(){}
    }

}

@Stable
fun QrLogoShape.Companion.circle() : QrLogoShape=
    object  : QrLogoShape, QrShapeModifier by CircleShape(1f){}

@Stable
fun QrLogoShape.Companion.rect(aspectRatio : Float, cornerRadius : Float = 0f) : QrLogoShape=
    object  : QrLogoShape, QrShapeModifier by RectangleShape(1f, aspectRatio,cornerRadius){}

@Stable
fun QrLogoShape.Companion.oval(aspectRatio : Float) : QrLogoShape=
    object  : QrLogoShape, QrShapeModifier by OvalShape(1f, aspectRatio){}


@Stable
fun QrLogoShape.Companion.roundCorners(radius: Float) : QrLogoShape=
    object  : QrLogoShape, QrShapeModifier by RoundCornersShape(radius, false){}
