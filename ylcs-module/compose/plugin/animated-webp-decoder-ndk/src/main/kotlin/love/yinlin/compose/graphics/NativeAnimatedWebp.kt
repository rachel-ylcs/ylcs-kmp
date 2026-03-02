package love.yinlin.compose.graphics

import java.nio.ByteBuffer

external fun nativeAnimatedWebpCreate(data: ByteArray): Long
external fun nativeAnimatedWebpRelease(handle: Long)
external fun nativeAnimatedWebpGetWidth(handle: Long): Int
external fun nativeAnimatedWebpGetHeight(handle: Long): Int
external fun nativeAnimatedWebpGetFrameCount(handle: Long): Int
external fun nativeAnimatedWebpHasMoreFrames(handle: Long): Boolean
external fun nativeAnimatedWebpGetNext(handle: Long, buffer: ByteBuffer): Boolean
external fun nativeAnimatedWebpReset(handle: Long)