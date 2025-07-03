/* 
 * FLAC library (Java)
 * 
 * Copyright (c) Project Nayuki
 * https://www.nayuki.io/page/flac-library-java
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program (see COPYING.txt and COPYING.LESSER.txt).
 * If not, see <http://www.gnu.org/licenses/>.
 */
package io.nayuki.flac.app

/*
import io.nayuki.flac.decode.FlacDecoder
import java.awt.event.MouseAdapter
import java.awt.event.MouseMotionAdapter
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.SourceDataLine
import javax.swing.JFrame
import javax.swing.JSlider
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.plaf.basic.BasicSliderUI
import javax.swing.plaf.metal.MetalSliderUI
import kotlin.jvm.JvmStatic

/**
 * Plays a single FLAC file to the system audio output, showing a GUI window with a seek bar.
 * The file to play is specified as a command line argument. The seek bar is responsible for both
 * displaying the current playback position, and allowing the user to click to seek to new positions.
 *
 * Usage: java SeekableFlacPlayerGui InFile.flac
 */
object SeekableFlacPlayerGui {
    @Throws(LineUnavailableException::class, java.io.IOException::class, java.lang.InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        /*-- Initialization code --*/

        // Handle command line arguments
        if (args.size != 1) {
            java.lang.System.err.println("Usage: java SeekableFlacPlayerGui InFile.flac")
            java.lang.System.exit(1)
            return
        }
        val inFile: java.io.File = java.io.File(args[0])

        // Process header metadata blocks
        val decoder = FlacDecoder(inFile)
        while (decoder.readAndHandleMetadataBlock() != null);
        val streamInfo = decoder.streamInfo
        if (streamInfo!!.numSamples == 0L) throw java.lang.IllegalArgumentException("Unknown audio length")

        // Start Java sound output API
        val format: javax.sound.sampled.AudioFormat = javax.sound.sampled.AudioFormat(
            streamInfo.sampleRate.toFloat(),
            streamInfo.sampleDepth, streamInfo.numChannels, true, false
        )
        val info: javax.sound.sampled.DataLine.Info =
            javax.sound.sampled.DataLine.Info(SourceDataLine::class.java, format)
        val line: SourceDataLine = AudioSystem.getLine(info) as SourceDataLine
        line.open(format)
        line.start()

        // Create GUI object, event handler, communication object
        val seekRequest = doubleArrayOf(-1.0)
        val gui = AudioPlayerGui("FLAC Player")
        gui.listener = object : AudioPlayerGui.Listener {
            override fun seekRequested(t: Double) {
                synchronized(seekRequest) {
                    seekRequest[0] = t
                    seekRequest.notify()
                }
            }

            override fun windowClosing() {
                java.lang.System.exit(0)
            }
        }

        /*-- Audio player loop --*/

        // Decode and write audio data, handle seek requests, wait for seek when end of stream reached
        val bytesPerSample = streamInfo.sampleDepth / 8
        var startTime: Long = line.getMicrosecondPosition()

        // Buffers for data created and discarded within each loop iteration, but allocated outside the loop
        val samples = Array<IntArray?>(streamInfo.numChannels) { IntArray(65536) }
        val sampleBytes = ByteArray(65536 * streamInfo.numChannels * bytesPerSample)
        while (true) {

            // Get and clear seek request, if any
            var seekReq: Double
            synchronized(seekRequest) {
                seekReq = seekRequest[0]
                seekRequest[0] = -1.0
            }

            // Decode next audio block, or seek and decode
            var blockSamples: Int
            if (seekReq == -1.0) blockSamples = decoder.readAudioBlock(samples, 0) else {
                val samplePos: Long = java.lang.Math.round(seekReq * streamInfo.numSamples)
                seekReq = -1.0
                blockSamples = decoder.seekAndReadAudioBlock(samplePos, samples, 0)
                line.flush()
                startTime =
                    line.getMicrosecondPosition() - java.lang.Math.round(samplePos * 1e6 / streamInfo.sampleRate)
            }

            // Set display position
            val timePos: Double = (line.getMicrosecondPosition() - startTime) / 1e6
            gui.setPosition(timePos * streamInfo.sampleRate / streamInfo.numSamples)

            // Wait when end of stream reached
            if (blockSamples == 0) {
                synchronized(seekRequest) { while (seekRequest[0] == -1.0) seekRequest.wait() }
                continue
            }

            // Convert samples to channel-interleaved bytes in little endian
            var sampleBytesLen = 0
            for (i in 0 until blockSamples) {
                for (ch in 0 until streamInfo.numChannels) {
                    val `val` = samples[ch]!![i]
                    var j = 0
                    while (j < bytesPerSample) {
                        sampleBytes[sampleBytesLen] = (`val` ushr (j shl 3)).toByte()
                        j++
                        sampleBytesLen++
                    }
                }
            }
            line.write(sampleBytes, 0, sampleBytesLen)
        }
    }

    /*---- User interface classes ----*/
    private class AudioPlayerGui(windowTitle: String?) {
        /*-- Fields --*/
        var listener: Listener? = null
        private val slider: JSlider
        private val sliderUi: BasicSliderUI

        /*-- Constructor --*/
        init {
            // Create and configure slider
            slider = JSlider(SwingConstants.HORIZONTAL, 0, 10000, 0)
            sliderUi = MetalSliderUI()
            slider.setUI(sliderUi)
            slider.setPreferredSize(java.awt.Dimension(800, 50))
            slider.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(ev: java.awt.event.MouseEvent) {
                    moveSlider(ev)
                }

                override fun mouseReleased(ev: java.awt.event.MouseEvent) {
                    moveSlider(ev)
                    listener!!.seekRequested(slider.getValue().toDouble() / slider.getMaximum())
                }
            })
            slider.addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseDragged(ev: java.awt.event.MouseEvent) {
                    moveSlider(ev)
                }
            })

            // Create and configure frame (window)
            val frame = JFrame(windowTitle)
            frame.add(slider)
            frame.pack()
            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(ev: WindowEvent) {
                    listener!!.windowClosing()
                }
            })
            frame.setResizable(false)
            frame.setVisible(true)
        }

        /*-- Methods --*/
        fun setPosition(t: Double) {
            if (java.lang.Double.isNaN(t)) return
            val `val`: Double = java.lang.Math.max(java.lang.Math.min(t, 1.0), 0.0)
            SwingUtilities.invokeLater(object : java.lang.Runnable() {
                override fun run() {
                    if (!slider.getValueIsAdjusting()) slider.setValue(
                        java.lang.Math.round(`val` * slider.getMaximum()).toInt()
                    )
                }
            })
        }

        private fun moveSlider(ev: java.awt.event.MouseEvent) {
            slider.setValue(sliderUi.valueForXPosition(ev.getX()))
        }

        /*-- Helper interface --*/
        interface Listener {
            fun seekRequested(t: Double) // 0.0 <= t <= 1.0
            fun windowClosing()
        }
    }
}
*/
