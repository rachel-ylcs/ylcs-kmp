package love.yinlin.extension

import androidx.compose.runtime.snapshots.SnapshotStateMap

//  ----------  Data Transfer  ----------

fun <K, V> Map<K, V>.toMutableStateMap() = SnapshotStateMap<K, V>().also { it.putAll(this) }

//  ----------  Data Change  ----------

// 将容器替换为另一个容器
fun <T> MutableCollection<T>.replaceAll(other: Collection<T>) {
	this.clear()
	this.addAll(other)
}

// 将容器替换为另一个容器
fun <K, V> MutableMap<K, V>.replaceAll(other: Map<K, V>) {
	this.clear()
	this.putAll(other)
}

//  ----------  Data Find  ----------

// 在集合中查找计算结果与value相同的元素
inline fun <T, V> V.findSelf(collection: Iterable<T>, predicate: (T) -> V): T? =
	collection.find { predicate(it) == this }

// 若查找到元素则对这个元素执行操作并返回结果
inline fun <T, R> Iterable<T>.findRun(predicate: (T) -> Boolean, block: (T) -> R): R? =
	this.find(predicate = predicate)?.let { block(it) }

// 若查找到元素则对这个元素执行操作并返回结果
inline fun <T, R> MutableCollection<T>.findModify(predicate: (T) -> Boolean, block: MutableCollection<T>.(T) -> R): R? =
	this.find(predicate = predicate)?.let { this.block(it) }

// 若查找到元素则对这个元素执行操作并将结果赋值给自身
inline fun <T> MutableList<T>.findAssign(predicate: (T) -> Boolean, block: (T) -> T) {
	val index = this.indexOfFirst(predicate = predicate)
	if (index != -1) this[index] = block(this[index])
}