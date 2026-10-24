package org.jetbrains.skiko

internal data class Direct3DCompositionScale(val x: Float, val y: Float)

internal fun direct3DCompositionScale(
    sourceWidth: Int,
    sourceHeight: Int,
    renderTargetWidth: Int,
    renderTargetHeight: Int,
): Direct3DCompositionScale {
    require(sourceWidth > 0 && sourceHeight > 0 && renderTargetWidth >= sourceWidth && renderTargetHeight >= sourceHeight)
    return Direct3DCompositionScale(
        x = sourceWidth.toFloat() / renderTargetWidth.toFloat(),
        y = sourceHeight.toFloat() / renderTargetHeight.toFloat(),
    )
}