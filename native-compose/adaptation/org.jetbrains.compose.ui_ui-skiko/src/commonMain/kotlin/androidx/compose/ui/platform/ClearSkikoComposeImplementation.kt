package androidx.compose.ui.platform

import androidx.compose.ui.InternalComposeUiApi

/** Clears the registered backends for tests or controlled teardown with no active graphics users. */
@InternalComposeUiApi
expect fun clearSkikoComposeImplementation()
