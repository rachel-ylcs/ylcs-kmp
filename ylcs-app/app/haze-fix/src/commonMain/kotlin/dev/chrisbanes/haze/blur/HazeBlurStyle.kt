// Copyright 2024, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0

package dev.chrisbanes.haze.blur

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp

/**
 * A [ProvidableCompositionLocal] which provides the default [HazeBlurStyle] for all [dev.chrisbanes.haze.hazeEffect]
 * layout nodes placed within this composition local's content.
 *
 * There are precedence rules to how each styling property is applied. The order of precedence
 * for each property are as follows:
 *
 *  - Value set in [dev.chrisbanes.haze.HazeEffectScope], if specified.
 *  - Value set in style provided to [dev.chrisbanes.haze.hazeEffect] (or [dev.chrisbanes.haze.HazeEffectScope.style]), if specified.
 *  - Value set in this composition local.
 */
val LocalHazeBlurStyle: ProvidableCompositionLocal<HazeBlurStyle> =
  compositionLocalOf { HazeBlurDefaults.style(Color.Unspecified) }

/**
 * A holder for the style properties used by Haze.
 *
 * Can be set via [dev.chrisbanes.haze.hazeSource] and [dev.chrisbanes.haze.hazeEffect].
 *
 * @property backgroundColor Color to draw behind the blurred content. Ideally should be opaque
 * so that the original content is not visible behind. Typically this would be
 * `MaterialTheme.colorScheme.surface` or similar.
 * @property colorEffects The to apply to the blurred content.
 * @property blurRadius Radius of the blur.
 * @property noiseFactor Amount of noise applied to the content, in the range `0f` to `1f`.
 * Anything outside of that range will be clamped.
 * @property fallbackColorEffect The to use when Haze uses the fallback scrim functionality.
 * The scrim used whenever blurring is disabled, either because the host platform does not
 * support blurring, or it has been manually disabled.
 * When the fallback tint is used, the tints provided in [colorEffects] are ignored.
 */
@Immutable
data class HazeBlurStyle(
  val backgroundColor: Color = Color.Unspecified,
  val colorEffects: List<HazeColorEffect> = emptyList(),
  val blurRadius: Dp = Dp.Unspecified,
  val noiseFactor: Float = -1f,
  val fallbackColorEffect: HazeColorEffect = HazeColorEffect.Unspecified,
) {
   constructor(
    backgroundColor: Color = Color.Unspecified,
    colorEffect: HazeColorEffect? = null,
    blurRadius: Dp = Dp.Unspecified,
    noiseFactor: Float = -1f,
    fallbackColorEffect: HazeColorEffect = HazeColorEffect.Unspecified,
  ) : this(
    backgroundColor = backgroundColor,
    colorEffects = listOfNotNull(colorEffect),
    blurRadius = blurRadius,
    noiseFactor = noiseFactor,
    fallbackColorEffect = fallbackColorEffect,
  )

   companion object {
     val Unspecified: HazeBlurStyle = HazeBlurStyle(colorEffects = emptyList())
  }
}

/**
 * Describes a color effect applied by the haze effect.
 *
 * This is a sealed interface with concrete implementations for color filters and tints.
 * Follows the Compose UI model where ColorFilter is a top-level effect.
 */
@Stable
sealed interface HazeColorEffect {
  /**
   * The blend mode to use when applying the effect.
   */
  val blendMode: BlendMode

  /**
   * Whether this effect is specified (not [Unspecified]).
   */
  val isSpecified: Boolean

  /**
   * A color filter effect.
   */
  @Immutable
  data class ColorFilter(val colorFilter: androidx.compose.ui.graphics.ColorFilter,
    override val blendMode: BlendMode = DefaultBlendMode,
  ) : HazeColorEffect {
    override val isSpecified: Boolean get() = true
  }

  /**
   * A color-based tint effect.
   */
  @Immutable
  data class TintColor(val color: Color,
    override val blendMode: BlendMode = DefaultBlendMode,
  ) : HazeColorEffect {
    override val isSpecified: Boolean get() = color.isSpecified
  }

  /**
   * A brush-based tint effect.
   */
  @Immutable
  data class TintBrush(val brush: Brush,
    override val blendMode: BlendMode = DefaultBlendMode,
  ) : HazeColorEffect {
    override val isSpecified: Boolean = true
  }

  /**
   * An unspecified color effect. When used, no effect will be applied.
   */
  object Unspecified : HazeColorEffect {
    override val blendMode: BlendMode = BlendMode.SrcOver
    override val isSpecified: Boolean = false
  }

  @Suppress("NOTHING_TO_INLINE")
  companion object {
    /**
     * Default blend mode for effects.
     */
    val DefaultBlendMode: BlendMode = BlendMode.SrcOver

    /**
     * Creates a color-based tint effect.
     */
    inline fun tint(
      color: Color,
      blendMode: BlendMode = DefaultBlendMode,
    ): HazeColorEffect = TintColor(color, blendMode)
  }
}


internal inline fun Float.takeOrElse(block: () -> Float): Float =
  if (this in 0f..1f) this else block()
