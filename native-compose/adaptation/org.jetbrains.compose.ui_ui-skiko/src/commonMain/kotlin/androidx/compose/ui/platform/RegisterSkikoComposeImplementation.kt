package androidx.compose.ui.platform

import androidx.compose.ui.InternalComposeUiApi

/** Registers the Skiko graphics and text backends; repeated registration of this backend is safe. */
@InternalComposeUiApi
expect fun registerSkikoComposeImplementation()
