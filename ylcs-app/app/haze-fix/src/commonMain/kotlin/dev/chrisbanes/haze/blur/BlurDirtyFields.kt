// Copyright 2025, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0

package dev.chrisbanes.haze.blur

@Suppress("ConstPropertyName", "ktlint:standard:property-naming")
internal object BlurDirtyFields {
  const val BlurEnabled: Int = 0b1
  const val BlurRadius: Int = BlurEnabled shl 1
  const val NoiseFactor: Int = BlurRadius shl 1
  const val Mask: Int = NoiseFactor shl 1
  const val BackgroundColor: Int = Mask shl 1
  const val ColorEffects: Int = BackgroundColor shl 1
  const val FallbackColorEffect: Int = ColorEffects shl 1
  const val Alpha: Int = FallbackColorEffect shl 1
  const val Progressive: Int = Alpha shl 1
  const val BlurredEdgeTreatment: Int = Progressive shl 1

  const val RenderEffectAffectingFlags: Int =
    BlurEnabled or
      BlurRadius or
      NoiseFactor or
      Mask or
      ColorEffects or
      FallbackColorEffect or
      Progressive or
      BlurredEdgeTreatment

  const val InvalidateFlags: Int =
    RenderEffectAffectingFlags or
      BackgroundColor or
      Alpha
}
