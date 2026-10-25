package androidx.compose.ui.text.platform

import androidx.compose.ui.text.font.Font
import org.jetbrains.skia.Typeface

internal actual fun loadTypeface(font: Font): Typeface = loadNativeTypeface(font)

internal actual fun currentPlatform(): Platform = Platform.MacOS
