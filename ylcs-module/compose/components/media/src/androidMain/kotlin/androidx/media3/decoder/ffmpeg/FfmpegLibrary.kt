package androidx.media3.decoder.ffmpeg

import androidx.media3.common.C
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.LibraryLoader
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi

/** Configures and queries the underlying native library.  */
@UnstableApi
object FfmpegLibrary {
    init {
        MediaLibraryInfo.registerModule("media3.decoder.ffmpeg")
    }

    private const val TAG = "FfmpegLibrary"

    private val LOADER: LibraryLoader = object : LibraryLoader("ffmpegJNI") {
        override fun loadLibrary(name: String) {
            System.loadLibrary(name)
        }
    }

    var version: String? = null
        /** Returns the version of the underlying library if available, or null otherwise.  */
        get() {
            if (!isAvailable) return null
            if (field == null) field = ffmpegGetVersion()
            return field
        }
        private set

    var inputBufferPaddingSize: Int = C.LENGTH_UNSET
        /**
         * Returns the required amount of padding for input buffers in bytes, or [C.LENGTH_UNSET] if
         * the underlying library is not available.
         */
        get() {
            if (!isAvailable) return C.LENGTH_UNSET
            if (field == C.LENGTH_UNSET) field = ffmpegGetInputBufferPaddingSize()
            return field
        }
        private set

    val isAvailable: Boolean get() = LOADER.isAvailable()
        /** Returns whether the underlying library is available, loading it if necessary.  */

    /**
     * Returns whether the underlying library supports the specified MIME type.
     *
     * @param mimeType The MIME type to check.
     */
    fun supportsFormat(mimeType: String): Boolean {
        if (!isAvailable) return false
        val codecName = getCodecName(mimeType) ?: return false
        if (!ffmpegHasDecoder(codecName)) {
            Log.w(TAG, "No $codecName decoder available. Check the FFmpeg build configuration.")
            return false
        }
        return true
    }

    /**
     * Returns the name of the FFmpeg decoder that could be used to decode the format, or `null`
     * if it's unsupported.
     */
    fun getCodecName(mimeType: String): String? {
        when (mimeType) {
            MimeTypes.AUDIO_AAC -> return "aac"
            MimeTypes.AUDIO_MPEG, MimeTypes.AUDIO_MPEG_L1, MimeTypes.AUDIO_MPEG_L2 -> return "mp3"
            MimeTypes.AUDIO_AC3 -> return "ac3"
            MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> return "eac3"
            MimeTypes.AUDIO_TRUEHD -> return "truehd"
            MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD -> return "dca"
            MimeTypes.AUDIO_VORBIS -> return "vorbis"
            MimeTypes.AUDIO_OPUS -> return "opus"
            MimeTypes.AUDIO_AMR_NB -> return "amrnb"
            MimeTypes.AUDIO_AMR_WB -> return "amrwb"
            MimeTypes.AUDIO_FLAC -> return "flac"
            MimeTypes.AUDIO_ALAC -> return "alac"
            MimeTypes.AUDIO_MLAW -> return "pcm_mulaw"
            MimeTypes.AUDIO_ALAW -> return "pcm_alaw"
            MimeTypes.VIDEO_H264 -> return "h264"
            MimeTypes.VIDEO_H265 -> return "hevc"
            else -> return null
        }
    }

    private external fun ffmpegGetVersion(): String?
    private external fun ffmpegGetInputBufferPaddingSize(): Int
    private external fun ffmpegHasDecoder(codecName: String?): Boolean
}
