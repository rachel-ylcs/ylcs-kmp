@file:Suppress("unused")
package love.yinlin.foundation.parser.yaml

import kotlin.contracts.*

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class YamlDsl

@YamlDsl
class YamlObjectBuilder @PublishedApi internal constructor() {
    @PublishedApi
    internal val content: MutableMap<String, YamlElement> = linkedMapOf()

    infix fun String.with(value: YamlElement) { content[this] = value }
    infix fun String.with(value: String?) { content[this] = YamlPrimitive(value) }
    infix fun String.with(value: Boolean?) { content[this] = YamlPrimitive(value) }
    infix fun String.with(value: Number?) { content[this] = YamlPrimitive(value) }
    infix fun String.with(value: Nothing?) { content[this] = YamlNull }

    inline fun obj(name: String, block: YamlObjectBuilder.() -> Unit) { content[name] = buildYamlObject(block) }
    inline fun arr(name: String, block: YamlArrayBuilder.() -> Unit) { content[name] = buildYamlArray(block) }
    fun merge(value: YamlObject) { content.putAll(value) }
}

@YamlDsl
class YamlArrayBuilder @PublishedApi internal constructor() {
    @PublishedApi internal val content: MutableList<YamlElement> = arrayListOf()

    fun add(value: YamlElement) { content.add(value) }
    fun add(value: String?) = add(YamlPrimitive(value))
    fun add(value: Boolean?) = add(YamlPrimitive(value))
    fun add(value: Number?) = add(YamlPrimitive(value))
    fun add(value: Nothing?) = add(YamlNull)

    inline fun obj(block: YamlObjectBuilder.() -> Unit) = add(buildYamlObject(block))
    inline fun arr(block: YamlArrayBuilder.() -> Unit) = add(buildYamlArray(block))
    fun merge(value: YamlArray) { content.addAll(value) }
}

@OptIn(ExperimentalContracts::class)
inline fun buildYamlObject(block: YamlObjectBuilder.() -> Unit): YamlObject {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return YamlObject(YamlObjectBuilder().apply(block).content)
}

@OptIn(ExperimentalContracts::class)
inline fun buildYamlArray(block: YamlArrayBuilder.() -> Unit): YamlArray {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return YamlArray(YamlArrayBuilder().apply(block).content)
}

inline fun yaml(block: YamlObjectBuilder.() -> Unit): YamlObject = buildYamlObject(block)