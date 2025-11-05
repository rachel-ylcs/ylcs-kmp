package love.yinlin.platform

internal external fun nativeInit(path: String): Long
internal external fun nativeSetBoolean(handle: Long, key: String, value: Boolean, expire: Int)
internal external fun nativeSetInt(handle: Long, key: String, value: Int, expire: Int)
internal external fun nativeSetLong(handle: Long, key: String, value: Long, expire: Int)
internal external fun nativeSetFloat(handle: Long, key: String, value: Float, expire: Int)
internal external fun nativeSetDouble(handle: Long, key: String, value: Double, expire: Int)
internal external fun nativeSetString(handle: Long, key: String, value: String, expire: Int)
internal external fun nativeSetByteArray(handle: Long, key: String, value: ByteArray, expire: Int)
external fun nativeGetBoolean(handle: Long, key: String, default: Boolean): Boolean
external fun nativeGetInt(handle: Long, key: String, default: Int): Int
external fun nativeGetLong(handle: Long, key: String, default: Long): Long
external fun nativeGetFloat(handle: Long, key: String, default: Float): Float
external fun nativeGetDouble(handle: Long, key: String, default: Double): Double
external fun nativeGetString(handle: Long, key: String, default: String): String
external fun nativeGetByteArray(handle: Long, key: String, default: ByteArray): ByteArray
internal external fun nativeContains(handle: Long, key: String): Boolean
internal external fun nativeRemove(handle: Long, key: String)