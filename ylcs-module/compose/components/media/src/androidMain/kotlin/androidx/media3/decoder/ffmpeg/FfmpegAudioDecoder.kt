package androidx.media3.decoder.ffmpeg

import androidx.media3.common.C
import androidx.media3.common.C.PcmEncoding
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.decoder.SimpleDecoder
import androidx.media3.decoder.SimpleDecoderOutputBuffer
import com.google.common.base.Preconditions
import java.nio.ByteBuffer
import kotlin.concurrent.Volatile

@UnstableApi
class FfmpegAudioDecoder(
    format: Format,
    numInputBuffers: Int,
    numOutputBuffers: Int,
    initialInputBufferSize: Int,
    outputFloat: Boolean
) : SimpleDecoder<DecoderInputBuffer?, SimpleDecoderOutputBuffer?, FfmpegDecoderException?>(
    arrayOfNulls(numInputBuffers), arrayOfNulls(numOutputBuffers)
) {
    private val codecName: String
    private val extraData: ByteArray?

    /** Returns the encoding of output audio.  */
    val encoding: @PcmEncoding Int
    private var outputBufferSize: Int

    private var nativeContext: Long // May be reassigned on resetting the codec.
    private var hasOutputFormat = false

    /** Returns the channel count of output audio.  */
    @Volatile
    var channelCount: Int = 0
        private set

    /** Returns the sample rate of output audio.  */
    @Volatile
    var sampleRate: Int = 0
        private set

    init {
        if (!FfmpegLibrary.isAvailable) throw FfmpegDecoderException("Failed to load decoder native libraries.")
        val mimeType = format.sampleMimeType!!
        codecName = requireNotNull(FfmpegLibrary.getCodecName(mimeType))
        extraData = getExtraData(mimeType, format.initializationData)
        encoding = if (outputFloat) C.ENCODING_PCM_FLOAT else C.ENCODING_PCM_16BIT
        outputBufferSize = if (outputFloat) INITIAL_OUTPUT_BUFFER_SIZE_32BIT else INITIAL_OUTPUT_BUFFER_SIZE_16BIT
        nativeContext = ffmpegInitialize(codecName, extraData, outputFloat, format.sampleRate, format.channelCount)
        if (nativeContext == 0L) throw FfmpegDecoderException("Initialization failed.")
        setInitialInputBufferSize(initialInputBufferSize)
    }

    override fun getName(): String = "ffmpeg${FfmpegLibrary.version}-$codecName"

    override fun createInputBuffer(): DecoderInputBuffer = DecoderInputBuffer(
        DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT,
        FfmpegLibrary.inputBufferPaddingSize
    )

    override fun createOutputBuffer(): SimpleDecoderOutputBuffer = SimpleDecoderOutputBuffer(::releaseOutputBuffer)

    override fun createUnexpectedDecodeException(error: Throwable): FfmpegDecoderException = FfmpegDecoderException("Unexpected decode error", error)

    override fun decode(inputBuffer: DecoderInputBuffer, outputBuffer: SimpleDecoderOutputBuffer, reset: Boolean): FfmpegDecoderException? {
        if (reset) {
            nativeContext = ffmpegReset(nativeContext, extraData)
            if (nativeContext == 0L) return FfmpegDecoderException("Error resetting (see logcat).")
        }
        val inputData = inputBuffer.data!!
        val inputSize = inputData.limit()
        var outputData = outputBuffer.init(inputBuffer.timeUs, outputBufferSize)
        val result = ffmpegDecode(nativeContext, inputData, inputSize, outputBuffer, outputData, outputBufferSize)
        when {
            result == AUDIO_DECODER_ERROR_OTHER -> return FfmpegDecoderException("Error decoding (see logcat).")
            result == AUDIO_DECODER_ERROR_INVALID_DATA -> {
                // Treat invalid data errors as non-fatal to match the behavior of MediaCodec. No output will
                // be produced for this buffer, so mark it as skipped to ensure that the audio sink's
                // position is reset when more audio is produced.
                outputBuffer.shouldBeSkipped = true
                return null
            }
            result == 0 -> {
                // There's no need to output empty buffers.
                outputBuffer.shouldBeSkipped = true
                return null
            }
            !hasOutputFormat -> {
                channelCount = ffmpegGetChannelCount(nativeContext)
                sampleRate = ffmpegGetSampleRate(nativeContext)
                if (sampleRate == 0 && "alac" == codecName) {
                    Preconditions.checkNotNull<ByteArray?>(extraData)
                    // ALAC decoder did not set the sample rate in earlier versions of FFmpeg. See
                    // https://trac.ffmpeg.org/ticket/6096.
                    val parsableExtraData = ParsableByteArray(extraData!!)
                    parsableExtraData.setPosition(extraData.size - 4)
                    sampleRate = parsableExtraData.readUnsignedIntToInt()
                }
                hasOutputFormat = true
            }
        }
        // Get a new reference to the output ByteBuffer in case the native decode method reallocated the
        // buffer to grow its size.
        outputData = requireNotNull(outputBuffer.data)
        outputData.position(0)
        outputData.limit(result)
        return null
    }

    // Called from native code
    @Suppress("unused")
    private fun growOutputBuffer(outputBuffer: SimpleDecoderOutputBuffer, requiredSize: Int): ByteBuffer {
        // Use it for new buffer so that hopefully we won't need to reallocate again
        outputBufferSize = requiredSize
        return outputBuffer.grow(requiredSize)
    }

    override fun release() {
        super.release()
        ffmpegRelease(nativeContext)
        nativeContext = 0
    }

    private external fun ffmpegInitialize(
        codecName: String?,
        extraData: ByteArray?,
        outputFloat: Boolean,
        rawSampleRate: Int,
        rawChannelCount: Int
    ): Long

    private external fun ffmpegDecode(
        context: Long,
        inputData: ByteBuffer?,
        inputSize: Int,
        decoderOutputBuffer: SimpleDecoderOutputBuffer?,
        outputData: ByteBuffer?,
        outputSize: Int
    ): Int

    private external fun ffmpegGetChannelCount(context: Long): Int

    private external fun ffmpegGetSampleRate(context: Long): Int

    private external fun ffmpegReset(context: Long, extraData: ByteArray?): Long

    private external fun ffmpegRelease(context: Long)

    companion object {
        private const val INITIAL_OUTPUT_BUFFER_SIZE_16BIT = 65535
        private const val INITIAL_OUTPUT_BUFFER_SIZE_32BIT = INITIAL_OUTPUT_BUFFER_SIZE_16BIT * 2

        private const val AUDIO_DECODER_ERROR_INVALID_DATA = -1
        private const val AUDIO_DECODER_ERROR_OTHER = -2

        // FLAC parsing constants
        private val flacStreamMarker =
            byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte())
        private const val FLAC_METADATA_TYPE_STREAM_INFO = 0
        private const val FLAC_METADATA_BLOCK_HEADER_SIZE = 4
        private const val FLAC_STREAM_INFO_DATA_SIZE = 34

        /**
         * Returns FFmpeg-compatible codec-specific initialization data ("extra data"), or `null` if
         * not required.
         */
        private fun getExtraData(mimeType: String, initializationData: MutableList<ByteArray>): ByteArray? = when (mimeType) {
            MimeTypes.AUDIO_AAC, MimeTypes.AUDIO_OPUS -> initializationData[0]
            MimeTypes.AUDIO_ALAC -> getAlacExtraData(initializationData)
            MimeTypes.AUDIO_VORBIS -> getVorbisExtraData(initializationData)
            MimeTypes.AUDIO_FLAC -> getFlacExtraData(initializationData)
            else -> null // Other codecs do not require extra data.
        }

        private fun getAlacExtraData(initializationData: MutableList<ByteArray>): ByteArray {
            // FFmpeg's ALAC decoder expects an ALAC atom, which contains the ALAC "magic cookie", as extra
            // data. initializationData[0] contains only the magic cookie, and so we need to package it into
            // an ALAC atom. See:
            // https://ffmpeg.org/doxygen/0.6/alac_8c.html
            // https://github.com/macosforge/alac/blob/master/ALACMagicCookieDescription.txt
            val magicCookie = initializationData[0]
            val alacAtomLength = 12 + magicCookie.size
            val alacAtom = ByteBuffer.allocate(alacAtomLength)
            alacAtom.putInt(alacAtomLength)
            alacAtom.putInt(0x616c6163) // type=alac
            alacAtom.putInt(0) // version=0, flags=0
            alacAtom.put(magicCookie,  /* offset= */0, magicCookie.size)
            return alacAtom.array()
        }

        private fun getVorbisExtraData(initializationData: MutableList<ByteArray>): ByteArray {
            val header0 = initializationData[0]
            val header1 = initializationData[1]
            val extraData = ByteArray(header0.size + header1.size + 6)
            extraData[0] = (header0.size shr 8).toByte()
            extraData[1] = (header0.size and 0xFF).toByte()
            System.arraycopy(header0, 0, extraData, 2, header0.size)
            extraData[header0.size + 2] = 0
            extraData[header0.size + 3] = 0
            extraData[header0.size + 4] = (header1.size shr 8).toByte()
            extraData[header0.size + 5] = (header1.size and 0xFF).toByte()
            System.arraycopy(header1, 0, extraData, header0.size + 6, header1.size)
            return extraData
        }

        private fun getFlacExtraData(initializationData: MutableList<ByteArray>): ByteArray? {
            for (i in initializationData.indices) {
                val out: ByteArray? = extractFlacStreamInfo(initializationData[i])
                if (out != null) return out
            }
            return null
        }

        private fun extractFlacStreamInfo(data: ByteArray): ByteArray? {
            var offset = 0
            if (arrayStartsWith(data, flacStreamMarker)) offset = flacStreamMarker.size

            if (data.size - offset == FLAC_STREAM_INFO_DATA_SIZE) {
                val streamInfo = ByteArray(FLAC_STREAM_INFO_DATA_SIZE)
                System.arraycopy(data, offset, streamInfo, 0, FLAC_STREAM_INFO_DATA_SIZE)
                return streamInfo
            }

            if (data.size >= offset + FLAC_METADATA_BLOCK_HEADER_SIZE) {
                val type = data[offset].toInt() and 0x7F
                val length = (((data[offset + 1].toInt() and 0xFF) shl 16)
                        or ((data[offset + 2].toInt() and 0xFF) shl 8)
                        or (data[offset + 3].toInt() and 0xFF))

                if (type == FLAC_METADATA_TYPE_STREAM_INFO && length == FLAC_STREAM_INFO_DATA_SIZE && data.size >= offset + FLAC_METADATA_BLOCK_HEADER_SIZE + FLAC_STREAM_INFO_DATA_SIZE) {
                    val streamInfo = ByteArray(FLAC_STREAM_INFO_DATA_SIZE)
                    System.arraycopy(
                        data,
                        offset + FLAC_METADATA_BLOCK_HEADER_SIZE,
                        streamInfo,
                        0,
                        FLAC_STREAM_INFO_DATA_SIZE
                    )
                    return streamInfo
                }
            }

            return null
        }

        private fun arrayStartsWith(data: ByteArray, prefix: ByteArray): Boolean {
            if (data.size < prefix.size) return false
            for (i in prefix.indices) {
                if (data[i] != prefix[i]) return false
            }
            return true
        }
    }
}
