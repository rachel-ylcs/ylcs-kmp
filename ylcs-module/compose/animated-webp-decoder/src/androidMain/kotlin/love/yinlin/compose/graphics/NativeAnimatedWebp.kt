package love.yinlin.compose.graphics

internal external fun nativeAnimatedWebpCreate(data: ByteArray): Long
internal external fun nativeAnimatedWebpRelease(handle: Long)
internal external fun nativeAnimatedWebpGetWidth(handle: Long): Int
internal external fun nativeAnimatedWebpGetHeight(handle: Long): Int