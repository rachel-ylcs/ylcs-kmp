package io.github.alexzhirkevich.qrose

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Converts [Painter] to [ImageBitmap] with desired [width], [height], [alpha] and [colorFilter]
 * */
fun Painter.toImageBitmap(
    width : Int,
    height : Int,
    alpha : Float = 1f,
    colorFilter: ColorFilter? = null
) : ImageBitmap {

    val bmp = ImageBitmap(width, height)
    val canvas = Canvas(bmp)

    CanvasDrawScope().draw(
        density = Density(1f, 1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(width.toFloat(), height.toFloat())
    ) {
        draw(this@draw.size, alpha, colorFilter)
    }

    return bmp
}