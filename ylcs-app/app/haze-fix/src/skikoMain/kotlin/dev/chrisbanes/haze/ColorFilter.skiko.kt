// Copyright 2025, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0

package dev.chrisbanes.haze

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asSkiaColorFilter

/**
 * Converts a Compose [ColorFilter] to a Skia platform [PlatformColorFilter].
 */
actual fun ColorFilter.toPlatformColorFilter(): PlatformColorFilter {
  return asSkiaColorFilter()
}
