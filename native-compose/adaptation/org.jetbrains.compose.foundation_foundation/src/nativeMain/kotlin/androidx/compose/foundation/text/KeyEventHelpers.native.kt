package androidx.compose.foundation.text

import androidx.compose.ui.input.key.KeyEvent

internal actual fun KeyEvent.cancelsTextSelection(): Boolean = false
