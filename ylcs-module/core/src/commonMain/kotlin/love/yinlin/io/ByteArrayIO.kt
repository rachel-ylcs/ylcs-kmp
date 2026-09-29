package love.yinlin.io

import kotlin.enums.enumEntries

class ByteArrayIO(val bytes: ByteArray) {
    constructor(size: Int) : this(ByteArray(size))

    val size: Int get() = bytes.size

    fun readByte(index: Int): Byte = bytes[index]

    fun readUByte(index: Int): UByte = readByte(index).toUByte()

    fun readBoolean(index: Int): Boolean = readByte(index).toInt() != 0

    fun readShort(index: Int, endian: Endian = Endian.BIG): Short {
        val b0 = bytes[index].toInt() and 0xFF
        val b1 = bytes[index + 1].toInt() and 0xFF
        val value = if (endian == Endian.BIG) (b0 shl 8) or b1 else (b1 shl 8) or b0
        return value.toShort()
    }

    fun readUShort(index: Int, endian: Endian = Endian.BIG): UShort = readShort(index, endian).toUShort()

    fun readChar(index: Int, endian: Endian = Endian.BIG): Char = readUShort(index, endian).toInt().toChar()

    fun readInt(index: Int, endian: Endian = Endian.BIG): Int {
        val b0 = bytes[index].toInt() and 0xFF
        val b1 = bytes[index + 1].toInt() and 0xFF
        val b2 = bytes[index + 2].toInt() and 0xFF
        val b3 = bytes[index + 3].toInt() and 0xFF
        return if (endian == Endian.BIG) (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3 else (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
    }

    fun readUInt(index: Int, endian: Endian = Endian.BIG): UInt = readInt(index, endian).toUInt()

    inline fun <reified E : Enum<E>> readEnum(index: Int, endian: Endian = Endian.BIG): E = enumEntries<E>()[readInt(index, endian)]

    fun readLong(index: Int, endian: Endian = Endian.BIG): Long {
        val b0 = bytes[index].toLong() and 0xFFL
        val b1 = bytes[index + 1].toLong() and 0xFFL
        val b2 = bytes[index + 2].toLong() and 0xFFL
        val b3 = bytes[index + 3].toLong() and 0xFFL
        val b4 = bytes[index + 4].toLong() and 0xFFL
        val b5 = bytes[index + 5].toLong() and 0xFFL
        val b6 = bytes[index + 6].toLong() and 0xFFL
        val b7 = bytes[index + 7].toLong() and 0xFFL
        return if (endian == Endian.BIG) (b0 shl 56) or (b1 shl 48) or (b2 shl 40) or (b3 shl 32) or (b4 shl 24) or (b5 shl 16) or (b6 shl 8) or b7 else (b7 shl 56) or (b6 shl 48) or (b5 shl 40) or (b4 shl 32) or (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
    }

    fun readULong(index: Int, endian: Endian = Endian.BIG): ULong = readLong(index, endian).toULong()

    fun readFloat(index: Int): Float = Float.fromBits(readInt(index))

    fun readDouble(index: Int): Double = Double.fromBits(readLong(index))

    fun readString(index: Int, length: Int): String = bytes.decodeToString(startIndex = index, endIndex = index + length)

    fun readByteArray(index: Int, length: Int): ByteArray = bytes.copyOfRange(index, index + length)

    fun writeByte(index: Int, value: Byte) { bytes[index] = value }

    fun writeUByte(index: Int, value: UByte) = writeByte(index, value.toByte())

    fun writeBoolean(index: Int, value: Boolean) = writeByte(index, (if (value) 1 else 0).toByte())

    fun writeShort(index: Int, value: Short, endian: Endian = Endian.BIG) {
        val bits = value.toInt()
        if (endian == Endian.BIG) {
            bytes[index] = (bits ushr 8).toByte()
            bytes[index + 1] = bits.toByte()
        } else {
            bytes[index] = bits.toByte()
            bytes[index + 1] = (bits ushr 8).toByte()
        }
    }

    fun writeUShort(index: Int, value: UShort, endian: Endian = Endian.BIG) = writeShort(index, value.toShort(), endian)

    fun writeChar(index: Int, value: Char, endian: Endian = Endian.BIG) = writeShort(index, value.code.toShort(), endian)

    fun writeInt(index: Int, value: Int, endian: Endian = Endian.BIG) {
        if (endian == Endian.BIG) {
            bytes[index] = (value ushr 24).toByte()
            bytes[index + 1] = (value ushr 16).toByte()
            bytes[index + 2] = (value ushr 8).toByte()
            bytes[index + 3] = value.toByte()
        } else {
            bytes[index] = value.toByte()
            bytes[index + 1] = (value ushr 8).toByte()
            bytes[index + 2] = (value ushr 16).toByte()
            bytes[index + 3] = (value ushr 24).toByte()
        }
    }

    fun writeUInt(index: Int, value: UInt, endian: Endian = Endian.BIG) = writeInt(index, value.toInt(), endian)

    inline fun <reified E : Enum<E>> writeEnum(index: Int, value: E, endian: Endian = Endian.BIG) = writeInt(index, value.ordinal, endian)

    fun writeLong(index: Int, value: Long, endian: Endian = Endian.BIG) {
        if (endian == Endian.BIG) {
            bytes[index] = (value ushr 56).toByte()
            bytes[index + 1] = (value ushr 48).toByte()
            bytes[index + 2] = (value ushr 40).toByte()
            bytes[index + 3] = (value ushr 32).toByte()
            bytes[index + 4] = (value ushr 24).toByte()
            bytes[index + 5] = (value ushr 16).toByte()
            bytes[index + 6] = (value ushr 8).toByte()
            bytes[index + 7] = value.toByte()
        } else {
            bytes[index] = value.toByte()
            bytes[index + 1] = (value ushr 8).toByte()
            bytes[index + 2] = (value ushr 16).toByte()
            bytes[index + 3] = (value ushr 24).toByte()
            bytes[index + 4] = (value ushr 32).toByte()
            bytes[index + 5] = (value ushr 40).toByte()
            bytes[index + 6] = (value ushr 48).toByte()
            bytes[index + 7] = (value ushr 56).toByte()
        }
    }

    fun writeULong(index: Int, value: ULong, endian: Endian = Endian.BIG) = writeLong(index, value.toLong(), endian)

    fun writeFloat(index: Int, value: Float) = writeInt(index, value.toRawBits())

    fun writeDouble(index: Int, value: Double) = writeLong(index, value.toRawBits())

    fun writeString(index: Int, value: String) { value.encodeToByteArray().copyInto(bytes, destinationOffset = index) }

    fun writeByteArray(index: Int, value: ByteArray) { value.copyInto(bytes, destinationOffset = index) }

    inline fun write(block: ByteArrayIO.() -> Unit): ByteArray {
        block()
        return bytes
    }
}