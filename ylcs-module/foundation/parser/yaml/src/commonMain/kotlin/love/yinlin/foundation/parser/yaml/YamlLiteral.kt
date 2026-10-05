package love.yinlin.foundation.parser.yaml

internal class YamlLiteral(
    override val content: String,
    override val kind: YamlScalarKind,
) : YamlPrimitive() {
    override val isString: Boolean get() = kind == YamlScalarKind.String
    override fun equals(other: Any?): Boolean = other is YamlLiteral && kind == other.kind && content == other.content
    override fun hashCode(): Int = 31 * kind.hashCode() + content.hashCode()
}