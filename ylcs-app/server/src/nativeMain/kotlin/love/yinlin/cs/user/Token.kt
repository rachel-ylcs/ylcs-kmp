package love.yinlin.cs.user

import kotlinx.cinterop.*
import love.yinlin.extension.DateEx
import love.yinlin.platform.Platform

@OptIn(ExperimentalForeignApi::class)
data class Token(
    val uid: Int,
    val platform: Platform,
    val magic: Int = MAGIC,
    val timestamp: Long = DateEx.CurrentLong
) {
    companion object {
        const val MAGIC = 19911211

        fun fromBytes(bytes: ByteArray): Token? = bytes.usePinned { pinned ->
            val ptr = pinned.addressOf(0)

            val intPtr = ptr.reinterpret<IntVar>()
            val uid = intPtr[0]
            val magic = intPtr[1]
            val platform = Platform.entries.getOrNull(intPtr[2])

            val longPtr = (ptr + 3 * sizeOf<IntVar>())!!.reinterpret<LongVar>()
            val timestamp = longPtr[0]

            return if (uid > 0 && magic == MAGIC && timestamp in 1727755860000..1917058260000 && platform != null)
                Token(uid = uid, platform = platform, timestamp = timestamp) else null
        }

        fun keys(uid: Int): List<String> = Platform.entries.map { "token/${it.ordinal}/$uid" }
    }

    val bytes: ByteArray by lazy {
        val data = ByteArray((sizeOf<IntVar>() * 3 + sizeOf<LongVar>()).toInt())
        data.usePinned { pinned ->
            val ptr = pinned.addressOf(0)

            val intPtr = ptr.reinterpret<IntVar>()
            intPtr[0] = uid
            intPtr[1] = 19911211
            intPtr[2] = platform.ordinal

            val longPtr = (ptr + 3 * sizeOf<IntVar>())!!.reinterpret<LongVar>()
            longPtr[0] = timestamp
        }
        data
    }

    val key: String = "token/${platform.ordinal}/$uid"
}