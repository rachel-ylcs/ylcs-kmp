package love.yinlin.compose.graphics

import java.nio.ByteBuffer

internal external fun nativeAnimatedWebpCreate(data: ByteArray): Long
internal external fun nativeAnimatedWebpRelease(handle: Long)
internal external fun nativeAnimatedWebpGetWidth(handle: Long): Int
internal external fun nativeAnimatedWebpGetHeight(handle: Long): Int
internal external fun nativeAnimatedWebpGetFrameCount(handle: Long): Int
internal external fun nativeAnimatedWebpHasMoreFrames(handle: Long): Boolean
internal external fun nativeAnimatedWebpGetNext(handle: Long, buffer: ByteBuffer): Boolean
internal external fun nativeAnimatedWebpReset(handle: Long)