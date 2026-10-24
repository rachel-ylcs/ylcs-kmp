package org.jetbrains.skiko

private const val Direct3DRenderTargetGrowthAlignment = 64L

internal data class Direct3DRenderTargetSize(val width: Int, val height: Int)

internal fun direct3DRenderTargetSize(
    currentWidth: Int,
    currentHeight: Int,
    requestedWidth: Int,
    requestedHeight: Int,
): Direct3DRenderTargetSize {
    require(requestedWidth > 0 && requestedHeight > 0)
    return Direct3DRenderTargetSize(
        width = expandedRenderTargetDimension(currentWidth, requestedWidth),
        height = expandedRenderTargetDimension(currentHeight, requestedHeight),
    )
}

private fun expandedRenderTargetDimension(current: Int, requested: Int): Int {
    if (current >= requested) return current
    val currentLong = current.toLong()
    val requestedLong = requested.toLong()
    val expanded = if (current <= 0) requestedLong + maxOf(requestedLong / 2L, Direct3DRenderTargetGrowthAlignment) else maxOf(requestedLong, currentLong + maxOf(currentLong / 2L, Direct3DRenderTargetGrowthAlignment))
    return ((expanded + Direct3DRenderTargetGrowthAlignment - 1L) / Direct3DRenderTargetGrowthAlignment * Direct3DRenderTargetGrowthAlignment).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}