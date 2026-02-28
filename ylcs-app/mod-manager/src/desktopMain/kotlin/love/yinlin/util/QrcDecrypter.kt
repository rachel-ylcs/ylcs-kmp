package love.yinlin.util

import androidx.compose.runtime.Stable
import love.yinlin.extension.catchingNull
import java.nio.charset.StandardCharsets
import java.util.HexFormat
import java.util.Locale.getDefault
import java.util.zip.Inflater
import kotlin.math.min

@Stable
object QrcDecrypter {
    private const val XOR_KEY = "629F5B0900C35E95239F13117ED8923FBC90BB740EC347743D90AA3F51D8F411849FDE951DC3C609D59FFA66F9D8F0F7A090A1D6F3C3F3D6A190A0F7F0D8F966FA9FD509C6C31D95DE9F8411F4D8513FAA903D7447C30E74BB90BC3F92D87E11139F23955EC300095B9F6266A1D852F76790CAD64AC34AD6CA9067F752D8A166"
    private val KEY1 = "!@#)(NHLiuy*$%^&".toByteArray(StandardCharsets.UTF_8)
    private val KEY2 = "123ZXC!@#)(*$%^&".toByteArray(StandardCharsets.UTF_8)
    private val KEY3 = "!@#)(*$%^&abcDEF".toByteArray(StandardCharsets.UTF_8)
    private const val ENCRYPT_MODE = 1
    private const val DECRYPT_MODE = 0
    private val sboxes = arrayOf(
        intArrayOf(
            14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7,
            0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8,
            4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0,
            15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13
        ),
        intArrayOf(
            15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10,
            3, 13, 4, 7, 15, 2, 8, 15, 12, 0, 1, 10, 6, 9, 11, 5,
            0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15,
            13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9
        ),
        intArrayOf(
            10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8,
            13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1,
            13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7,
            1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12
        ),
        intArrayOf(
            7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15,
            13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9,
            10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4,
            3, 15, 0, 6, 10, 10, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14
        ),
        intArrayOf(
            2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9,
            14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6,
            4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14,
            11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3
        ),
        intArrayOf(
            12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11,
            10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8,
            9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6,
            4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13
        ),
        intArrayOf(
            4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1,
            13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6,
            1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2,
            6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12
        ),
        intArrayOf(
            13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7,
            1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2,
            7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8,
            2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11
        )
    )

    private fun extractBitFromBytes(dataBytes: ByteArray, bitPosition: Int, shiftAmount: Int): Int {
        val byteIndex = (bitPosition / 32) * 4 + 3 - (bitPosition % 32) / 8
        val bitInByte = 7 - (bitPosition % 8)
        return ((dataBytes[byteIndex].toInt() shr bitInByte) and 0x01) shl shiftAmount
    }

    private fun extractBitFromInt(dataInt: Int, bitPosition: Int, shiftAmount: Int): Int {
        return ((dataInt shr (31 - bitPosition)) and 0x00000001) shl shiftAmount
    }

    private fun extractBitLeftShift(dataInt: Int, bitPosition: Int, shiftAmount: Int): Int {
        return ((dataInt shl bitPosition) and -0x80000000) ushr shiftAmount
    }

    private fun prepareSboxIndex(inputByte: Int): Int {
        return (inputByte and 0x20) or ((inputByte and 0x1f) shr 1) or ((inputByte and 0x01) shl 4)
    }

    private fun initialPermutation(state: IntArray, inputData: ByteArray) {
        state[0] = (extractBitFromBytes(inputData, 57, 31) or
                extractBitFromBytes(inputData, 49, 30) or
                extractBitFromBytes(inputData, 41, 29) or
                extractBitFromBytes(inputData, 33, 28) or
                extractBitFromBytes(inputData, 25, 27) or
                extractBitFromBytes(inputData, 17, 26) or
                extractBitFromBytes(inputData, 9, 25) or
                extractBitFromBytes(inputData, 1, 24) or
                extractBitFromBytes(inputData, 59, 23) or
                extractBitFromBytes(inputData, 51, 22) or
                extractBitFromBytes(inputData, 43, 21) or
                extractBitFromBytes(inputData, 35, 20) or
                extractBitFromBytes(inputData, 27, 19) or
                extractBitFromBytes(inputData, 19, 18) or
                extractBitFromBytes(inputData, 11, 17) or
                extractBitFromBytes(inputData, 3, 16) or
                extractBitFromBytes(inputData, 61, 15) or
                extractBitFromBytes(inputData, 53, 14) or
                extractBitFromBytes(inputData, 45, 13) or
                extractBitFromBytes(inputData, 37, 12) or
                extractBitFromBytes(inputData, 29, 11) or
                extractBitFromBytes(inputData, 21, 10) or
                extractBitFromBytes(inputData, 13, 9) or
                extractBitFromBytes(inputData, 5, 8) or
                extractBitFromBytes(inputData, 63, 7) or
                extractBitFromBytes(inputData, 55, 6) or
                extractBitFromBytes(inputData, 47, 5) or
                extractBitFromBytes(inputData, 39, 4) or
                extractBitFromBytes(inputData, 31, 3) or
                extractBitFromBytes(inputData, 23, 2) or
                extractBitFromBytes(inputData, 15, 1) or
                extractBitFromBytes(inputData, 7, 0)
                )
        state[1] = (extractBitFromBytes(inputData, 56, 31) or
                extractBitFromBytes(inputData, 48, 30) or
                extractBitFromBytes(inputData, 40, 29) or
                extractBitFromBytes(inputData, 32, 28) or
                extractBitFromBytes(inputData, 24, 27) or
                extractBitFromBytes(inputData, 16, 26) or
                extractBitFromBytes(inputData, 8, 25) or
                extractBitFromBytes(inputData, 0, 24) or
                extractBitFromBytes(inputData, 58, 23) or
                extractBitFromBytes(inputData, 50, 22) or
                extractBitFromBytes(inputData, 42, 21) or
                extractBitFromBytes(inputData, 34, 20) or
                extractBitFromBytes(inputData, 26, 19) or
                extractBitFromBytes(inputData, 18, 18) or
                extractBitFromBytes(inputData, 10, 17) or
                extractBitFromBytes(inputData, 2, 16) or
                extractBitFromBytes(inputData, 60, 15) or
                extractBitFromBytes(inputData, 52, 14) or
                extractBitFromBytes(inputData, 44, 13) or
                extractBitFromBytes(inputData, 36, 12) or
                extractBitFromBytes(inputData, 28, 11) or
                extractBitFromBytes(inputData, 20, 10) or
                extractBitFromBytes(inputData, 12, 9) or
                extractBitFromBytes(inputData, 4, 8) or
                extractBitFromBytes(inputData, 62, 7) or
                extractBitFromBytes(inputData, 54, 6) or
                extractBitFromBytes(inputData, 46, 5) or
                extractBitFromBytes(inputData, 38, 4) or
                extractBitFromBytes(inputData, 30, 3) or
                extractBitFromBytes(inputData, 22, 2) or
                extractBitFromBytes(inputData, 14, 1) or
                extractBitFromBytes(inputData, 6, 0)
                )
    }

    private fun inversePermutation(state: IntArray, output: ByteArray) {
        output[3] = (extractBitFromInt(state[1], 7, 7) or
                extractBitFromInt(state[0], 7, 6) or
                extractBitFromInt(state[1], 15, 5) or
                extractBitFromInt(state[0], 15, 4) or
                extractBitFromInt(state[1], 23, 3) or
                extractBitFromInt(state[0], 23, 2) or
                extractBitFromInt(state[1], 31, 1) or
                extractBitFromInt(state[0], 31, 0)
                ).toByte()

        output[2] = (extractBitFromInt(state[1], 6, 7) or
                extractBitFromInt(state[0], 6, 6) or
                extractBitFromInt(state[1], 14, 5) or
                extractBitFromInt(state[0], 14, 4) or
                extractBitFromInt(state[1], 22, 3) or
                extractBitFromInt(state[0], 22, 2) or
                extractBitFromInt(state[1], 30, 1) or
                extractBitFromInt(state[0], 30, 0)
                ).toByte()

        output[1] = (extractBitFromInt(state[1], 5, 7) or
                extractBitFromInt(state[0], 5, 6) or
                extractBitFromInt(state[1], 13, 5) or
                extractBitFromInt(state[0], 13, 4) or
                extractBitFromInt(state[1], 21, 3) or
                extractBitFromInt(state[0], 21, 2) or
                extractBitFromInt(state[1], 29, 1) or
                extractBitFromInt(state[0], 29, 0)
                ).toByte()

        output[0] = (extractBitFromInt(state[1], 4, 7) or
                extractBitFromInt(state[0], 4, 6) or
                extractBitFromInt(state[1], 12, 5) or
                extractBitFromInt(state[0], 12, 4) or
                extractBitFromInt(state[1], 20, 3) or
                extractBitFromInt(state[0], 20, 2) or
                extractBitFromInt(state[1], 28, 1) or
                extractBitFromInt(state[0], 28, 0)
                ).toByte()

        output[7] = (extractBitFromInt(state[1], 3, 7) or
                extractBitFromInt(state[0], 3, 6) or
                extractBitFromInt(state[1], 11, 5) or
                extractBitFromInt(state[0], 11, 4) or
                extractBitFromInt(state[1], 19, 3) or
                extractBitFromInt(state[0], 19, 2) or
                extractBitFromInt(state[1], 27, 1) or
                extractBitFromInt(state[0], 27, 0)
                ).toByte()

        output[6] = (extractBitFromInt(state[1], 2, 7) or
                extractBitFromInt(state[0], 2, 6) or
                extractBitFromInt(state[1], 10, 5) or
                extractBitFromInt(state[0], 10, 4) or
                extractBitFromInt(state[1], 18, 3) or
                extractBitFromInt(state[0], 18, 2) or
                extractBitFromInt(state[1], 26, 1) or
                extractBitFromInt(state[0], 26, 0)
                ).toByte()

        output[5] = (extractBitFromInt(state[1], 1, 7) or
                extractBitFromInt(state[0], 1, 6) or
                extractBitFromInt(state[1], 9, 5) or
                extractBitFromInt(state[0], 9, 4) or
                extractBitFromInt(state[1], 17, 3) or
                extractBitFromInt(state[0], 17, 2) or
                extractBitFromInt(state[1], 25, 1) or
                extractBitFromInt(state[0], 25, 0)
                ).toByte()

        output[4] = (extractBitFromInt(state[1], 0, 7) or
                extractBitFromInt(state[0], 0, 6) or
                extractBitFromInt(state[1], 8, 5) or
                extractBitFromInt(state[0], 8, 4) or
                extractBitFromInt(state[1], 16, 3) or
                extractBitFromInt(state[0], 16, 2) or
                extractBitFromInt(state[1], 24, 1) or
                extractBitFromInt(state[0], 24, 0)
                ).toByte()
    }

    private fun feistelFunction(state: Int, roundKey: ByteArray): Int {
        val expanded = IntArray(6)
        val t1 = (extractBitLeftShift(state, 31, 0) or
                ((state and -0x10000000) ushr 1) or
                extractBitLeftShift(state, 4, 5) or
                extractBitLeftShift(state, 3, 6) or
                ((state and 0x0f000000) ushr 3) or
                extractBitLeftShift(state, 8, 11) or
                extractBitLeftShift(state, 7, 12) or
                ((state and 0x00f00000) ushr 5) or
                extractBitLeftShift(state, 12, 17) or
                extractBitLeftShift(state, 11, 18) or
                ((state and 0x000f0000) ushr 7) or
                extractBitLeftShift(state, 16, 23)
                )
        val t2 = (extractBitLeftShift(state, 15, 0) or
                ((state and 0x0000f000) shl 15) or
                extractBitLeftShift(state, 20, 5) or
                extractBitLeftShift(state, 19, 6) or
                ((state and 0x00000f00) shl 13) or
                extractBitLeftShift(state, 24, 11) or
                extractBitLeftShift(state, 23, 12) or
                ((state and 0x000000f0) shl 11) or
                extractBitLeftShift(state, 28, 17) or
                extractBitLeftShift(state, 27, 18) or
                ((state and 0x0000000f) shl 9) or
                extractBitLeftShift(state, 0, 23)
                )
        expanded[0] = (t1 shr 24) and 0xFF
        expanded[1] = (t1 shr 16) and 0xFF
        expanded[2] = (t1 shr 8) and 0xFF
        expanded[3] = (t2 shr 24) and 0xFF
        expanded[4] = (t2 shr 16) and 0xFF
        expanded[5] = (t2 shr 8) and 0xFF
        for (i in 0..5) {
            expanded[i] = expanded[i] xor (roundKey[i].toInt() and 0xFF)
        }
        val sboxInputs = IntArray(8)
        sboxInputs[0] = (expanded[0] shr 2)
        sboxInputs[1] = ((expanded[0] and 0x03) shl 4) or (expanded[1] shr 4)
        sboxInputs[2] = ((expanded[1] and 0x0F) shl 2) or (expanded[2] shr 6)
        sboxInputs[3] = expanded[2] and 0x3F
        sboxInputs[4] = (expanded[3] shr 2)
        sboxInputs[5] = ((expanded[3] and 0x03) shl 4) or (expanded[4] shr 4)
        sboxInputs[6] = ((expanded[4] and 0x0F) shl 2) or (expanded[5] shr 6)
        sboxInputs[7] = expanded[5] and 0x3F
        var sboxOutput = 0
        sboxOutput = sboxOutput or (sboxes[0][prepareSboxIndex(sboxInputs[0])] shl 28)
        sboxOutput = sboxOutput or (sboxes[1][prepareSboxIndex(sboxInputs[1])] shl 24)
        sboxOutput = sboxOutput or (sboxes[2][prepareSboxIndex(sboxInputs[2])] shl 20)
        sboxOutput = sboxOutput or (sboxes[3][prepareSboxIndex(sboxInputs[3])] shl 16)
        sboxOutput = sboxOutput or (sboxes[4][prepareSboxIndex(sboxInputs[4])] shl 12)
        sboxOutput = sboxOutput or (sboxes[5][prepareSboxIndex(sboxInputs[5])] shl 8)
        sboxOutput = sboxOutput or (sboxes[6][prepareSboxIndex(sboxInputs[6])] shl 4)
        sboxOutput = sboxOutput or sboxes[7][prepareSboxIndex(sboxInputs[7])]
        return (extractBitLeftShift(sboxOutput, 15, 0) or
                extractBitLeftShift(sboxOutput, 6, 1) or
                extractBitLeftShift(sboxOutput, 19, 2) or
                extractBitLeftShift(sboxOutput, 20, 3) or
                extractBitLeftShift(sboxOutput, 28, 4) or
                extractBitLeftShift(sboxOutput, 11, 5) or
                extractBitLeftShift(sboxOutput, 27, 6) or
                extractBitLeftShift(sboxOutput, 16, 7) or
                extractBitLeftShift(sboxOutput, 0, 8) or
                extractBitLeftShift(sboxOutput, 14, 9) or
                extractBitLeftShift(sboxOutput, 22, 10) or
                extractBitLeftShift(sboxOutput, 25, 11) or
                extractBitLeftShift(sboxOutput, 4, 12) or
                extractBitLeftShift(sboxOutput, 17, 13) or
                extractBitLeftShift(sboxOutput, 30, 14) or
                extractBitLeftShift(sboxOutput, 9, 15) or
                extractBitLeftShift(sboxOutput, 1, 16) or
                extractBitLeftShift(sboxOutput, 7, 17) or
                extractBitLeftShift(sboxOutput, 23, 18) or
                extractBitLeftShift(sboxOutput, 13, 19) or
                extractBitLeftShift(sboxOutput, 31, 20) or
                extractBitLeftShift(sboxOutput, 26, 21) or
                extractBitLeftShift(sboxOutput, 2, 22) or
                extractBitLeftShift(sboxOutput, 8, 23) or
                extractBitLeftShift(sboxOutput, 18, 24) or
                extractBitLeftShift(sboxOutput, 12, 25) or
                extractBitLeftShift(sboxOutput, 29, 26) or
                extractBitLeftShift(sboxOutput, 5, 27) or
                extractBitLeftShift(sboxOutput, 21, 28) or
                extractBitLeftShift(sboxOutput, 10, 29) or
                extractBitLeftShift(sboxOutput, 3, 30) or
                extractBitLeftShift(sboxOutput, 24, 31)
                )
    }

    private fun generateKeySchedule(masterKey: ByteArray, keySchedule: Array<ByteArray>, mode: Int) {
        val keyRotation = intArrayOf(1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1)
        val keyPermC = intArrayOf(
            56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1,
            58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35
        )
        val keyPermD = intArrayOf(
            62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5,
            60, 52, 44, 36, 28, 20, 12, 4, 27, 19, 11, 3
        )
        val keyCompression = intArrayOf(
            13, 16, 10, 23, 0, 4, 2, 27, 14, 5, 20, 9, 22, 18, 11, 3,
            25, 7, 15, 6, 26, 19, 12, 1, 40, 51, 30, 36, 46, 54, 29, 39,
            50, 44, 32, 47, 43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31
        )
        var leftHalf = 0
        var rightHalf = 0
        var j = 31
        for (i in 0..27) {
            leftHalf = leftHalf or extractBitFromBytes(masterKey, keyPermC[i], j)
            j--
        }
        j = 31
        for (i in 0..27) {
            rightHalf = rightHalf or extractBitFromBytes(masterKey, keyPermD[i], j)
            j--
        }
        for (roundNum in 0..15) {
            val shift = keyRotation[roundNum]
            leftHalf = ((leftHalf shl shift) or (leftHalf ushr (28 - shift))) and -0x10
            rightHalf = ((rightHalf shl shift) or (rightHalf ushr (28 - shift))) and -0x10
            val scheduleIndex: Int = if (mode == DECRYPT_MODE) 15 - roundNum else roundNum
            for (k in 0..5) {
                keySchedule[scheduleIndex][k] = 0
            }
            for (k in 0..23) {
                keySchedule[scheduleIndex][k / 8] = (keySchedule[scheduleIndex][k / 8].toInt() or extractBitFromInt(
                    leftHalf, keyCompression[k], 7 - (k % 8)
                ).toByte().toInt()).toByte()
            }
            for (k in 24..47) {
                keySchedule[scheduleIndex][k / 8] = (keySchedule[scheduleIndex][k / 8].toInt() or extractBitFromInt(
                    rightHalf, keyCompression[k] - 27, 7 - (k % 8)
                ).toByte().toInt()).toByte()
            }
        }
    }

    private fun processDesBlock(inputBlock: ByteArray, keySchedule: Array<ByteArray>): ByteArray {
        val state = IntArray(2)
        initialPermutation(state, inputBlock)
        for (roundIdx in 0..14) {
            val temp = state[1]
            state[1] = feistelFunction(state[1], keySchedule[roundIdx]) xor state[0]
            state[0] = temp
        }
        state[0] = feistelFunction(state[1], keySchedule[15]) xor state[0]
        val outputBlock = ByteArray(8)
        inversePermutation(state, outputBlock)
        return outputBlock
    }

    private fun desEncrypt(data: ByteArray): ByteArray {
        val keySchedule = Array(16) { ByteArray(6) }
        generateKeySchedule(KEY2, keySchedule, ENCRYPT_MODE)
        return processInBlocks(data, keySchedule)
    }

    private fun desDecrypt(data: ByteArray, key: ByteArray): ByteArray {
        val keySchedule = Array(16) { ByteArray(6) }
        generateKeySchedule(key, keySchedule, DECRYPT_MODE)
        return processInBlocks(data, keySchedule)
    }

    private fun processInBlocks(data: ByteArray, keySchedule: Array<ByteArray>): ByteArray {
        val length = data.size
        val result = ByteArray(length)
        var i = 0
        while (i < length) {
            val blockSize = min(8, length - i)
            var inputBlock = data.copyOfRange(i, i + blockSize)
            if (inputBlock.size < 8) inputBlock = inputBlock.copyOf(8)
            val outputBlock = processDesBlock(inputBlock, keySchedule)
            System.arraycopy(outputBlock, 0, result, i, min(8, length - i))
            i += 8
        }
        return result
    }

    private fun hexToBytes(hexString: String): ByteArray {
        var hexString = hexString
        if (hexString.length % 2 != 0) hexString = "0$hexString"
        return HexFormat.of().parseHex(hexString)
    }

    private fun bytesToHex(byteData: ByteArray): String {
        return HexFormat.of().formatHex(byteData).uppercase(getDefault())
    }

    private fun xorHexStrings(hexStr1: String): String {
        val bytes1 = hexToBytes(hexStr1)
        val bytes2 = hexToBytes(XOR_KEY)
        val result = ByteArray(bytes1.size)
        for (i in bytes1.indices) {
            val keyIndex = i % bytes2.size
            result[i] = (bytes1[i].toInt() xor bytes2[keyIndex].toInt()).toByte()
        }
        return bytesToHex(result)
    }

    private fun decryptQrc(hexString: String): String {
        val encryptedData: ByteArray = hexToBytes(hexString)
        val step1 = desDecrypt(encryptedData, KEY1)
        val step2 = desEncrypt(step1)
        val step3 = desDecrypt(step2, KEY3)
        val inflater = Inflater()
        inflater.setInput(step3)
        val decompressed = ByteArray(step3.size * 4)
        val decompressedLength = inflater.inflate(decompressed)
        inflater.end()
        return String(decompressed, 0, decompressedLength, StandardCharsets.UTF_8)
    }

    private fun decryptByQrcHexContent(hexContent: String): String {
        val processedHex = if (hexContent.startsWith("9825B0ACE3028368E8FC6C")) hexContent.substring(22) else hexContent
        var decryptQrc = decryptQrc(xorHexStrings(processedHex))
        if (decryptQrc.contains("<QrcInfos>") && !decryptQrc.contains("</QrcInfos>")) {
            decryptQrc += "\n\"/>\n</LyricInfo>\n</QrcInfos>"
        }
        return decryptQrc
    }

    fun decrypt(data: ByteArray): String? = catchingNull { decryptByQrcHexContent(bytesToHex(data)) }
}