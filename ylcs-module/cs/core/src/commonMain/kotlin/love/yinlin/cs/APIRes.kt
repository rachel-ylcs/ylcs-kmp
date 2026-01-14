package love.yinlin.cs

open class APIRes private constructor(name: String?, parent: APIRes?) : APIFile {
    constructor(root: String) : this(root, null)
    constructor(parent: APIRes) : this(null, parent)
    constructor(parent: APIRes, name: String) : this(name, parent)

    val path: String = when {
        parent == null -> name!!
        name != null -> "$parent/$name"
        else -> "$parent/${this::class.simpleName!!.lowercase()}"
    }

    override val files: List<String> = listOf(path)

    override fun toString(): String = path
}