package love.yinlin.compose.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastLastOrNull
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import love.yinlin.extension.replaceAll
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Stable
@Serializable(UUIDKey.Serializer::class)
class UUIDKey<T>(val data: T) {
    class Serializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<UUIDKey<T>> {
        override val descriptor: SerialDescriptor = dataSerializer.descriptor
        override fun serialize(encoder: Encoder, value: UUIDKey<T>) {
            encoder.encodeSerializableValue(dataSerializer, value.data)
        }
        override fun deserialize(decoder: Decoder): UUIDKey<T> = UUIDKey(decoder.decodeSerializableValue(dataSerializer))
    }

    val key: Any = Uuid.generateV7()

    operator fun component1(): T = data
    override fun equals(other: Any?): Boolean = (other as? UUIDKey<T>)?.key == key
    override fun hashCode(): Int = key.hashCode()
    override fun toString(): String = data.toString()
}

inline val <reified T> Collection<T>.keyList: List<UUIDKey<T>> get() = map { UUIDKey(it) }
inline val <reified T> List<UUIDKey<T>>.data: List<T> get() = fastMap { it.data }
inline fun <reified T> List<UUIDKey<T>>.findByData(predicate: (T) -> Boolean): T? = fastFirstOrNull { predicate(it.data) }?.data
inline fun <reified T> List<UUIDKey<T>>.findLastByData(predicate: (T) -> Boolean): T? = fastLastOrNull { predicate(it.data) }?.data
inline fun <reified T> List<UUIDKey<T>>.firstByData(): T = first().data
inline fun <reified T> List<UUIDKey<T>>.firstOrNullByData(): T? = firstOrNull()?.data
inline fun <reified T> List<UUIDKey<T>>.lastByData(): T = last().data
inline fun <reified T> List<UUIDKey<T>>.lastOrNullByData(): T? = lastOrNull()?.data
inline fun <reified T> List<UUIDKey<T>>.getByData(index: Int) = get(index).data
inline fun <reified T> List<UUIDKey<T>>.getOrElseByData(index: Int, defaultValue: (Int) -> T): T = getOrElse(index) { UUIDKey(defaultValue(it)) }.data
inline fun <reified T> List<UUIDKey<T>>.getOrNullByData(index: Int): T? = getOrNull(index)?.data
inline fun <reified T> List<UUIDKey<T>>.indexOfFirstByData(predicate: (T) -> Boolean): Int = indexOfFirst { predicate(it.data) }
inline fun <reified T> List<UUIDKey<T>>.indexOfLastByData(predicate: (T) -> Boolean): Int = indexOfLast { predicate(it.data) }
inline fun <reified T> List<UUIDKey<T>>.randomByData(): T = random().data
inline fun <reified T> List<UUIDKey<T>>.randomOrNullByData(): T? = randomOrNull()?.data
inline fun <reified T, reified R> List<UUIDKey<T>>.mapByData(block: (T) -> R): List<R> = fastMap { block(it.data) }
inline fun <reified T> List<UUIDKey<T>>.filterByData(predicate: (T) -> Boolean): List<T> = fastFilter { predicate(it.data) }.data
inline fun <reified T> List<UUIDKey<T>>.forEachByData(action: (T) -> Unit) = fastForEach { action(it.data) }
inline fun <reified T> List<UUIDKey<T>>.forEachIndexedByData(action: (Int, T) -> Unit) = fastForEachIndexed { index, item -> action(index, item.data) }
inline fun <reified T> MutableList<UUIDKey<T>>.setByData(index: Int, value: T) { set(index, UUIDKey(value)) }
inline fun <reified T> MutableList<UUIDKey<T>>.setByData(index: Int, valueProvider: (T) -> T) { set(index, UUIDKey(valueProvider(get(index).data))) }
inline fun <reified T> MutableList<UUIDKey<T>>.addByData(value: T) = add(UUIDKey(value))
inline fun <reified T> MutableList<UUIDKey<T>>.addByData(index: Int, value: T) = add(index, UUIDKey(value))
inline fun <reified T> MutableList<UUIDKey<T>>.addAllByData(elements: Collection<T>): Boolean = addAll(elements.keyList)
inline fun <reified T> MutableList<UUIDKey<T>>.replaceAllByData(elements: Collection<T>) = replaceAll(elements.keyList)