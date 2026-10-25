package androidx.compose.ui.graphics

import androidx.compose.ui.graphics.platform.PlatformColorFilter
import org.jetbrains.skia.colorMatrixOrNull

internal fun colorMatrixFromSkiko(filter: PlatformColorFilter): ColorMatrix {
    require(filter is SkikoColorFilter) { "Expected a Skiko color filter" }
    val matrix = requireNotNull(filter.skiaColorFilter.colorMatrixOrNull()) { "The filter cannot be represented as a color matrix" }
    val values = matrix.mat.copyOf()
    for (index in 4 ..< values.size step 5) values[index] *= 255f
    return ColorMatrix(values)
}
