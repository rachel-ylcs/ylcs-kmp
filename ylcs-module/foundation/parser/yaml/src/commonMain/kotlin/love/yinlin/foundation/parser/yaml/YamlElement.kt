package love.yinlin.foundation.parser.yaml

sealed class YamlElement {
    final override fun toString(): String =
        if (this is YamlDocument) Yaml.encodeToString(this)
        else YamlWriter(YamlConfiguration(style = YamlStyle.Flow)).write(this)
}
