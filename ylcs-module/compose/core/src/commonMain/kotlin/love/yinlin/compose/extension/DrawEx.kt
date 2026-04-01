package love.yinlin.compose.extension

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawTransform

fun DrawTransform.translate(offset: Offset) = translate(offset.x, offset.y)

fun DrawTransform.scale(ratio: Float, pivot: Offset = center) = scale(ratio, ratio, pivot)

fun DrawTransform.flipX(pivot: Offset = center) = scale(-1f, 1f, pivot)

fun DrawTransform.flipY(pivot: Offset = center) = scale(1f, -1f, pivot)