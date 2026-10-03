package love.yinlin.cs.user

import kotlinx.cinterop.*
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingNull
import love.yinlin.extension.enumMap
import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian
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

        fun fromBytes(bytes: ByteArray): Token? = catchingNull {
            val io = ByteArrayIO(bytes)
            val uid = io.readInt(0, Endian.LITTLE)
            val magic = io.readInt(4, Endian.LITTLE)
            val platform = io.readEnum<Platform>(8, Endian.LITTLE)
            val timestamp = io.readLong(12, Endian.LITTLE)
            require(uid > 0 && magic == MAGIC && timestamp in 1727755860000..1917058260000)
            return Token(uid = uid, platform = platform, timestamp = timestamp)
        }

        fun keys(uid: Int): List<String> = enumMap { platform: Platform -> "token/${platform.ordinal}/$uid" }
    }

    val bytes: ByteArray by lazy {
        ByteArrayIO((sizeOf<IntVar>() * 3 + sizeOf<LongVar>()).toInt()).write {
            writeInt(0, uid, Endian.LITTLE)
            writeInt(4, 19911211, Endian.LITTLE)
            writeEnum(8, platform, Endian.LITTLE)
            writeLong(12, timestamp, Endian.LITTLE)
        }
    }

    val key: String = "token/${platform.ordinal}/$uid"
}