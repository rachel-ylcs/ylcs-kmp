// Copyright 2025, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0

package dev.chrisbanes.haze.blur

import androidx.collection.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import dev.chrisbanes.haze.VisualEffectContext

/**
 * Calculates the blur tile mode for a blur visual effect.
 */
internal fun BlurVisualEffect.calculateBlurTileMode(): TileMode = when (blurredEdgeTreatment) {
  androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded -> TileMode.Decal
  else -> TileMode.Clamp
}

internal fun BlurVisualEffect.getOrCreateRenderEffect(
  context: VisualEffectContext,
  inputScale: Float = resolveInputScaleFactor(context.inputScale),
  blurRadius: Dp = this.blurRadius.takeOrElse { 0.dp },
  noiseFactor: Float = this.noiseFactor,
  colorEffects: List<HazeColorEffect> = this.colorEffects,
  colorEffectsAlphaModulate: Float = 1f,
  contentSize: Size = context.size,
  contentOffset: Offset = context.layerOffset,
  mask: Brush? = this.mask,
  progressive: HazeProgressive? = null,
  blurTileMode: TileMode = calculateBlurTileMode(),
): RenderEffect? = getOrCreateRenderEffect(
    context = context,
    params = RenderEffectParams(
        blurRadius = blurRadius,
        noiseFactor = noiseFactor,
        scale = inputScale,
        colorEffects = colorEffects,
        colorEffectsAlphaModulate = colorEffectsAlphaModulate,
        contentSize = contentSize,
        contentOffset = contentOffset,
        mask = mask,
        progressive = progressive,
        blurTileMode = blurTileMode,
    ),
)

private val renderEffectCache = lazy(mode = LazyThreadSafetyMode.NONE) {
  LruCache<RenderEffectParams, RenderEffect>(maxSize = 50)
}

internal fun clearRenderEffectCache() {
  clearIfInitialized(renderEffectCache) { it.evictAll() }
}

internal inline fun <T> clearIfInitialized(lazyValue: Lazy<T>, clear: (T) -> Unit) {
  if (lazyValue.isInitialized()) {
    clear(lazyValue.value)
  }
}

internal class RenderEffectParams(
  val blurRadius: Dp,
  val noiseFactor: Float,
  val scale: Float,
  val contentSize: Size,
  val contentOffset: Offset,
  val colorEffects: List<HazeColorEffect> = emptyList(),
  val colorEffectsAlphaModulate: Float = 1f,
  val mask: Brush? = null,
  val progressive: HazeProgressive? = null,
  val blurTileMode: TileMode,
)

private fun getOrCreateRenderEffect(context: VisualEffectContext, params: RenderEffectParams): RenderEffect {
  val cached = renderEffectCache.value[params]
  if (cached != null) {
    return cached
  }

  return createRenderEffect(
    context = context.requirePlatformContext(),
    density = context.requireDensity(),
    params = params,
  ).also { effect ->
    renderEffectCache.value.put(params, effect)
  }
}
