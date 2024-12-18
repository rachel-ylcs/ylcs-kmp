package love.yinlin.platform

actual class KVContext

actual class KV actual constructor(context: KVContext) {
	actual fun set(key: String, value: Boolean, expire: Int) { }
	actual fun set(key: String, value: Int, expire: Int) { }
	actual fun set(key: String, value: Long, expire: Int) { }
	actual fun set(key: String, value: Float, expire: Int) { }
	actual fun set(key: String, value: Double, expire: Int) { }
	actual fun set(key: String, value: String, expire: Int) { }
	actual fun set(key: String, value: ByteArray, expire: Int) { }
	actual inline fun <reified T> get(key: String, default: T): T { return default }
	actual fun has(key: String): Boolean { return false }
}