package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import love.yinlin.extension.*

@PublishedApi
@Stable
@Serializable(ScreenArgs.Serializer::class)
internal class ScreenArgs(raw: JsonArray) {
    object Serializer : DelegateSerializer<ScreenArgs, JsonArray>() {
        override val delegate: KSerializer<JsonArray> = JsonArray.serializer()
        override fun encode(value: ScreenArgs): JsonArray = value.delegate
        override fun decode(value: JsonArray): ScreenArgs = ScreenArgs(value)
    }

    companion object {
        @Stable
        val Empty: ScreenArgs = ScreenArgs(makeArray { })

        inline fun <reified A1> build(arg1: A1): ScreenArgs = ScreenArgs(makeArray {
            add(arg1.toJson())
        })

        inline fun <reified A1, reified A2> build(arg1: A1, arg2: A2): ScreenArgs = ScreenArgs(makeArray {
            add(arg1.toJson())
            add(arg2.toJson())
        })

        inline fun <reified A1, reified A2, reified A3> build(arg1: A1, arg2: A2, arg3: A3): ScreenArgs = ScreenArgs(makeArray {
            add(arg1.toJson())
            add(arg2.toJson())
            add(arg3.toJson())
        })

        inline fun <reified A1, reified A2, reified A3, reified A4> build(arg1: A1, arg2: A2, arg3: A3, arg4: A4): ScreenArgs = ScreenArgs(makeArray {
            add(arg1.toJson())
            add(arg2.toJson())
            add(arg3.toJson())
            add(arg4.toJson())
        })

        inline fun <reified A1, reified A2, reified A3, reified A4, reified A5> build(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5): ScreenArgs = ScreenArgs(makeArray {
            add(arg1.toJson())
            add(arg2.toJson())
            add(arg3.toJson())
            add(arg4.toJson())
            add(arg5.toJson())
        })
    }

    @PublishedApi
    internal val delegate: JsonArray = raw.toString().parseJson.Array

    val size: Int get() = delegate.size
    val isEmpty: Boolean get() = delegate.isEmpty()
    val isNotEmpty: Boolean get() = delegate.isNotEmpty()

    inline operator fun <reified T> get(index: Int): T = delegate[index].to()

    override fun equals(other: Any?): Boolean = delegate == (other as? ScreenArgs)?.delegate
    override fun toString(): String = delegate.toString()
    override fun hashCode(): Int = delegate.hashCode()
}
