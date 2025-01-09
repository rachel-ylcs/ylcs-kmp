package love.yinlin.extension

fun <T> MutableCollection<T>.replaceAll(other: Collection<T>) {
	this.clear()
	this.addAll(other)
}

fun <K, V> MutableMap<K, V>.replaceAll(other: Map<K, V>) {
	this.clear()
	this.putAll(other)
}