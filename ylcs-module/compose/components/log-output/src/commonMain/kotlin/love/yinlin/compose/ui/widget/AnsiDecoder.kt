package love.yinlin.compose.ui.widget

internal class AnsiDecoder {
    private var style = LogStyle()

    private fun applySgr(sequence: String) {
        val codes = if (sequence.isEmpty()) [0] else sequence.split(';').map { it.toIntOrNull() ?: 0 }
        var i = 0
        while (i < codes.size) {
            when (val code = codes[i]) {
                0 -> style = LogStyle()
                1 -> style = style.copy(bold = true)
                3 -> style = style.copy(italic = true)
                4 -> style = style.copy(underline = true)
                22 -> style = style.copy(bold = false)
                23 -> style = style.copy(italic = false)
                24 -> style = style.copy(underline = false)
                39 -> style = style.copy(foreground = null)
                49 -> style = style.copy(background = null)
                in 30 .. 37 -> style = style.copy(foreground = AnsiColor.Indexed(code - 30))
                in 90 .. 97 -> style = style.copy(foreground = AnsiColor.Indexed(code - 90 + 8))
                in 40 .. 47 -> style = style.copy(background = AnsiColor.Indexed(code - 40))
                in 100 .. 107 -> style = style.copy(background = AnsiColor.Indexed(code - 100 + 8))
                38, 48 -> {
                    var color: AnsiColor? = null
                    if (codes.getOrNull(i + 1) == 5 && codes.getOrNull(i + 2) in 0 .. 255) {
                        color = AnsiColor.Indexed(codes[i + 2])
                        i += 2
                    }
                    else if (codes.getOrNull(i + 1) == 2 && i + 4 < codes.size && codes.subList(i + 2, i + 5).all { it in 0 .. 255 }) {
                        color = AnsiColor.Rgb(codes[i + 2], codes[i + 3], codes[i + 4])
                        i += 4
                    }
                    if (color != null) style = if (code == 38) style.copy(foreground = color) else style.copy(background = color)
                }
            }
            i++
        }
    }

    fun feed(input: String): List<LogSpan> {
        val result: MutableList<LogSpan> = []
        val text = buildString {
            var i = 0
            while (i < input.length) {
                if (input[i] != '\u001b') {
                    append(input[i++])
                    continue
                }
                if (this.isNotEmpty()) {
                    result += LogSpan(this.toString(), style)
                    clear()
                }
                if (i + 1 == input.length) break
                when (input[i + 1]) {
                    '[' -> {
                        var end = i + 2
                        while (end < input.length && input[end].code !in 0x40 .. 0x7e) end++
                        if (end == input.length) break
                        if (input[end] == 'm') applySgr(input.substring(i + 2, end))
                        i = end + 1
                    }
                    ']' -> {
                        var end = i + 2
                        while (end < input.length && input[end] != '\u0007' && !(input[end] == '\u001b' && end + 1 < input.length && input[end + 1] == '\\')) end++
                        if (end == input.length) break
                        i = end + if (input[end] == '\u0007') 1 else 2
                    }
                    else -> i += 2
                }
            }
        }
        if (text.isNotEmpty()) result += LogSpan(text, style)
        style = LogStyle()
        return result
    }
}