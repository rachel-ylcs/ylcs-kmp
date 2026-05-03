package io.github.alexzhirkevich.qrose.options.dsl

import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel
import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel.Auto

sealed interface QrOptionsBuilderScope  {

    /**
     * Level of error correction.
     * [Auto] by default
     * */
    var errorCorrectionLevel: QrErrorCorrectionLevel

    /**
     * Enable 4th qr code eye. False by default
     * */
    var fourEyed : Boolean

    /**
     *  QR code scale inside the painter. Can be used to add a padding from the image sides
     * */
    var scale : Float

    /**
     * Shapes of the QR code pattern and its parts.
     * */
    fun shapes(centralSymmetry : Boolean = true, block: QrShapesBuilderScope.() -> Unit)

    /**
     * Colors of QR code parts.
     * */
    fun colors(block: QrColorsBuilderScope.() -> Unit)

    /**
     * Middle image.
     * */
    fun logo(block: QrLogoBuilderScope.() -> Unit)

    /**
     * Background.
     * */
    fun background(block: QrBackgroundBuilderScope.() -> Unit)
}




