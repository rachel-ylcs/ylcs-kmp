package androidx.compose.foundation.content

import androidx.compose.ui.platform.ClipEntry

actual fun TransferableContent.hasMediaType(mediaType: MediaType): Boolean = clipMetadata.hasMimeType(mediaType.representation)

internal actual fun ClipEntry.readPlainText(): String? = if (clipMetadata.hasMimeType("text/plain")) getPlainText() else null
