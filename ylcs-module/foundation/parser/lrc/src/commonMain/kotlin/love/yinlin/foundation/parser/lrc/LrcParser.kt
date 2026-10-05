package love.yinlin.foundation.parser.lrc

import love.yinlin.extension.timeString

class LrcParser(val lines: List<LrcLine>?, val metadata: LrcMetadata) {
    companion object {
        fun parse(source: String): LrcParser {
            val newLines = mutableListOf<LrcLine>()
            val timePattern = """\[(\d+):([0-5]?\d)(?:[.:](\d{2,3}))?]""".toRegex()
            val metadataPattern = """^\[([A-Za-z]+):(.*)]$""".toRegex()

            var title: String? = null
            var artist: String? = null
            var album: String? = null
            var author: String? = null
            var by: String? = null
            var offset = 0L
            var editor: String? = null
            var version: String? = null
            var length: String? = null

            val items = source.split(Regex("""\r\n|\r|\n"""))

            for (item in items) {
                val line = item.trim()
                if (line.isEmpty()) continue
                val metadataMatch = metadataPattern.matchEntire(line)

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

                val timeMatches = timePattern.findAll(line).toList()
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

            val parsedLines = newLines.asSequence().map {
                it.copy(position = (it.position + offset).coerceAtLeast(0L))
            }.distinct().sorted().toList().ifEmpty { null }

            return LrcParser(
                lines = parsedLines,
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

    val ok: Boolean get() = lines != null

    val plainText: String get() = lines?.let { items ->
        items.joinToString("\n") { it.text }
    } ?: ""

    override fun toString(): String = lines?.let { items ->
        items.joinToString("\n") {
            val milliseconds = (it.position % 1000) / 10
            "[${it.position.timeString}.${if (milliseconds < 10) "0" else ""}$milliseconds]${it.text}"
        }
    } ?: ""
}