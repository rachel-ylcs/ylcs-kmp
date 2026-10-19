package love.yinlin.compose.ui.node

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect

internal interface BlurRenderer {
    fun createEffect(blurRadiusPx: Float, tintColor: Color, noiseFactor: Float, inputScale: Float, ): RenderEffect?
    fun release()
}
