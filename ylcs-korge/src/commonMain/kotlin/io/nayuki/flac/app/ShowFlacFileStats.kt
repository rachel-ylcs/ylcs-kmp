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
import io.nayuki.flac.common.StreamInfo
import io.nayuki.flac.decode.DataFormatException
import io.nayuki.flac.decode.FrameDecoder
import io.nayuki.flac.decode.SeekableFileFlacInput
import kotlin.jvm.JvmStatic

/**
 * Reads a FLAC file, collects various statistics, and
 * prints human-formatted information to standard output.
 *
 * Usage: java ShowFlacFileStats InFile.flac
 *
 * Example output from this program (abbreviated):
 * <pre>===== Block sizes (samples) =====
 * 4096: * (11)
 * 5120: ***** (56)
 * 6144: *********** (116)
 * 7168: ************* (134)
 * 8192: ***************** (177)
 * 9216: ***************** (182)
 * 10240: ***************** (179)
 * 11264: ****************************** (318)
 * 12288: ****************** (194)
 *
 * ===== Frame sizes (bytes) =====
 * 12000: ****** (20)
 * 13000: ******* (24)
 * 14000: ********** (34)
 * 15000: **************** (51)
 * 16000: ********************* (68)
 * 17000: ******************* (63)
 * 18000: ******************* (63)
 * 19000: ************************ (77)
 * 20000: ********************* (70)
 * 21000: ****************** (60)
 * 22000: ************************* (82)
 * 23000: ********************* (69)
 * 24000: *************************** (87)
 * 25000: *************************** (88)
 * 26000: ********************** (73)
 * 27000: ************************** (84)
 * 28000: ****************************** (98)
 * 29000: ********************** (73)
 * 30000: *********************** (75)
 * 31000: ************ (39)
 *
 * ===== Average compression ratio at block sizes =====
 * 4096: ********************** (0.7470)
 * 5120: ******************** (0.6815)
 * 6144: ******************** (0.6695)
 * 7168: ******************* (0.6438)
 * 8192: ******************* (0.6379)
 * 9216: ****************** (0.6107)
 * 10240: ****************** (0.6022)
 * 11264: ***************** (0.5628)
 * 12288: ***************** (0.5724)
 *
 * ===== Stereo coding modes =====
 * Independent: **** (83)
 * Left-side  :  (3)
 * Right-side : ************************ (574)
 * Mid-side   : ****************************** (708)</pre>
 */
object ShowFlacFileStats {
    /*---- Main application function ----*/
    @Throws(java.io.IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // Handle command line arguments
        if (args.size != 1) {
            java.lang.System.err.println("Usage: java ShowFlacFileStats InFile.flac")
            java.lang.System.exit(1)
            return
        }
        val inFile: java.io.File = java.io.File(args[0])

        // Data structures to hold statistics
        val blockSizes: MutableList<Int> = java.util.ArrayList<Int>()
        val frameSizes: MutableList<Int> = java.util.ArrayList<Int>()
        val channelAssignments: MutableList<Int> = java.util.ArrayList<Int>()

        // Read input file
        var streamInfo: StreamInfo? = null
        SeekableFileFlacInput(inFile).use { input ->
            // Magic string "fLaC"
            if (input.readUint(32) != 0x664C6143) throw DataFormatException("Invalid magic string")

            // Handle metadata blocks
            var last = false
            while (!last) {
                last = input.readUint(1) != 0
                val type = input.readUint(7)
                val length = input.readUint(24)
                val data = ByteArray(length)
                input.readFully(data)
                if (type == 0) streamInfo = StreamInfo(data)
            }

            // Decode every frame
            val dec = FrameDecoder(input, streamInfo!!.sampleDepth)
            val blockSamples = Array(8) { IntArray(65536) }
            while (true) {
                val meta = dec.readFrame(blockSamples, 0) ?: break
                blockSizes.add(meta.blockSize)
                frameSizes.add(meta.frameSize)
                channelAssignments.add(meta.channelAssignment)
            }
        }

        // Build and print graphs
        printBlockSizeHistogram(blockSizes)
        printFrameSizeHistogram(frameSizes)
        printCompressionRatioGraph(streamInfo, blockSizes, frameSizes)
        if (streamInfo!!.numChannels == 2) printStereoModeGraph(channelAssignments)
    }

    /*---- Statistics-processing functions ----*/
    private fun printBlockSizeHistogram(blockSizes: List<Int>) {
        val blockSizeCounts: MutableMap<Int, Int> = java.util.TreeMap<Int, Int>()
        for (bs in blockSizes) {
            if (!blockSizeCounts.containsKey(bs)) blockSizeCounts[bs] = 0
            val count = blockSizeCounts[bs]!! + 1
            blockSizeCounts[bs] = count
        }
        val blockSizeLabels: MutableList<String> = java.util.ArrayList<String>()
        val blockSizeValues: MutableList<Double> = java.util.ArrayList<Double>()
        for ((key, value) in blockSizeCounts) {
            blockSizeLabels.add(String.format("%5d", key))
            blockSizeValues.add(value.toDouble())
        }
        printNormalizedBarGraph("Block sizes (samples)", blockSizeLabels, blockSizeValues)
    }

    private fun printFrameSizeHistogram(frameSizes: List<Int>) {
        val step = 1000
        val frameSizeCounts: java.util.SortedMap<Int, Int> = java.util.TreeMap<Int, Int>()
        var maxKeyLen = 0
        for (fs in frameSizes) {
            val key: Int = java.lang.Math.round(fs.toDouble() / step).toInt()
            maxKeyLen = java.lang.Math.max(java.lang.Integer.toString(key * step).length, maxKeyLen)
            if (!frameSizeCounts.containsKey(key)) frameSizeCounts.put(key, 0)
            frameSizeCounts.put(key, frameSizeCounts.get(key) + 1)
        }
        for (i in frameSizeCounts.firstKey() until frameSizeCounts.lastKey()) {
            if (!frameSizeCounts.containsKey(i)) frameSizeCounts.put(i, 0)
        }
        val frameSizeLabels: MutableList<String> = java.util.ArrayList<String>()
        val frameSizeValues: MutableList<Double> = java.util.ArrayList<Double>()
        for ((key, value) in frameSizeCounts.entries) {
            frameSizeLabels.add(String.format("%" + maxKeyLen + "d", key * step))
            frameSizeValues.add(value.toDouble())
        }
        printNormalizedBarGraph("Frame sizes (bytes)", frameSizeLabels, frameSizeValues)
    }

    private fun printCompressionRatioGraph(streamInfo: StreamInfo?, blockSizes: List<Int>, frameSizes: List<Int>) {
        val blockSizeCounts: MutableMap<Int, Int> = java.util.TreeMap<Int, Int>()
        val blockSizeBytes: MutableMap<Int, Long> = java.util.TreeMap<Int, Long>()
        for (i in blockSizes.indices) {
            val bs = blockSizes[i]
            if (!blockSizeCounts.containsKey(bs)) {
                blockSizeCounts[bs] = 0
                blockSizeBytes[bs] = 0L
            }
            blockSizeCounts[bs] = blockSizeCounts[bs]!! + 1
            blockSizeBytes[bs] = blockSizeBytes[bs]!! + frameSizes[i]
        }
        val blockRatioLabels: MutableList<String> = java.util.ArrayList<String>()
        val blockRatioValues: MutableList<Double> = java.util.ArrayList<Double>()
        for ((key, value) in blockSizeCounts) {
            blockRatioLabels.add(String.format("%5d", key))
            blockRatioValues.add(blockSizeBytes[key]!! / (value.toDouble() * key * streamInfo!!.numChannels * streamInfo.sampleDepth / 8))
        }
        printNormalizedBarGraph("Average compression ratio at block sizes", blockRatioLabels, blockRatioValues)
    }

    private fun printStereoModeGraph(channelAssignments: List<Int>) {
        val stereoModeLabels: List<String> = mutableListOf("Independent", "Left-side", "Right-side", "Mid-side")
        val stereoModeValues: MutableList<Double> = java.util.ArrayList<Double>()
        for (i in 0..3) stereoModeValues.add(0.0)
        for (mode in channelAssignments) {
            var index: Int
            index = when (mode) {
                1 -> 0
                8 -> 1
                9 -> 2
                10 -> 3
                else -> throw DataFormatException("Invalid mode in stereo stream")
            }
            stereoModeValues[index] = stereoModeValues[index] + 1
        }
        printNormalizedBarGraph("Stereo coding modes", stereoModeLabels, stereoModeValues)
    }

    /*---- Utility functions ----*/
    private fun printNormalizedBarGraph(heading: String, labels: List<String>, values: List<Double>) {
        java.util.Objects.requireNonNull<String>(heading)
        java.util.Objects.requireNonNull<List<String>>(labels)
        java.util.Objects.requireNonNull<List<Double>>(values)
        if (labels.size != values.size) throw java.lang.IllegalArgumentException()
        val maxBarWidth = 100
        java.lang.System.out.printf("==================== %s ====================%n", heading)
        println()
        var maxLabelLen = 0
        for (s in labels) maxLabelLen = java.lang.Math.max(s.length, maxLabelLen)
        val spaces = String(CharArray(maxLabelLen)).replace(0.toChar(), ' ')
        var maxValue = 1.0 // This avoids division by zero
        for (`val` in values) maxValue = java.lang.Math.max(`val`, maxValue)
        for (i in labels.indices) {
            val label = labels[i]
            val value = values[i]
            val barWidth: Int = java.lang.Math.round(value / maxValue * maxBarWidth).toInt()
            val bar = String(CharArray(barWidth)).replace(0.toChar(), '*')
            java.lang.System.out.printf(
                "%s%s: %s (%s)%n",
                label,
                spaces.substring(label.length),
                bar,
                if (value.toLong()
                        .toDouble() == value
                ) java.lang.Long.toString(value.toLong()) else java.lang.Double.toString(value)
            )
        }
        println()
        println()
    }
}
*/
