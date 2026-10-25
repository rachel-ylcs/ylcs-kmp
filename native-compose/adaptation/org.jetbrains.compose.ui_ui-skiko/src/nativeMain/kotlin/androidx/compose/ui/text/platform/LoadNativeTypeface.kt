/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.font.Font
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontSlant
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.FontWeight
import org.jetbrains.skia.FontWidth
import org.jetbrains.skia.Typeface
import org.jetbrains.skia.impl.use

internal fun loadNativeTypeface(font: Font): Typeface {
    require(font is PlatformFont) { "Unsupported font type: $font" }
    val typeface = when (font) {
        is LoadedFont -> Data.makeFromBytes(font.data).use { FontMgr.default.makeFromData(it) }
        is SystemFont -> FontMgr.default.legacyMakeTypeface(font.identity, FontStyle(
            weight = FontWeight(font.weight.weight),
            width = FontWidth.NORMAL,
            slant = if (font.style == androidx.compose.ui.text.font.FontStyle.Italic) FontSlant.ITALIC else FontSlant.UPRIGHT
        ))
        else -> throw IllegalArgumentException("Unsupported font type: $font")
    } ?: error("Unable to load font: ${font.identity}")
    if (font.variationSettings.settings.isEmpty()) return typeface
    return typeface.use { it.cloneWithVariationSettings(font.variationSettings) }
}
