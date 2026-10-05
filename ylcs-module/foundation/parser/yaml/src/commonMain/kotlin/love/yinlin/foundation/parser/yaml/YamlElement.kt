package love.yinlin.foundation.parser.yaml

sealed class YamlElement {
    final override fun toString(): String = YamlWriter(YamlConfiguration(style = YamlStyle.Flow)).write(this)
}