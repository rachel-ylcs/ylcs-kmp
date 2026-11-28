package love.yinlin.tpl

import kotlinx.coroutines.test.runTest
import love.yinlin.extension.catchingError
import love.yinlin.extension.catchingNull
import love.yinlin.extension.toJsonString
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KVariance
import kotlin.reflect.typeOf
import kotlin.test.assertTrue

class TPTest private constructor() {
    suspend inline fun <reified T : Any> tp(request: suspend () -> T?): Task<T> {
        val result = runCatching { checkNotNull(request()) { "TP request returned null" } }
        return Task(result, typeOf<T>().logTypeName(), result.logJson())
    }

    suspend inline fun <reified T : Any> tpNull(request: suspend () -> T?): Task<T?> {
        val result = runCatching { request() }
        return Task(result, typeOf<T>().logTypeName(), result.logJson())
    }

    @PublishedApi
    internal fun KType.logTypeName(): String {
        val typeClass = classifier as? KClass<*> ?: return toString()
        val name = typeClass.simpleName ?: typeClass.java.name
        val typeArguments = if (arguments.isEmpty()) "" else arguments.joinToString(prefix = "<", postfix = ">") { (val variance, val type) ->
            val argument = type ?: return@joinToString "*"
            val prefix = when (variance) {
                KVariance.IN -> "in "
                KVariance.OUT -> "out "
                else -> ""
            }
            prefix + argument.logTypeName()
        }
        return if (isMarkedNullable) "$name$typeArguments?" else "$name$typeArguments"
    }

    @PublishedApi
    internal inline fun <reified T> Result<T>.logJson(): String? = fold(
        onSuccess = { value -> catchingNull { value.toJsonString() } ?: value.toString().toJsonString() },
        onFailure = { null },
    )

    suspend fun tpAssert(message: String = "tpAssert returned false", assertion: suspend () -> Boolean) {
        assertTrue(assertion(), message)
    }

    class Task<T> @PublishedApi internal constructor(
        private val result: Result<T>,
        private val type: String,
        private val json: String?,
    ) {
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, T> {
            val name = property.name
            val value = result.getOrElse { error ->
                println("[TP / $name] failed")
                throw error
            }
            println("###### [TP / $name: $type]\n$json")
            return ReadOnlyProperty { _, _ -> value }
        }
    }

    companion object {
        operator fun invoke(block: suspend TPTest.() -> Unit) = runTest {
            catchingError { TPTest().block() }?.printStackTrace(System.err)
            println()
        }
    }
}
