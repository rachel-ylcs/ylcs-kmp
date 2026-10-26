package androidx.compose.animation.graphics.vector

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathSegment
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.toPath
import kotlin.math.ceil

/** Immutable distance samples: no native path/measure is shared by running animations. */
internal class MotionPath(nodes: List<PathNode>) {
    private val distances: DoubleArray
    private val xs: FloatArray
    private val ys: FloatArray

    init {
        require(nodes.size <= 65536) { "Motion path has too many nodes" }
        val contours: MutableList<Path> = []
        val iterator = PathIterator(nodes.toPath())
        val points = FloatArray(8)
        var contour = Path()
        var hasContour = false
        // Skia's iterator omits a contour consisting only of a move verb.
        var fallback = when (val first = nodes.firstOrNull()) {
            is PathNode.MoveTo -> Offset(first.x, first.y)
            is PathNode.RelativeMoveTo -> Offset(first.dx, first.dy)
            else -> Offset.Zero
        }
        var verbs = 0
        while (iterator.hasNext()) {
            require(++verbs <= 65536) { "Motion path has too many segments" }
            when (iterator.next(points)) {
                PathSegment.Type.Move -> {
                    if (hasContour) contours.add(contour)
                    contour = Path()
                    contour.moveTo(points[0], points[1])
                    if (!hasContour) fallback = Offset(points[0], points[1])
                    hasContour = true
                }
                PathSegment.Type.Line -> contour.lineTo(points[2], points[3])
                PathSegment.Type.Quadratic -> contour.quadraticTo(points[2], points[3], points[4], points[5])
                PathSegment.Type.Cubic -> contour.cubicTo(points[2], points[3], points[4], points[5], points[6], points[7])
                PathSegment.Type.Close -> contour.close()
                PathSegment.Type.Conic -> error("Motion path iterator must convert conics to quadratics")
                PathSegment.Type.Done -> break
            }
        }
        if (hasContour) contours.add(contour)
        val samples: MutableList<Offset> = []
        val lengths: MutableList<Double> = []
        val measure = PathMeasure()
        var distance = 0.0
        for (path in contours) {
            measure.setPath(path, false)
            val length = measure.length
            require(length.isFinite() && length >= 0f) { "Motion path length is invalid" }
            if (length == 0f) continue
            val steps = ceil(length.toDouble() / 0.5).coerceIn(1.0, 2048.0).toInt()
            require(samples.size + steps + 1 <= 65536) { "Motion path has too many samples" }
            for (index in 0 .. steps) {
                val fraction = index.toDouble() / steps
                val point = measure.getPosition((length * fraction).toFloat())
                require(point.x.isFinite() && point.y.isFinite()) { "Motion path position is invalid" }
                samples.add(point)
                lengths.add(distance + length * fraction)
            }
            distance += length
        }
        if (samples.isEmpty()) {
            require(fallback.x.isFinite() && fallback.y.isFinite()) { "Motion path position is invalid" }
            samples.add(fallback)
            lengths.add(0.0)
        }
        distances = DoubleArray(lengths.size) { lengths[it] }
        xs = FloatArray(samples.size) { samples[it].x }
        ys = FloatArray(samples.size) { samples[it].y }
    }

    fun coordinate(fraction: Float, xAxis: Boolean): Float {
        val values = if (xAxis) xs else ys
        if (fraction <= 0f || values.size == 1) return values.first()
        if (fraction >= 1f) return values.last()
        val distance = distances.last() * fraction
        var low = 0
        var high = distances.lastIndex
        // Choose the next contour at a repeated distance, without drawing a connecting line.
        while (low < high) {
            val middle = (low + high + 1) ushr 1
            if (distances[middle] <= distance) low = middle
            else high = middle - 1
        }
        if (low == values.lastIndex) return values[low]
        val span = distances[low + 1] - distances[low]
        val inner = if (span == 0.0) 0.0 else (distance - distances[low]) / span
        return (values[low] + (values[low + 1] - values[low]) * inner).toFloat()
    }
}
