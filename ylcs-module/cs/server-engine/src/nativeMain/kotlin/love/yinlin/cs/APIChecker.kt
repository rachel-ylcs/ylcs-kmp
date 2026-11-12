package love.yinlin.cs

import love.yinlin.extension.lazyName
import kotlin.properties.ReadOnlyProperty

@Suppress("PropertyName", "FunctionName", "unused")
abstract class APIChecker {
    protected inline fun <T> define(
        name: String,
        message: String? = null,
        crossinline block: (T) -> Boolean
    ): APIVerify<T> = object : APIVerify<T> {
        val callback = { arg: T ->
            if (!block(arg)) throw CheckerException(name, arg, message)
        }

        override infix fun check(value: T) {
            callback(value)
        }

        override infix fun check(values: List<T>) {
            for (value in values) callback(value)
        }
    }

    protected inline fun <T> define(
        message: String? = null,
        crossinline block: (T) -> Boolean
    ): ReadOnlyProperty<Any?, APIVerify<T>> = lazyName { name -> define(name, message, block) }

    val True by define<Boolean> { it }
    val False by define<Boolean> { !it }

    val Equals by define<Pair<Any, Any>> { it.first == it.second }
    val NotEquals by define<Pair<Any, Any>> { it.first != it.second }

    fun <T : Comparable<T>> AtMost(value: T) = define<T>("AtMost") { it <= value }
    fun <T : Comparable<T>> AtLeast(value: T) = define<T>("AtLeast") { it >= value }

    val NotEmpty by define<String> { it.isNotEmpty() }

    val Positive by define<Number> { it.toLong() > 0L }

    val HasFile by define<APIFile> { it.files.isNotEmpty() }

    fun Range(range: IntRange) = define<Int>("Range") { it in range }
}
