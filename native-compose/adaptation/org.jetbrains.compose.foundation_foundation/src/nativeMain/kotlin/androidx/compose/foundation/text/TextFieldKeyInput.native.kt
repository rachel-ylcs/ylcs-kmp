package androidx.compose.foundation.text

import androidx.compose.ui.input.key.KeyEvent

// SDL_TEXT_INPUT supplies committed text, including IME and AltGr. Inserting KEY_DOWN text too
// would duplicate characters and expose uncommitted preedit to both text-field implementations.
internal actual val KeyEvent.isTypedEvent: Boolean get() = false
