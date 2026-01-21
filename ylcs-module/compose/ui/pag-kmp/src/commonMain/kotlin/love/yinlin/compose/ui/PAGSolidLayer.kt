package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Stable
expect class PAGSolidLayer : PAGLayer {
    companion object {
        fun make(duration: Long, width: Int, height: Int, solidColor: Color, opacity: Int = 255): PAGSolidLayer
    }

    var solidColor: Color
}