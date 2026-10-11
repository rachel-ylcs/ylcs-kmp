package love.yinlin.foundation.parser.arguments

internal class ArgumentsParser {
    var executable: String? = null
        private set
    val arguments: Map<String, Argument>

    private val content = linkedMapOf<String, MutableList<String>?>()
    private var source = ""
    private var index = 0
    private var argumentIndex: Int? = null
    private var isFirst = true

    constructor(commandLine: String) {
        source = commandLine
        skipWhitespace()
        while (index < source.length) {
            readArgument()
            skipWhitespace()
        }
        arguments = finish()
    }

    constructor(args: Array<out String>) {
        for (i in args.indices) {
            source = args[i]
            index = 0
            argumentIndex = i
            if (source.isEmpty()) fail("Empty argument")
            if (isFirst && '=' !in source && '"' !in source && isExecutable(source)) {
                executable = source
                isFirst = false
            }
            else {
                readArgument()
                if (index != source.length) fail("Each array element must contain exactly one argument")
            }
        }
        arguments = finish()
    }

    private fun readArgument() {
        if (source[index] == '"') {
            if (!isFirst) fail("Argument keys cannot be quoted")
            val start = index
            val path = readQuoted()
            if (path.isEmpty()) fail("Executable path cannot be empty", start)
            executable = path
            requireBoundary()
        }
        else {
            val start = index
            while (index < source.length && !source[index].isWhitespace() && source[index] != '=') {
                if (source[index] == '"') fail("Argument keys cannot contain quotes")
                ++index
            }
            if (index == start) fail("Argument key cannot be empty")
            val key = source.substring(start, index)
            if (index < source.length && source[index] == '=') {
                ++index
                val value = readValue()
                val values = content[key]
                if (values == null) content[key] = arrayListOf(value)
                else values.add(value)
            }
            else if (isFirst && isExecutable(key)) executable = key
            else if (key !in content) content[key] = null
        }
        isFirst = false
    }

    private fun readValue(): String {
        if (index < source.length && source[index] == '"') {
            val value = readQuoted()
            requireBoundary()
            return value
        }
        val start = index
        while (index < source.length && !source[index].isWhitespace()) {
            when (source[index]) {
                '=' -> fail("Values containing '=' must be double quoted")
                '"' -> fail("Double quotes must enclose the entire value")
            }
            ++index
        }
        return source.substring(start, index)
    }

    private fun readQuoted(): String {
        val opening = index++
        var start = index
        var builder: StringBuilder? = null
        while (index < source.length) {
            when {
                source[index] == '"' -> {
                    val value = builder?.append(source, start, index)?.toString() ?: source.substring(start, index)
                    ++index
                    return value
                }
                source[index] == '\\' && index + 1 < source.length && source[index + 1] == '"' -> {
                    val output = builder ?: StringBuilder().also { builder = it }
                    output.append(source, start, index).append('"')
                    index += 2
                    start = index
                }
                else -> ++index
            }
        }
        fail("Unterminated double quoted string", opening)
    }

    private fun requireBoundary() {
        if (index < source.length && !source[index].isWhitespace()) fail("Expected whitespace after closing quote")
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) ++index
    }

    private fun isExecutable(value: String): Boolean = !value.startsWith('-') && (
        '/' in value || '\\' in value ||
                value.endsWith(".exe", true) || value.endsWith(".com", true) ||
                value.endsWith(".bat", true) || value.endsWith(".cmd", true) ||
                value.endsWith(".sh", true) || value.endsWith(".bin", true) ||
                value.endsWith(".kexe", true) || value.endsWith(".app", true) ||
                value.endsWith(".out", true) || value.endsWith(".jar", true)
    )

    private fun finish(): Map<String, Argument> = content.mapValues { [_, values] ->
        if (values == null) Argument.Empty else Argument(values)
    }

    private fun fail(message: String, offset: Int = index): Nothing = throw ArgumentsParseException(message, offset, argumentIndex)
}
