package love.yinlin.foundation.parser.toml

sealed class TomlElement {
    final override fun toString(): String = Toml.encodeToString(this)
}
