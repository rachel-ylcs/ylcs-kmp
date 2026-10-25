package androidx.compose.ui.text.platform

import androidx.compose.ui.text.PlatformStringDelegate

internal actual fun ActualStringDelegate(): PlatformStringDelegate = IcuStringDelegate()
