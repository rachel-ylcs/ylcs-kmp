package love.yinlin.foundation.parser.yaml

import org.intellij.lang.annotations.Language

class Yaml(val configuration: YamlConfiguration = YamlConfiguration()) {
    fun parse(@Language("YAML") source: String): YamlDocument = YamlParser(source, configuration).parse()
    fun encodeToString(element: YamlElement): String = YamlWriter(configuration).write(element)

    companion object {
        val Default: Yaml = Yaml()

        fun parse(@Language("YAML") source: String): YamlDocument = Default.parse(source)
        fun encodeToString(element: YamlElement): String = Default.encodeToString(element)
    }
}
