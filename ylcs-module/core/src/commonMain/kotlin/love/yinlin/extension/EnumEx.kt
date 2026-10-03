package love.yinlin.extension

import kotlin.enums.enumEntries

/**
 * 获取枚举数量
 */
inline fun <reified E : Enum<E>> enumSize(): Int = enumEntries<E>().size

/**
 * 获取指定位置的枚举值
 *
 * @param index 索引
 * @throws IndexOutOfBoundsException
 */
inline fun <reified E : Enum<E>> enum(index: Int): E = enumEntries<E>()[index]

/**
 * 获取指定位置的枚举值
 *
 * @param index 索引
 * @param default 默认值
 */
inline fun <reified E : Enum<E>> enum(index: Int, default: E): E = enumEntries<E>().getOrNull(index) ?: default

/**
 * 获取指定位置的枚举值
 *
 * @param index 索引
 */
inline fun <reified E : Enum<E>> enumNull(index: Int): E? = enumEntries<E>().getOrNull(index)

/**
 * 获取满足条件的枚举值
 *
 * @param predicate 谓词
 * @throws NoSuchElementException
 */
inline fun <reified E : Enum<E>> enum(predicate: (E) -> Boolean): E = enumEntries<E>().first(predicate)

/**
 * 获取满足条件的枚举值
 *
 * @param default 默认值
 * @param predicate 谓词
 */
inline fun <reified E : Enum<E>> enum(default: E, predicate: (E) -> Boolean): E = enumEntries<E>().firstOrNull(predicate) ?: default

/**
 * 获取满足条件的枚举值
 *
 * @param predicate 谓词
 */
inline fun <reified E : Enum<E>> enumNull(predicate: (E) -> Boolean): E? = enumEntries<E>().firstOrNull(predicate)

/**
 * 获取满足期望值的枚举值
 *
 * @param value 期望值
 * @param predicate 谓词
 * @throws NoSuchElementException
 */
inline fun <reified E : Enum<E>, reified T> enum(value: T, predicate: (E) -> T): E = enumEntries<E>().first { predicate(it) == value }

/**
 * 获取满足期望值的枚举值
 *
 * @param value 期望值
 * @param default 默认值
 * @param predicate 谓词
 */
inline fun <reified E : Enum<E>, reified T> enum(value: T, default: E, predicate: (E) -> T): E = enumEntries<E>().firstOrNull { predicate(it) == value } ?: default

/**
 * 获取满足期望值的枚举值
 *
 * @param value 期望值
 * @param predicate 谓词
 */
inline fun <reified E : Enum<E>, reified T> enumNull(value: T, predicate: (E) -> T): E? = enumEntries<E>().firstOrNull { predicate(it) == value }

/**
 * 枚举序列
 */
inline fun <reified E : Enum<E>> enumSequence(): Sequence<E> = enumEntries<E>().asSequence()

/**
 * 枚举列表
 */
inline fun <reified E : Enum<E>, R> enumMap(transform: (E) -> R): List<R> = enumEntries<E>().map(transform)

/**
 * 枚举映射
 */
inline fun <reified E : Enum<E>, V> enumAssociateWith(valueSelector: (E) -> V): Map<E, V> = enumEntries<E>().associateWith(valueSelector)

/**
 * 遍历枚举值
 */
inline fun <reified E : Enum<E>> enumForEach(action: (E) -> Unit) = enumEntries<E>().forEach(action)

/**
 * 遍历枚举值
 */
inline fun <reified E : Enum<E>> enumForEachIndexed(action: (Int, E) -> Unit) = enumEntries<E>().forEachIndexed(action)

/**
 * 下一个枚举值（循环）
 *
 * @param value 枚举值
 */
inline fun <reified E : Enum<E>> enumNext(value: E): E {
    val entries = enumEntries<E>()
    return entries[(value.ordinal + 1) % entries.size]
}