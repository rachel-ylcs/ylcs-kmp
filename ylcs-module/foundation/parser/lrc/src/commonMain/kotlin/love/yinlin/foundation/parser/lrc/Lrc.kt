package love.yinlin.foundation.parser.lrc

data class Lrc(
    val lines: List<LrcLine>,
    val metadata: LrcMetadata
) {
    val isEmpty: Boolean = lines.isEmpty()
    val isNotEmpty: Boolean = lines.isNotEmpty()

    val plainText: String get() = lines.joinToString("\n") { it.text }

    override fun toString(): String = buildString {
        for ([key, value] in [
            "ti" to metadata.title,
            "ar" to metadata.artist,
            "al" to metadata.album,
            "au" to metadata.author,
            "by" to metadata.by,
            "re" to metadata.editor,
            "ve" to metadata.version,
            "length" to metadata.length
        ]) {
            if (!value.isNullOrBlank()) appendLine("[$key:$value]")
        }
        if (metadata.offset != 0L) appendLine("[offset:${metadata.offset}]")

        lines.forEachIndexed { index, (val p = position, val text) ->
            val position = (p - metadata.offset).coerceAtLeast(0L)
            val minutes = (position / 60000L).toString().padStart(2, '0')
            val seconds = (position / 1000L % 60L).toString().padStart(2, '0')
            val milliseconds = (position % 1000L / 10L).toString().padStart(2, '0')
            if (index > 0) append('\n')
            append("[$minutes:$seconds.$milliseconds]$text")
        }
    }

    companion object {
        private val SourcePattern = Regex("""\r\n|\r|\n""")
        private val TimePattern = Regex("""\[(\d+):([0-5]?\d)(?:[.:](\d{2,3}))?]""")
        private val MetadataPattern = Regex("""^\[([A-Za-z]+):(.*)]$""")

        fun parse(source: String): Lrc {
            val newLines: MutableList<LrcLine> = []

            var title: String? = null
            var artist: String? = null
            var album: String? = null
            var author: String? = null
            var by: String? = null
            var offset = 0L
            var editor: String? = null
            var version: String? = null
            var length: String? = null

            val items = source.split(SourcePattern)

            for (item in items) {
                val line = item.trim()
                if (line.isEmpty()) continue
                val metadataMatch = MetadataPattern.matchEntire(line)

                if (metadataMatch != null) {
                    val key = metadataMatch.groupValues[1].lowercase()
                    val value = metadataMatch.groupValues[2].trim()

                    when (key) {
                        "ti" -> title = value.ifEmpty { null }
                        "ar" -> artist = value.ifEmpty { null }
                        "al" -> album = value.ifEmpty { null }
                        "au" -> author = value.ifEmpty { null }
                        "by" -> by = value.ifEmpty { null }
                        "offset" -> offset = value.toLongOrNull() ?: 0L
                        "re" -> editor = value.ifEmpty { null }
                        "ve" -> version = value.ifEmpty { null }
                        "length" -> length = value.ifEmpty { null }
                    }
                    continue
                }

                val timeMatches = TimePattern.findAll(line).toList()
                if (timeMatches.isEmpty()) continue

                val lastMatch = timeMatches.last()
                val text = line.substring(lastMatch.range.last + 1).trim()
                if (text.isEmpty()) continue

                for (match in timeMatches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: continue
                    val seconds = match.groupValues[2].toLongOrNull() ?: continue

                    val fraction = match.groupValues[3]
                    val milliseconds = when (fraction.length) {
                        2 -> fraction.toLong() * 10L
                        3 -> fraction.toLong()
                        else -> 0L
                    }

                    val position = (minutes * 60L + seconds) * 1000L + milliseconds

                    newLines += LrcLine(position = position, text = text)
                }
            }

            return Lrc(
                lines = newLines.asSequence().map {
                    it.copy(position = (it.position + offset).coerceAtLeast(0L))
                }.distinct().sorted().toList(),
                metadata = LrcMetadata(
                    title = title,
                    artist = artist,
                    album = album,
                    author = author,
                    by = by,
                    offset = offset,
                    editor = editor,
                    version = version,
                    length = length
                )
            )
        }
    }
}
