package love.yinlin.compose.graphics

internal external fun nativeAnimatedWebpCreate(data: ByteArray): Long
internal external fun nativeAnimatedWebpRelease(handle: Long)
internal external fun nativeAnimatedWebpGetWidth(handle: Long): Int
internal external fun nativeAnimatedWebpGetHeight(handle: Long): Int
internal external fun nativeAnimatedWebpGetFrameCount(handle: Long): Int
internal external fun nativeAnimatedWebpCreateIterator(): Long
internal external fun nativeAnimatedWebpReleaseIterator(iteratorHandle: Long)
internal external fun nativeAnimatedWebpFirstFrame(handle: Long, iteratorHandle: Long): Boolean
internal external fun nativeAnimatedWebpNextFrame(iteratorHandle: Long): Boolean