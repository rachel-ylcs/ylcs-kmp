package love.yinlin.compose.ui.image

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import love.yinlin.compose.data.CropRegion

@Stable
class CropImageState {
    var frameRect by mutableStateOf(Rect.Zero)
        internal set
    var imageRect by mutableStateOf(Rect.Zero)
        internal set

    val result: CropRegion get() {
        if (imageRect.isEmpty || frameRect.isEmpty) return CropRegion(0f, 0f, 0f, 0f)
        val frame = frameRect.intersect(imageRect)

        if (frame.isEmpty) return CropRegion(0f, 0f, 0f, 0f)
        return CropRegion(
            xPercent = ((frame.left - imageRect.left) / imageRect.width).coerceIn(0f, 1f),
            yPercent = ((frame.top - imageRect.top) / imageRect.height).coerceIn(0f, 1f),
            widthPercent = (frame.width / imageRect.width).coerceIn(0f, 1f),
            heightPercent = (frame.height / imageRect.height).coerceIn(0f, 1f),
        )
    }

    internal var resetGeneration by mutableIntStateOf(0)
        private set

    fun reset() {
        frameRect = Rect.Zero
        imageRect = Rect.Zero
        resetGeneration++
    }
}