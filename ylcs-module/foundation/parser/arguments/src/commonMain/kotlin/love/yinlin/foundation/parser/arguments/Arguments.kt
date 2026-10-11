package love.yinlin.foundation.parser.arguments

import kotlinx.serialization.Serializable


/**
 * 严格的 `key=value` 参数集合，同时提供 Map 的访问接口。
 *
 * 首项含路径分隔符或常见可执行文件后缀时识别为 [executable]；裸名称保留为无值参数。
 * 完整命令行中的双引号首项明确表示程序路径，可以用来指定裸程序名。
 * 重复键按顺序收集所有赋值；无值项只表示键存在，不会清除已收集的值。
 */

@ConsistentCopyVisibility
@Serializable
data class Arguments private constructor(val executable: String?, private val arguments: Map<String, Argument>) {
    @Suppress("unused")
    private constructor(parser: ArgumentsParser) : this(parser.executable, parser.arguments)
    // 解析完整命令行；引号外的空白分隔参数
    constructor(commandLine: String) : this(ArgumentsParser(commandLine))
    // 每个元素是一项原始参数；包含空白或等号的值仍需双引号
    constructor(args: Array<out String>) : this(ArgumentsParser(args))

    val size: Int = arguments.size
    val isEmpty: Boolean = arguments.isEmpty()
    val isNotEmpty: Boolean = arguments.isNotEmpty()
    val keys: Set<String> = arguments.keys

    operator fun contains(key: String): Boolean = key in arguments

    operator fun get(key: String): Argument? = arguments[key]

    operator fun iterator(): Iterator<Map.Entry<String, Argument>> = arguments.iterator()

    override fun toString(): String = buildString {
        val first = executable
        if (first != null) {
            append(first)
            append(' ')
        }
        for ([key, arg] in arguments) {
            when (arg) {
                is Argument.Empty -> {
                    append(key)
                    append(' ')
                }
                is Argument.Single -> append("$key=${arg.value} ")
                is Argument.Values -> {
                    for (v in arg.values) append("$key=$v ")
                }
            }
        }
        deleteAt(length - 1)
    }
}