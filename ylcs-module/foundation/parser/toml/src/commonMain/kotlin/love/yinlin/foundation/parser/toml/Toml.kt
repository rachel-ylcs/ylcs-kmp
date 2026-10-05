package love.yinlin.foundation.parser.toml

import org.intellij.lang.annotations.Language

class Toml(val configuration: TomlConfiguration = TomlConfiguration()) {
    fun parse(@Language("TOML") source: String): TomlElement = TomlParser(source, configuration).parse()
    fun encodeToString(element: TomlElement): String = TomlWriter(configuration).write(element)

    companion object {
        val Default: Toml = Toml()

        fun parse(@Language("TOML") source: String): TomlElement = Default.parse(source)
        fun encodeToString(element: TomlElement): String = Default.encodeToString(element)
    }
}

