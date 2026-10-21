package love.yinlin.compose.image.cache

import kotlinx.io.readByteArray
import love.yinlin.compose.cache.DiskPolicy
import love.yinlin.compose.cache.FileDiskStorage
import love.yinlin.compose.image.data.ImageInputData
import love.yinlin.compose.image.data.ImageOrigin
import love.yinlin.extension.catchingNull
import love.yinlin.fs.File
import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

internal class ImageDiskCache(
    directory: File,
    maxBytes: Long,
    private val maxSourceBytes: Int
) {
    companion object {
        const val MAGIC = 0x19911211

        private fun imageCacheMix(value: Int): Int {
            var result = value
            result = (result xor (result ushr 16)) * 0x85ebca6b.toInt()
            result = (result xor (result ushr 13)) * 0xc2b2ae35.toInt()
            return result xor (result ushr 16)
        }

        fun imageCacheHash(value: String): String = imageCacheHash(value.encodeToByteArray())

        fun imageCacheHash(value: ByteArray): String {
            val c1 = 0x239b961b
            val c2 = 0xab0e9789.toInt()
            val c3 = 0x38b34ae5
            val c4 = 0xa1e38b93.toInt()
            var h1 = 0
            var h2 = 0
            var h3 = 0
            var h4 = 0
            val input = ByteArrayIO(value)
            val blockEnd = value.size / 16 * 16

            for (index in 0 ..< blockEnd step 16) {
                h1 = h1 xor ((input.readInt(index, Endian.LITTLE) * c1).rotateLeft(15) * c2)
                h1 = (h1.rotateLeft(19) + h2) * 5 + 0x561ccd1b

                h2 = h2 xor ((input.readInt(index + 4, Endian.LITTLE) * c2).rotateLeft(16) * c3)
                h2 = (h2.rotateLeft(17) + h3) * 5 + 0x0bcaa747

                h3 = h3 xor ((input.readInt(index + 8, Endian.LITTLE) * c3).rotateLeft(17) * c4)
                h3 = (h3.rotateLeft(15) + h4) * 5 + 0x96cd1c35.toInt()

                h4 = h4 xor ((input.readInt(index + 12, Endian.LITTLE) * c4).rotateLeft(18) * c1)
                h4 = (h4.rotateLeft(13) + h1) * 5 + 0x32ac3b17
            }

            val tail = IntArray(4)
            for (index in blockEnd ..< value.size) {
                val offset = index - blockEnd
                tail[offset / 4] = tail[offset / 4] xor ((value[index].toInt() and 0xFF) shl (offset % 4 * 8))
            }

            val remaining = value.size - blockEnd
            if (remaining > 12) h4 = h4 xor ((tail[3] * c4).rotateLeft(18) * c1)
            if (remaining > 8) h3 = h3 xor ((tail[2] * c3).rotateLeft(17) * c4)
            if (remaining > 4) h2 = h2 xor ((tail[1] * c2).rotateLeft(16) * c3)
            if (remaining > 0) h1 = h1 xor ((tail[0] * c1).rotateLeft(15) * c2)

            h1 = h1 xor value.size
            h2 = h2 xor value.size
            h3 = h3 xor value.size
            h4 = h4 xor value.size
            h1 += h2 + h3 + h4
            h2 += h1
            h3 += h1
            h4 += h1

            h1 = imageCacheMix(h1)
            h2 = imageCacheMix(h2)
            h3 = imageCacheMix(h3)
            h4 = imageCacheMix(h4)
            h1 += h2 + h3 + h4
            h2 += h1
            h3 += h1

            return ByteArrayIO(12).write {
                writeInt(0, h1, Endian.LITTLE)
                writeInt(4, h2, Endian.LITTLE)
                writeInt(8, h3, Endian.LITTLE)
            }.toHexString()
        }
    }

    private val storage = FileDiskStorage(File(directory, "rachel-image"), DiskPolicy.Lru(maxBytes))

    suspend fun read(key: String): ImageInputData? {
        val fileKey = imageCacheHash(key)
        val data = catchingNull {
            storage.read(fileKey) { source ->
                val header = ByteArrayIO(source.readByteArray(8))
                require(header.readInt(0) == MAGIC)
                val dataSize = header.readInt(4)
                require(dataSize in 1 .. maxSourceBytes)
                val bytes = source.readByteArray(dataSize)
                require(source.exhausted())

                ImageInputData(bytes, ImageOrigin.Disk)
            }
        }

        if (data == null) storage.remove(fileKey)
        return data
    }

    suspend fun write(key: String, data: ImageInputData) {
        require(data.bytes.size in 1 .. maxSourceBytes)

        storage.write(imageCacheHash(key)) { sink ->
            val header = ByteArrayIO(8).write {
                writeInt(0, MAGIC)
                writeInt(4, data.bytes.size)
            }
            sink.write(header)
            sink.write(data.bytes)
        }
    }

    suspend fun clear() = storage.clear()
}
