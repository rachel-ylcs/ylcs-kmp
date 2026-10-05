package love.yinlin.foundation.parser.toml

import kotlin.contracts.*

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class TomlDsl

@TomlDsl
class TomlObjectBuilder @PublishedApi internal constructor() {
    @PublishedApi
    internal val content: MutableMap<String, TomlElement> = linkedMapOf()

    infix fun String.with(value: TomlElement) { content[this] = value }
    infix fun String.with(value: String) { content[this] = TomlPrimitive(value) }
    infix fun String.with(value: Boolean) { content[this] = TomlPrimitive(value) }
    infix fun String.with(value: Number) { content[this] = TomlPrimitive(value) }

    inline fun obj(name: String, block: TomlObjectBuilder.() -> Unit) { content[name] = buildTomlObject(block) }
    inline fun arr(name: String, block: TomlArrayBuilder.() -> Unit) { content[name] = buildTomlArray(block) }
    fun merge(value: TomlObject) { content.putAll(value) }
}

@TomlDsl
class TomlArrayBuilder @PublishedApi internal constructor() {
    @PublishedApi internal val content: MutableList<TomlElement> = arrayListOf()

    fun add(value: TomlElement) { content.add(value) }
    fun add(value: String) { content.add(TomlPrimitive(value)) }
    fun add(value: Boolean) { content.add(TomlPrimitive(value)) }
    fun add(value: Number) { content.add(TomlPrimitive(value)) }

    inline fun obj(block: TomlObjectBuilder.() -> Unit) { content.add(buildTomlObject(block)) }
    inline fun arr(block: TomlArrayBuilder.() -> Unit) { content.add(buildTomlArray(block)) }
    fun merge(value: TomlArray) { content.addAll(value) }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTomlObject(block: TomlObjectBuilder.() -> Unit): TomlObject {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return TomlObject(TomlObjectBuilder().apply(block).content)
}

@OptIn(ExperimentalContracts::class)
inline fun buildTomlArray(block: TomlArrayBuilder.() -> Unit): TomlArray {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return TomlArray(TomlArrayBuilder().apply(block).content)
}

inline fun toml(block: TomlObjectBuilder.() -> Unit): TomlObject = buildTomlObject(block)