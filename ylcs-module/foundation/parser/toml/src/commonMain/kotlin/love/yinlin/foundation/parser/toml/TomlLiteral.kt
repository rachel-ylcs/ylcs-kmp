package love.yinlin.foundation.parser.toml

internal class TomlLiteral(
    override val content: String,
    override val kind: TomlScalarKind,
) : TomlPrimitive() {
    override val isString: Boolean get() = kind == TomlScalarKind.String
    override fun equals(other: Any?): Boolean = other is TomlLiteral && kind == other.kind && content == other.content
    override fun hashCode(): Int = 31 * kind.hashCode() + content.hashCode()
}