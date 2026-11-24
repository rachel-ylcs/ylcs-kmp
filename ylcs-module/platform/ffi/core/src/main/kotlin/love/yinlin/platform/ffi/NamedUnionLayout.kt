package love.yinlin.platform.ffi

import java.lang.foreign.MemoryLayout
import java.lang.foreign.UnionLayout

@NamedLayoutDsl
class NamedUnionLayout private constructor(
    private val name: String?,
    private val alignment: Long?
) {
    private val items: MutableList<MemoryLayout> = []

    infix fun String.with(layout: MemoryLayout) {
        items += layout.withName(this)
    }

    fun add(layout: MemoryLayout) {
        items += layout
    }

    fun padding(alignment: Long) {
        items += MemoryLayout.paddingLayout(alignment)
    }

    fun struct(name: String? = null, alignment: Long? = null, builder: NamedStructLayout.() -> Unit) {
        items += NamedStructLayout(name, alignment, builder)
    }

    fun union(name: String? = null, alignment: Long? = null, builder: NamedUnionLayout.() -> Unit) {
        items += NamedUnionLayout(name, alignment, builder)
    }

    private fun build(): UnionLayout {
        var layout = MemoryLayout.unionLayout(*items.toTypedArray())
        if (name != null) layout = layout.withName(name)
        if (alignment != null) layout = layout.withByteAlignment(alignment)
        return layout
    }

    companion object {
        operator fun invoke(
            name: String? = null,
            alignment: Long? = null,
            builder: NamedUnionLayout.() -> Unit
        ): UnionLayout {
            val layout = NamedUnionLayout(name, alignment)
            layout.builder()
            return layout.build()
        }
    }
}
