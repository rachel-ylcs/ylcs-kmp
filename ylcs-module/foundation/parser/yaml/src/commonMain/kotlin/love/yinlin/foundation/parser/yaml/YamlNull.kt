package love.yinlin.foundation.parser.yaml

object YamlNull : YamlPrimitive() {
    override val content: String = "null"
    override val isString: Boolean = false
    override val kind: YamlScalarKind = YamlScalarKind.Null
}