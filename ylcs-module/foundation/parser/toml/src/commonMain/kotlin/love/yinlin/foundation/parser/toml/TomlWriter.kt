package love.yinlin.foundation.parser.toml

internal class TomlWriter(private val configuration: TomlConfiguration) {
    private val output = StringBuilder()
    private var nodes = 0

    fun write(element: TomlElement): String {
        require(element is TomlObject) { "A TOML document must have a table root" }
        if (configuration.style == TomlStyle.Inline) {
            count(0)
            for ([key, child] in element) {
                key(key)
                output.append(" = ")
                value(child, 1)
                output.append('\n')
            }
        } else table(element, "", 0, false)
        return output.toString()
    }

    fun writeValue(element: TomlElement): String {
        value(element, 0)
        return output.toString()
    }

    private fun table(element: TomlObject, path: String, depth: Int, arrayHeader: Boolean) {
        count(depth)
        if (path.isNotEmpty()) {
            if (output.isNotEmpty()) output.append('\n')
            output.append(if (arrayHeader) "[[" else "[")
            output.append(path)
            output.append(if (arrayHeader) "]]\n" else "]\n")
        }

        for ([key, child] in element) if (child !is TomlObject && !isTableArray(child)) {
            key(key)
            output.append(" = ")
            value(child, depth + 1)
            output.append('\n')
        }
        for ([key, child] in element) {
            if (child is TomlObject) table(child, path(path, key), depth + 1, false)
            else if (isTableArray(child)) {
                child as TomlArray
                count(depth + 1)
                val childPath = path(path, key)
                for (item in child) table(item.Object, childPath, depth + 2, true)
            }
        }
    }

    private fun isTableArray(element: TomlElement): Boolean =
        element is TomlArray && element.isNotEmpty() && element.all { it is TomlObject }

    private fun value(element: TomlElement, depth: Int) {
        count(depth)
        when (element) {
            is TomlPrimitive -> if (element.isString) string(output, element.content) else {
                if (element.isDateTime) require(TomlScalarKind.dateTimeKind(element.content, configuration.version) != null) {
                    "Date/time '${element.content}' cannot be written as ${configuration.version}"
                }
                output.append(element.content)
            }
            is TomlArray -> {
                output.append('[')
                for (i in element.indices) {
                    if (i != 0) output.append(", ")
                    value(element[i], depth + 1)
                }
                output.append(']')
            }
            is TomlObject -> {
                output.append('{')
                var first = true
                for ([name, child] in element) {
                    if (!first) output.append(", ")
                    first = false
                    key(name)
                    output.append(" = ")
                    value(child, depth + 1)
                }
                output.append('}')
            }
        }
    }

    private fun key(name: String) {
        if (name.isNotEmpty() && name.all { TomlScalarKind.isBareKey(it) }) output.append(name) else string(output, name)
    }

    private fun path(parent: String, name: String): String {
        val builder = StringBuilder()
        if (parent.isNotEmpty()) builder.append(parent).append('.')
        if (name.isNotEmpty() && name.all { TomlScalarKind.isBareKey(it) }) builder.append(name) else string(builder, name)
        return builder.toString()
    }

    private fun string(builder: StringBuilder, text: String) {
        builder.append('"')
        var start = 0
        var i = 0
        while (i < text.length) {
            val char = text[i]
            val escape = when (char) {
                '"' -> "\\\""
                '\\' -> "\\\\"
                '\b' -> "\\b"
                '\t' -> "\\t"
                '\n' -> "\\n"
                '\u000c' -> "\\f"
                '\r' -> "\\r"
                else -> null
            }
            if (escape != null || char < ' ' || char == '\u007f') {
                builder.append(text, start, i)
                if (escape != null) builder.append(escape) else {
                    builder.append("\\u")
                    for (shift in 12 downTo 0 step 4) builder.append("0123456789abcdef"[(char.code shr shift) and 15])
                }
                start = i + 1
            } else if (char in '\ud800'..'\udbff') {
                require(text.getOrNull(i + 1) in '\udc00'..'\udfff') { "Unpaired Unicode surrogate in TOML string" }
                ++i
            } else require(char !in '\udc00'..'\udfff') { "Unpaired Unicode surrogate in TOML string" }
            ++i
        }
        builder.append(text, start, text.length).append('"')
    }

    private fun count(depth: Int) {
        require(depth <= configuration.maxDepth) { "Maximum TOML depth exceeded" }
        require(++nodes <= configuration.maxNodes) { "Maximum TOML node count exceeded" }
    }
}