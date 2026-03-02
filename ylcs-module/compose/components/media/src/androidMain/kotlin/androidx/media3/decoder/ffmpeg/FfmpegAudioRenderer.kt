package androidx.media3.decoder.ffmpeg

import android.content.Context
import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.C.PcmEncoding
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.TraceUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.decoder.CryptoConfig
import androidx.media3.exoplayer.RendererCapabilities.AdaptiveSupport
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.AudioSink.SinkFormatSupport
import androidx.media3.exoplayer.audio.DecoderAudioRenderer
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * Creates a new instance.
 *
 * @param eventHandler A handler to use when delivering events to `eventListener`. May be
 * null if delivery of events is not required.
 * @param eventListener A listener of events. May be null if delivery of events is not required.
 * @param audioSink The sink to which audio will be output.
 */
@UnstableApi
class FfmpegAudioRenderer(
    eventHandler: Handler?,
    eventListener: AudioRendererEventListener?,
    audioSink: AudioSink
) : DecoderAudioRenderer<FfmpegAudioDecoder?>(eventHandler, eventListener, audioSink) {
    constructor(context: Context) : this(context, /* eventHandler= */null,  /* eventListener= */null)

    /**
     * Creates a new instance.
     *
     * @param eventHandler A handler to use when delivering events to `eventListener`. May be
     * null if delivery of events is not required.
     * @param eventListener A listener of events. May be null if delivery of events is not required.
     * @param audioProcessors Optional [AudioProcessor]s that will process audio before output.
     */
    constructor(
        context: Context,
        eventHandler: Handler?,
        eventListener: AudioRendererEventListener?,
        vararg audioProcessors: AudioProcessor
    ) : this(eventHandler, eventListener, DefaultAudioSink.Builder(context).setAudioProcessors(audioProcessors).build())

    override fun getName(): String = TAG

    @C.FormatSupport
    override fun supportsFormatInternal(format: Format): @C.FormatSupport Int {
        val mimeType = requireNotNull(format.sampleMimeType)
        return if (!FfmpegLibrary.isAvailable || !MimeTypes.isAudio(mimeType)) C.FORMAT_UNSUPPORTED_TYPE
        else if (!FfmpegLibrary.supportsFormat(mimeType) || (!sinkSupportsFormat(format, C.ENCODING_PCM_16BIT) && !sinkSupportsFormat(format, C.ENCODING_PCM_FLOAT))) C.FORMAT_UNSUPPORTED_SUBTYPE
        else if (format.cryptoType != C.CRYPTO_TYPE_NONE) C.FORMAT_UNSUPPORTED_DRM
        else C.FORMAT_HANDLED
    }

    override fun supportsMixedMimeTypeAdaptation(): @AdaptiveSupport Int = ADAPTIVE_NOT_SEAMLESS

    @Throws(FfmpegDecoderException::class)
    override fun createDecoder(format: Format, cryptoConfig: CryptoConfig?): FfmpegAudioDecoder {
        TraceUtil.beginSection("createFfmpegAudioDecoder")
        val initialInputBufferSize = if (format.maxInputSize != Format.NO_VALUE) format.maxInputSize else DEFAULT_INPUT_BUFFER_SIZE
        val decoder = FfmpegAudioDecoder(format, NUM_BUFFERS, NUM_BUFFERS, initialInputBufferSize, shouldOutputFloat(format))
        TraceUtil.endSection()
        return decoder
    }

    override fun getOutputFormat(decoder: FfmpegAudioDecoder): Format = Format.Builder()
        .setSampleMimeType(MimeTypes.AUDIO_RAW)
        .setChannelCount(decoder.channelCount)
        .setSampleRate(decoder.sampleRate)
        .setPcmEncoding(decoder.encoding)
        .build()

    /**
     * Returns whether the renderer's [AudioSink] supports the PCM format that will be output
     * from the decoder for the given input format and requested output encoding.
     */
    private fun sinkSupportsFormat(inputFormat: Format, pcmEncoding: @PcmEncoding Int): Boolean {
        return sinkSupportsFormat(Util.getPcmFormat(pcmEncoding, inputFormat.channelCount, inputFormat.sampleRate))
    }

    private fun shouldOutputFloat(inputFormat: Format): Boolean {
        if (!sinkSupportsFormat(inputFormat, C.ENCODING_PCM_16BIT)) {
            // We have no choice because the sink doesn't support 16-bit integer PCM.
            return true
        }

        val formatSupport: @SinkFormatSupport Int = getSinkFormatSupport(Util.getPcmFormat(C.ENCODING_PCM_FLOAT, inputFormat.channelCount, inputFormat.sampleRate))
        return when (formatSupport) {
            // using for all other formats.
            AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY -> MimeTypes.AUDIO_AC3 != inputFormat.sampleMimeType // AC-3 is always 16-bit, so there's no point using floating point. Assume that it's worth
            AudioSink.SINK_FORMAT_UNSUPPORTED, AudioSink.SINK_FORMAT_SUPPORTED_WITH_TRANSCODING -> false   // Always prefer 16-bit PCM if the sink does not provide direct support for floating point.
            else -> false
        }
    }

    companion object {
        private const val TAG = "FfmpegAudioRenderer"

        /** The number of input and output buffers.  */
        private const val NUM_BUFFERS = 16

        /** The default input buffer size.  */
        private const val DEFAULT_INPUT_BUFFER_SIZE = 960 * 6
    }
}
