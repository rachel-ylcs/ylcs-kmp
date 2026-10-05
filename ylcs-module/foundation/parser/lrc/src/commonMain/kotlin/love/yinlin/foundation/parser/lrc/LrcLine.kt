package love.yinlin.foundation.parser.lrc

import kotlinx.serialization.Serializable

@Serializable
data class LrcLine(val position: Long, val text: String) : Comparable<LrcLine> {
    override fun compareTo(other: LrcLine) = position.compareTo(other.position)
}