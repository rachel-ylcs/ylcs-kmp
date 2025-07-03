package korlibs.audio.format.flac

import io.nayuki.flac.decode.*
import korlibs.audio.format.*
import korlibs.audio.sound.*
import korlibs.datastructure.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.math.*
import korlibs.time.*
import kotlin.time.*

object FLAC : FlacAudioFormatBase()

open class FlacAudioFormatBase : AudioFormat("flac") {
    var Info.rate by Extra.Property { 0 }

    override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? {
        val bytes = data.sliceStart().readBytesUpTo(0x100)
        if (bytes.readStringz(0, 4) != "fLaC") return null

        val decoder = FlacDecoder(bytes.openSync())
        try {
            while (decoder.readAndHandleMetadataBlock() != null) Unit
        } catch (e: Throwable) {
        }
        val nchannels = decoder.streamInfo!!.numChannels
        val sampleRate = decoder.streamInfo!!.sampleRate
        val nsamples = decoder.streamInfo!!.numSamples
        val totalTime = (nsamples.toDouble() / sampleRate.toDouble()).seconds
        return Info(totalTime, nchannels).also {
            it.rate = sampleRate
        }
    }

    override suspend fun decodeStreamInternal(data: AsyncStream, props: AudioDecodingProps): AudioStream? {
        val info = tryReadInfo(data.sliceStart()) ?: return null
        val dataSync = data.readAll().openSync()
        return AudioStreamFlacDecoder(info.rate, info.channels, dataSync, info)
    }

    class AudioStreamFlacDecoder(rate: Int, channels: Int, val dataSyncBase: SyncStream, val info: Info) : AudioStream(rate, channels) {
        val dataSync = dataSyncBase.clone()
        val decoder = FlacDecoder(dataSync).also {
            while (it.readAndHandleMetadataBlock() != null) Unit
        }

        override val totalLengthInSamples: Long?
            get() = info.duration?.let { (it.seconds * rate).toLong() }

        private var seeked = true
        var _currentPositionInSamples: Long = 0L

        override var currentPositionInSamples: Long
            get() = _currentPositionInSamples
            set(value) {
                seeked = true
                _currentPositionInSamples = value
            }

        override suspend fun clone(): AudioStream = AudioStreamFlacDecoder(rate, channels, dataSyncBase, info)

        private val deque = AudioSamplesDeque(channels)

        private fun fillBuffer() {
            //println("fillBuffer")
            val streamInfo = decoder.streamInfo!!
            val temp = Array(streamInfo.numChannels) { IntArray(streamInfo.maxBlockSize) }
            val read = if (seeked) {
                seeked = false
                deque.clear()
                decoder.seekAndReadAudioBlock(_currentPositionInSamples, temp, 0)
            } else {
                decoder.readAudioBlock(temp, 0)
            }
            for (ch in 0 until streamInfo.numChannels) {
                val tempCh = temp[ch]
                for (n in 0 until read) {
                    deque.write(ch, AudioSample(tempCh[n].toShortClamped()))
                }
            }
        }

        override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
            if (length == 0) return 0

            if (deque.availableRead < length) {
                fillBuffer()
            }
            val len = deque.read(out, offset, length)
            //println("read.len=$len")
            return len
        }

        override suspend fun seek(position: Duration) {
            currentPositionInSamples = (position.seconds * rate).toLong()
        }
    }
}
