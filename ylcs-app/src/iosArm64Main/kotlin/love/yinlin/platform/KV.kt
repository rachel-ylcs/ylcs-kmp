package love.yinlin.platform

actual class KV {
	actual fun set(key: String, value: Boolean, expire: Int) { TODO() }
	actual fun set(key: String, value: Int, expire: Int) { TODO() }
	actual fun set(key: String, value: Long, expire: Int) { TODO() }
	actual fun set(key: String, value: Float, expire: Int) { TODO() }
	actual fun set(key: String, value: Double, expire: Int) { TODO() }
	actual fun set(key: String, value: String, expire: Int) { TODO() }
	actual fun set(key: String, value: ByteArray, expire: Int) { TODO() }
	actual inline fun <reified T> get(key: String, default: T): T { TODO() }
	actual fun has(key: String): Boolean { TODO() }
	actual fun remove(key: String) { TODO() }
}