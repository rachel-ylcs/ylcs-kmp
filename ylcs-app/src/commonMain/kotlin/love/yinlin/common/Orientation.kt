package love.yinlin.common

import androidx.compose.ui.unit.Dp
import love.yinlin.extension.localComposition

enum class Orientation {
    PORTRAIT, LANDSCAPE, SQUARE;

    companion object {
        fun fromSize(width: Dp, height: Dp): Orientation = when {
            height >= width * 1.2f -> PORTRAIT
            width >= height * 1.2f -> LANDSCAPE
            else -> SQUARE
        }
    }
}

val LocalOrientation = localComposition<Orientation>()