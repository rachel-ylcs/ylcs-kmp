package love.yinlin.api

open class ResNode protected constructor(val path: String) {
    constructor(parent: ResNode, name: String) : this("${parent.path}/$name")

    override fun toString(): String = path
}