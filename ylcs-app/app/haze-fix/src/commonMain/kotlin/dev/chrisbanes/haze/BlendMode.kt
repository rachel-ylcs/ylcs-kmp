// Copyright 2024, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0

package dev.chrisbanes.haze

import androidx.compose.ui.graphics.BlendMode

/**
 * Sealed interface representing supported blend modes for platform operations.
 */
sealed interface HazeBlendMode {
  data object Clear : HazeBlendMode
  data object Src : HazeBlendMode
  data object Dst : HazeBlendMode
  data object SrcOver : HazeBlendMode
  data object DstOver : HazeBlendMode
  data object SrcIn : HazeBlendMode
  data object DstIn : HazeBlendMode
  data object SrcOut : HazeBlendMode
  data object DstOut : HazeBlendMode
  data object SrcAtop : HazeBlendMode
  data object DstAtop : HazeBlendMode
  data object Xor : HazeBlendMode
  data object Plus : HazeBlendMode
  data object Modulate : HazeBlendMode
  data object Screen : HazeBlendMode
  data object Overlay : HazeBlendMode
  data object Darken : HazeBlendMode
  data object Lighten : HazeBlendMode
  data object ColorDodge : HazeBlendMode
  data object ColorBurn : HazeBlendMode
  data object Hardlight : HazeBlendMode
  data object Softlight : HazeBlendMode
  data object Difference : HazeBlendMode
  data object Exclusion : HazeBlendMode
  data object Multiply : HazeBlendMode
  data object Hue : HazeBlendMode
  data object Saturation : HazeBlendMode
  data object Color : HazeBlendMode
  data object Luminosity : HazeBlendMode
    companion object {
    /**
     * Converts a Compose [BlendMode] to a [HazeBlendMode].
     */
    fun from(blendMode: BlendMode): HazeBlendMode = when (blendMode) {
      BlendMode.Clear -> Clear
      BlendMode.Src -> Src
      BlendMode.Dst -> Dst
      BlendMode.SrcOver -> SrcOver
      BlendMode.DstOver -> DstOver
      BlendMode.SrcIn -> SrcIn
      BlendMode.DstIn -> DstIn
      BlendMode.SrcOut -> SrcOut
      BlendMode.DstOut -> DstOut
      BlendMode.SrcAtop -> SrcAtop
      BlendMode.DstAtop -> DstAtop
      BlendMode.Xor -> Xor
      BlendMode.Plus -> Plus
      BlendMode.Modulate -> Modulate
      BlendMode.Screen -> Screen
      BlendMode.Overlay -> Overlay
      BlendMode.Darken -> Darken
      BlendMode.Lighten -> Lighten
      BlendMode.ColorDodge -> ColorDodge
      BlendMode.ColorBurn -> ColorBurn
      BlendMode.Hardlight -> Hardlight
      BlendMode.Softlight -> Softlight
      BlendMode.Difference -> Difference
      BlendMode.Exclusion -> Exclusion
      BlendMode.Multiply -> Multiply
      BlendMode.Hue -> Hue
      BlendMode.Saturation -> Saturation
      BlendMode.Color -> Color
      BlendMode.Luminosity -> Luminosity
      else -> SrcOver
    }
  }
}

/**
 * Converts a Compose [BlendMode] to a [HazeBlendMode].
 */
fun BlendMode.toHazeBlendMode(): HazeBlendMode = HazeBlendMode.from(this)
