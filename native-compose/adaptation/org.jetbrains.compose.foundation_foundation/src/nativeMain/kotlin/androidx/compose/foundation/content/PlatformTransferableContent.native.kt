package androidx.compose.foundation.content

import androidx.compose.runtime.Stable

/** Owned file paths provided by SDL; no files are opened or native event pointers retained. */
@Stable
actual class PlatformTransferableContent internal constructor(val filePaths: List<String>)
