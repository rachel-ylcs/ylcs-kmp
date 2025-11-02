package love.yinlin.platform

internal external fun nativeCreatePlayer(): Long
internal external fun nativeReleasePlayer(handle: Long)
internal external fun nativeIsPlaying(handle: Long): Boolean
internal external fun nativeGetPosition(handle: Long): Long
internal external fun nativeGetDuration(handle: Long): Long
internal external fun nativeLoad(handle: Long, path: String)
internal external fun nativePlay(handle: Long)
internal external fun nativePause(handle: Long)
internal external fun nativeStop(handle: Long)